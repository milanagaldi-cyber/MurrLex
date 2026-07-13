import mimetypes
import re
import zipfile
from pathlib import Path
from xml.etree import ElementTree

from django.core.exceptions import ValidationError
from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import transaction
from django.utils import timezone

from .models import AdditionalGeneration, AiModelProfile, Asset, Character, DialogueLine, DocxImport, Episode, GenerationOutput, Project, Prompt, PromptBlock, Scene
from .revisions import audit, record_revision
from .storage import create_asset

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS = {"w": W_NS, "a": "http://schemas.openxmlformats.org/drawingml/2006/main", "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships"}
MAX_ARCHIVE_ENTRIES = 2000
MAX_UNCOMPRESSED_BYTES = 300 * 1024 * 1024
MAX_DOCUMENT_XML_BYTES = 20 * 1024 * 1024
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp"}
FIELD_NAMES = {
    "\u0445\u0443\u043a": "hook",
    "\u043e\u0431\u0449\u0435\u0435 \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u0441\u0446\u0435\u043d\u044b": "description",
    "\u043c\u0435\u0441\u0442\u043e": "location",
    "\u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044f": "actions",
    "\u0430\u043a\u0442\u0451\u0440\u0441\u043a\u0430\u044f \u043f\u043e\u0434\u0430\u0447\u0430": "performance_notes",
    "\u0430\u043a\u0442\u0435\u0440\u0441\u043a\u0430\u044f \u043f\u043e\u0434\u0430\u0447\u0430": "performance_notes",
    "\u0434\u0438\u0430\u043b\u043e\u0433\u0438": "dialogue",
    "\u043a\u0440\u0430\u0442\u043a\u043e\u0435 \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u0441\u0435\u0440\u0438\u0438": "summary",
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


def _body_media_assignments(root, archive):
    rel_path = "word/_rels/document.xml.rels"
    if rel_path not in archive.namelist():
        return []
    rel_root = ElementTree.fromstring(archive.read(rel_path))
    relationships = {}
    for rel in rel_root:
        rel_id = rel.get("Id")
        target = rel.get("Target", "")
        if rel_id and "media/" in target:
            relationships[rel_id] = "word/" + target.lstrip("/")
    assignments = []
    episode_number = None
    scene_number = None
    generation_title = None
    body = root.find(".//w:body", NS)
    if body is None:
        return assignments
    for child in body:
        if child.tag.endswith("}p"):
            style, text = _paragraph(child)
            episode_match = re.match(r"^(?:\u0421\u0415\u0420\u0418\u042f|EPISODE)\s*(\d+)", text, re.IGNORECASE)
            scene_match = re.match(r"^\u0421\u0446\u0435\u043d\u0430\s*(\d+)", text, re.IGNORECASE)
            if style == "Heading1" and episode_match:
                episode_number = int(episode_match.group(1))
                scene_number = None
                generation_title = None
            elif style == "Heading1" and scene_match:
                scene_number = int(scene_match.group(1))
                generation_title = None
            elif style == "Heading1":
                scene_number = None
                generation_title = text if re.match(r"^\u0414\u041e\u0413\u0415\u041d\u0415\u0420\u0410\u0426\u0418\u042f", text, re.IGNORECASE) else None
            for blip in child.findall(".//a:blip", NS):
                rel_id = blip.get(f"{{{NS['r']}}}embed")
                media_name = relationships.get(rel_id)
                if media_name:
                    assignments.append({"media": media_name, "episode": episode_number, "scene": scene_number, "generation": generation_title})
        if not child.tag.endswith("}p"):
            for blip in child.findall(".//a:blip", NS):
                rel_id = blip.get(f"{{{NS['r']}}}embed")
                media_name = relationships.get(rel_id)
                if media_name:
                    assignments.append({"media": media_name, "episode": episode_number, "scene": scene_number, "generation": generation_title})
    return assignments


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
        tables = []
        for table in root.findall(".//w:tbl", NS):
            table_rows = []
            for row in table.findall("./w:tr", NS):
                table_rows.append(["\n".join("".join(node.text or "" for node in paragraph.findall(".//w:t", NS)).strip() for paragraph in cell.findall("./w:p", NS) if "".join(node.text or "" for node in paragraph.findall(".//w:t", NS)).strip()) for cell in row.findall("./w:tc", NS)])
            tables.append(table_rows)
        table_count = len(tables)
        media_assignments = _body_media_assignments(root, archive)
    uploaded.seek(0)

    title = next((text.strip("\u201c\u201d\" ") for style, text in rows if not style and text.strip("\u201c\u201d\" ").upper() not in {"LEXAMORA SERIES", "MASTER DOCUMENT"}), Path(uploaded.name).stem)
    episodes = []
    current_episode = None
    current_scene = None
    current_field = None
    for style, text in rows:
        episode_match = re.match(r"^(?:\u0421\u0415\u0420\u0418\u042f|EPISODE)\s*(\d+)\s*[:.\-]?\s*(.*)$", text, re.IGNORECASE)
        scene_match = re.match(r"^\u0421\u0446\u0435\u043d\u0430\s*(\d+)\s*[.:\-]?\s*(.*)$", text, re.IGNORECASE)
        if style == "Heading1" and episode_match:
            current_episode = {"number": int(episode_match.group(1)), "title": episode_match.group(2).strip(" \u201c\u201d.\""), "summary": "", "include": True, "scenes": []}
            episodes.append(current_episode)
            current_scene = None
            current_field = None
            continue
        if style == "Heading1" and scene_match:
            if current_episode is None:
                current_episode = {"number": 1, "title": "Imported episode", "summary": "", "include": True, "scenes": []}
                episodes.append(current_episode)
            current_scene = {"number": int(scene_match.group(1)), "title": scene_match.group(2).strip(" \u201c\u201d.\""), "hook": "", "description": "", "location": "", "actions": "", "performance_notes": "", "dialogue": "", "include": True}
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

    characters = []
    for index, (style, text) in enumerate(rows):
        if style == "Heading3" and text.casefold() == "\u0433\u043b\u0430\u0432\u043d\u044b\u0435 \u0433\u0435\u0440\u043e\u0438":
            values = []
            for next_style, next_text in rows[index + 1:]:
                if next_style.startswith("Heading"):
                    break
                values.append(next_text)
            for offset in range(0, len(values) - 1, 2):
                if values[offset] and not any(item["name"] == values[offset] for item in characters):
                    characters.append({"name": values[offset], "description": values[offset + 1], "include": True})
    all_scenes = [scene for episode in episodes for scene in episode["scenes"]]
    candidate_tables = tables[2:2 + len(all_scenes)]
    for scene, table in zip(all_scenes, candidate_tables):
        scene["prompts"] = []
        for row in table:
            value = row[-1].strip() if row else ""
            if not value:
                continue
            parts = value.splitlines()
            if len(parts) > 1 and parts[0].strip():
                scene["prompts"].append({"model": parts[0].strip(), "content": "\n".join(parts[1:]).strip(), "include": True})
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
        "media_assignments": media_assignments,
        "characters": characters,
        "episodes": episodes,
    }, warnings


