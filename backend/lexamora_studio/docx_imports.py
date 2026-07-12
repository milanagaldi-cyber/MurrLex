import mimetypes
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree

from django.core.exceptions import ValidationError
from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import transaction
from django.utils import timezone

from .models import Asset, DialogueLine, DocxImport, Episode, Project, Scene
from .revisions import audit, record_revision
from .storage import create_asset

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS = {"w": W_NS}
MAX_ARCHIVE_ENTRIES = 2000
MAX_UNCOMPRESSED_BYTES = 300 * 1024 * 1024
MAX_DOCUMENT_XML_BYTES = 20 * 1024 * 1024
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp"}
FIELD_NAMES = {
    "хук": "hook",
    "общее описание сцены": "description",
    "место": "location",
    "действия": "actions",
    "актёрская подача": "performance_notes",
    "актерская подача": "performance_notes",
    "диалоги": "dialogue",
    "краткое описание серии": "summary",
}


def _paragraph(node):
    text = "".join(item.text or "" for item in node.findall(".//w:t", NS)).strip()
    style_node = node.find("./w:pPr/w:pStyle", NS)
    style = style_node.get(f"{{{W_NS}}}val", "") if style_node is not None else ""
    return style, text


def _safe_archive(uploaded):
    try:
        archive = zipfile.ZipFile(uploaded)
    except (zipfile.BadZipFile, OSError) as exc:
        raise ValidationError("The uploaded file is not a valid DOCX archive.") from exc
    entries = archive.infolist()
    if len(entries) > MAX_ARCHIVE_ENTRIES or sum(item.file_size for item in entries) > MAX_UNCOMPRESSED_BYTES:
        archive.close()
        raise ValidationError("The DOCX archive is too large after decompression.")
    if "word/document.xml" not in archive.namelist() or archive.getinfo("word/document.xml").file_size > MAX_DOCUMENT_XML_BYTES:
        archive.close()
        raise ValidationError("The DOCX document body is missing or too large.")
    return archive


def parse_docx(uploaded):
    uploaded.seek(0)
    with _safe_archive(uploaded) as archive:
        try:
            root = ElementTree.fromstring(archive.read("word/document.xml"))
        except ElementTree.ParseError as exc:
            raise ValidationError("The DOCX document XML is damaged.") from exc
        rows = [_paragraph(node) for node in root.findall(".//w:body/w:p", NS)]
        rows = [(style, text) for style, text in rows if text]
        media = [name for name in archive.namelist() if name.startswith("word/media/") and Path(name).suffix.lower() in IMAGE_SUFFIXES]
        table_count = len(root.findall(".//w:tbl", NS))
    uploaded.seek(0)

    title = next((text.strip("“”\" ") for style, text in rows if not style and text.strip("“”\" ").upper() not in {"LEXAMORA SERIES", "MASTER DOCUMENT"}), Path(uploaded.name).stem)
    episodes = []
    current_episode = None
    current_scene = None
    current_field = None
    for style, text in rows:
        episode_match = re.match(r"^(?:СЕРИЯ|EPISODE)\s*(\d+)\s*[:.\-]?\s*(.*)$", text, re.IGNORECASE)
        scene_match = re.match(r"^Сцена\s*(\d+)\s*[.:\-]?\s*(.*)$", text, re.IGNORECASE)
        if style == "Heading1" and episode_match:
            current_episode = {"number": int(episode_match.group(1)), "title": episode_match.group(2).strip(" “”.\""), "summary": "", "scenes": []}
            episodes.append(current_episode)
            current_scene = None
            current_field = None
            continue
        if style == "Heading1" and scene_match:
            if current_episode is None:
                current_episode = {"number": 1, "title": "Imported episode", "summary": "", "scenes": []}
                episodes.append(current_episode)
            current_scene = {"number": int(scene_match.group(1)), "title": scene_match.group(2).strip(" “”.\""), "hook": "", "description": "", "location": "", "actions": "", "performance_notes": "", "dialogue": ""}
            current_episode["scenes"].append(current_scene)
            current_field = None
            continue
        if style == "Heading1":
            current_scene = None
            current_field = None
            continue
        if style == "Heading3":
            current_field = FIELD_NAMES.get(text.casefold())
            continue
        if not current_field:
            continue
        target = current_scene if current_scene is not None else current_episode
        if target is not None and current_field in target:
            target[current_field] = "\n".join(filter(None, [target[current_field], text]))

    warnings = []
    if not episodes:
        warnings.append("No episode headings were recognized.")
    if not any(episode["scenes"] for episode in episodes):
        warnings.append("No scene headings were recognized.")
    return {
        "title": title or Path(uploaded.name).stem,
        "paragraph_count": len(rows),
        "table_count": table_count,
        "image_count": len(media),
        "media_files": media,
        "episodes": episodes,
    }, warnings