def _dialogue_parts(text):
    matches = list(re.finditer(r"(?:(?<=^)|(?<=[\n.!?]))\s*([^:\n]{1,80}):\s*[\u201c\"]([^\u201d\"]+)[\u201d\"]", text))
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
    for position, character_data in enumerate(data.get("characters", [])):
        if not character_data.get("include", True):
            continue
        character = Character.objects.create(project=project, name=character_data["name"][:180], description=character_data.get("description", ""), position=position, created_by=user, updated_by=user)
        record_revision(instance=character, user=user, operation="IMPORT")
    generation_lookup = {}
    scene_lookup = {}
    for episode_data in data.get("episodes", []):
        if not episode_data.get("include", True):
            continue
        episode = Episode.objects.create(project=project, number=episode_data["number"], title=(episode_data.get("title") or f"Episode {episode_data['number']}")[:240], summary=episode_data.get("summary", ""), position=project.episodes.count(), created_by=user, updated_by=user)
        record_revision(instance=episode, user=user, operation="IMPORT")
        for scene_data in episode_data.get("scenes", []):
            if not scene_data.get("include", True):
                continue
            scene = Scene.objects.create(episode=episode, number=scene_data["number"], title=(scene_data.get("title") or f"Scene {scene_data['number']}")[:240], hook=scene_data.get("hook", ""), description=scene_data.get("description", ""), location=scene_data.get("location", ""), actions=scene_data.get("actions", ""), performance_notes=scene_data.get("performance_notes", ""), position=episode.scenes.count(), created_by=user, updated_by=user)
            record_revision(instance=scene, user=user, operation="IMPORT")
            scene_lookup[(episode.number, scene.number)] = scene
            for prompt_position, prompt_data in enumerate(scene_data.get("prompts", [])):
                if not prompt_data.get("include", True):
                    continue
                model_name = prompt_data.get("model", "")
                model = AiModelProfile.objects.filter(name__iexact=model_name, is_active=True).first()
                if model is None:
                    media_type = AiModelProfile.MediaType.IMAGE if "banana" in model_name.casefold() else AiModelProfile.MediaType.VIDEO
                    model, _ = AiModelProfile.objects.get_or_create(name=model_name[:120] or "Imported model", defaults={"provider": "imported", "model_id": model_name[:160] or "imported", "media_type": media_type})
                prompt_content = prompt_data.get("content", "")
                prompt = Prompt.objects.create(scene=scene, ai_model=model, prompt_type=Prompt.Type.IMAGE if model.media_type == AiModelProfile.MediaType.IMAGE else Prompt.Type.VIDEO, title=model_name[:180], content=prompt_content, position=prompt_position, created_by=user, updated_by=user)
                record_revision(instance=prompt, user=user, operation="IMPORT")
                block = PromptBlock.objects.create(prompt=prompt, block_type=PromptBlock.Type.NARRATIVE, content=prompt.content, position=0, created_by=user, updated_by=user)
                record_revision(instance=block, user=user, operation="IMPORT")
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
                assignment = next((item for item in data.get("media_assignments", []) if item.get("media") == media_name), None)
                scene = scene_lookup.get((assignment.get("episode"), assignment.get("scene"))) if assignment else None
                asset = create_asset(user=user, workspace=draft.workspace, uploaded=uploaded, kind=Asset.Kind.SCENE_IMAGE if scene else Asset.Kind.OTHER, project=project, scene=scene)
                generation_title = assignment.get("generation") if assignment else None
                generation = generation_lookup.get(generation_title)
                if generation_title and generation is None:
                    first_scene = Scene.objects.filter(episode__project=project).first()
                    if first_scene:
                        generation = AdditionalGeneration.objects.create(scene=first_scene, reason=generation_title, prompt="Imported from DOCX", position=len(generation_lookup), created_by=user, updated_by=user)
                        generation_lookup[generation_title] = generation
                if generation:
                    GenerationOutput.objects.create(generation=generation, asset=asset, position=generation.outputs.count(), created_by=user, updated_by=user)
    draft.project = project
    draft.status = DocxImport.Status.ACCEPTED
    draft.accepted_by = user
    draft.accepted_at = timezone.now()
    draft.save(update_fields=["project", "status", "accepted_by", "accepted_at"])
    audit(workspace=draft.workspace, actor=user, action="DOCX_IMPORT_ACCEPTED", instance=project, metadata={"importId": str(draft.id)})
    return project