def _dialogue_parts(text):
    matches = list(re.finditer(r"(?:(?<=^)|(?<=[\n.!?]))\s*([^:\n]{1,80}):\s*[“\"]([^”\"]+)[”\"]", text))
    if matches:
        return [(match.group(1).strip(), match.group(2).strip()) for match in matches]
    return [("", line.strip()) for line in text.splitlines() if line.strip()]


@transaction.atomic
def accept_docx_import(*, draft, user):
    draft = DocxImport.objects.select_for_update().select_related("source_asset", "workspace").get(pk=draft.pk)
    if draft.status == DocxImport.Status.ACCEPTED:
        return draft.project
    data = draft.parsed_data
    base_title = str(data.get("title") or "Imported project")[:220]
    title = base_title
    suffix = 2
    while Project.objects.filter(workspace=draft.workspace, title=title).exists():
        title = f"{base_title[:210]} {suffix}"
        suffix += 1
    project = Project.objects.create(workspace=draft.workspace, project_type=Project.Type.SERIES, title=title, concept="Imported from DOCX. Review all detected fields before production use.", status=Project.Status.DRAFT, created_by=user, updated_by=user)
    record_revision(instance=project, user=user, operation="IMPORT")
    for episode_data in data.get("episodes", []):
        episode = Episode.objects.create(project=project, number=episode_data["number"], title=(episode_data.get("title") or f"Episode {episode_data['number']}")[:240], summary=episode_data.get("summary", ""), position=project.episodes.count(), created_by=user, updated_by=user)
        record_revision(instance=episode, user=user, operation="IMPORT")
        for scene_data in episode_data.get("scenes", []):
            scene = Scene.objects.create(episode=episode, number=scene_data["number"], title=(scene_data.get("title") or f"Scene {scene_data['number']}")[:240], hook=scene_data.get("hook", ""), description=scene_data.get("description", ""), location=scene_data.get("location", ""), actions=scene_data.get("actions", ""), performance_notes=scene_data.get("performance_notes", ""), position=episode.scenes.count(), created_by=user, updated_by=user)
            record_revision(instance=scene, user=user, operation="IMPORT")
            for position, (speaker, text) in enumerate(_dialogue_parts(scene_data.get("dialogue", ""))):
                line = DialogueLine.objects.create(scene=scene, speaker=speaker[:180], text=text, position=position, created_by=user, updated_by=user)
                record_revision(instance=line, user=user, operation="IMPORT")
    with draft.source_asset.file.open("rb") as source:
        with _safe_archive(source) as archive:
            for media_name in data.get("media_files", []):
                content = archive.read(media_name)
                filename = Path(media_name).name
                content_type = mimetypes.guess_type(filename)[0] or "application/octet-stream"
                uploaded = SimpleUploadedFile(filename, content, content_type=content_type)
                create_asset(user=user, workspace=draft.workspace, uploaded=uploaded, kind=Asset.Kind.OTHER, project=project)
    draft.project = project
    draft.status = DocxImport.Status.ACCEPTED
    draft.accepted_by = user
    draft.accepted_at = timezone.now()
    draft.save(update_fields=["project", "status", "accepted_by", "accepted_at"])
    audit(workspace=draft.workspace, actor=user, action="DOCX_IMPORT_ACCEPTED", instance=project, metadata={"importId": str(draft.id)})
    return project
