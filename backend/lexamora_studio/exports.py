import io
from datetime import timedelta
from html import escape
from pathlib import Path

from django.conf import settings
from django.core.files.base import ContentFile
from django.utils import timezone
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Image as PdfImage
from reportlab.platypus import PageBreak, Paragraph, SimpleDocTemplate, Spacer

from .models import Asset, ExportJob
from .revisions import audit
from .storage import create_asset


ALL_SECTIONS = [
    "metadata", "characters", "episodes", "scenes", "dialogue",
    "assets", "prompts", "generations", "subtitles",
]


class ExportError(Exception):
    pass


def _font_path():
    configured = settings.STUDIO_PDF_FONT_PATH.strip()
    candidates = [
        configured,
        "C:/Windows/Fonts/arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/dejavu/DejaVuSans.ttf",
    ]
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            return candidate
    raise ExportError("A Unicode PDF font is not configured on the server.")


def _register_font():
    name = "StudioSans"
    if name not in pdfmetrics.getRegisteredFontNames():
        pdfmetrics.registerFont(TTFont(name, _font_path()))
        pdfmetrics.registerFontFamily(name, normal=name, bold=name, italic=name, boldItalic=name)
    return name


def _safe(value):
    return escape(str(value or "")).replace(chr(10), "<br/>")


def _styles(font):
    styles = getSampleStyleSheet()
    for key in ("Normal", "Title", "Heading1", "Heading2", "Heading3"):
        styles[key].fontName = font
    styles["Title"].alignment = TA_CENTER
    styles.add(ParagraphStyle(name="StudioMeta", parent=styles["Normal"], textColor="#52615a", spaceAfter=5))
    styles.add(ParagraphStyle(name="StudioDialogue", parent=styles["Normal"], leftIndent=8 * mm, spaceAfter=4))
    return styles


def _header_footer(canvas, document, font, project_title):
    canvas.saveState()
    canvas.setFont(font, 8)
    canvas.setFillColorRGB(0.35, 0.4, 0.37)
    canvas.drawString(20 * mm, A4[1] - 13 * mm, project_title[:90])
    canvas.drawRightString(A4[0] - 20 * mm, 12 * mm, f"Page {document.page}")
    canvas.restoreState()


def _image_flowable(asset):
    if not asset.content_type.startswith("image/"):
        return None
    try:
        path = asset.file.path
        image = PdfImage(path)
        max_width = 165 * mm
        max_height = 105 * mm
        scale = min(max_width / image.imageWidth, max_height / image.imageHeight, 1)
        image.drawWidth = image.imageWidth * scale
        image.drawHeight = image.imageHeight * scale
        return image
    except (OSError, ValueError, AttributeError):
        return None


def render_project_pdf(project, episode, sections):
    font = _register_font()
    styles = _styles(font)
    output = io.BytesIO()
    document = SimpleDocTemplate(
        output, pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm,
        topMargin=22 * mm, bottomMargin=20 * mm,
        title=project.title, author=project.rights_holder or "Lexamora Studio",
    )
    story = [
        Spacer(1, 35 * mm),
        Paragraph(_safe(project.title), styles["Title"]),
        Spacer(1, 8 * mm),
        Paragraph(_safe(episode.title if episode else project.get_project_type_display()), styles["Heading2"]),
        Spacer(1, 14 * mm),
        Paragraph(_safe(project.rights_holder), styles["StudioMeta"]),
        Paragraph("CONFIDENTIAL / Lexamora Studio production document", styles["StudioMeta"]),
        PageBreak(),
    ]

    if "metadata" in sections:
        story += [Paragraph("Project metadata", styles["Heading1"])]
        for label, value in [
            ("Type", project.get_project_type_display()), ("Status", project.get_status_display()),
            ("Original language", project.original_language), ("Translation languages", ", ".join(project.translation_languages)),
            ("Concept", project.concept), ("Rights holder", project.rights_holder),
            ("Publication", project.publication_info),
        ]:
            story.append(Paragraph(f"<b>{escape(label)}:</b> {_safe(value)}", styles["Normal"]))
            story.append(Spacer(1, 2 * mm))

    if "characters" in sections:
        story.append(Paragraph("Characters", styles["Heading1"]))
        for character in project.characters.all():
            story.append(Paragraph(_safe(character.name), styles["Heading2"]))
            story.append(Paragraph(_safe(character.description), styles["Normal"]))
            if character.visual_description:
                story.append(Paragraph(f"<b>Visual:</b> {_safe(character.visual_description)}", styles["Normal"]))

    episodes = [episode] if episode else list(project.episodes.all())
    for current_episode in episodes:
        if "episodes" in sections:
            story += [
                PageBreak(), Paragraph(f"Episode {current_episode.number}: {_safe(current_episode.title)}", styles["Heading1"]),
                Paragraph(_safe(current_episode.summary), styles["Normal"]),
            ]
        for scene in current_episode.scenes.all():
            if "scenes" in sections:
                story.append(Paragraph(f"Scene {scene.number}: {_safe(scene.title)}", styles["Heading2"]))
                for label, value in [
                    ("Hook", scene.hook), ("Description", scene.description), ("Location", scene.location),
                    ("Actions", scene.actions), ("Performance", scene.performance_notes),
                ]:
                    if value:
                        story.append(Paragraph(f"<b>{escape(label)}:</b> {_safe(value)}", styles["Normal"]))
            if "dialogue" in sections:
                for line in scene.dialogue_lines.all():
                    story.append(Paragraph(f"<b>{_safe(line.speaker or 'Narrator')}:</b> {_safe(line.text)}", styles["StudioDialogue"]))
            if "prompts" in sections:
                for prompt in scene.prompts.all():
                    story.append(Paragraph(f"{_safe(prompt.ai_model.name)} / {prompt.get_prompt_type_display()}", styles["Heading3"]))
                    story.append(Paragraph(_safe(prompt.editor_content), styles["Normal"]))
            if "assets" in sections:
                for asset in scene.assets.all():
                    story.append(Paragraph(_safe(asset.original_filename), styles["StudioMeta"]))
                    image = _image_flowable(asset)
                    if image:
                        story += [image, Spacer(1, 3 * mm)]
            if "generations" in sections:
                for generation in scene.additional_generations.all():
                    story.append(Paragraph(f"Additional generation: {_safe(generation.reason)}", styles["Heading3"]))
                    story.append(Paragraph(f"<b>Prompt:</b> {_safe(generation.prompt)}", styles["Normal"]))
                    for item in generation.outputs.all():
                        marker = "Final" if item.is_final else "Output"
                        story.append(Paragraph(f"{marker}: {_safe(item.asset.original_filename)}", styles["StudioMeta"]))
            story.append(Spacer(1, 5 * mm))
        if "subtitles" in sections:
            for track in current_episode.subtitle_tracks.all():
                story.append(Paragraph(f"{track.language.upper()} / {track.get_kind_display()} subtitles", styles["Heading2"]))
                for line in track.lines.all():
                    story.append(Paragraph(f"{line.position + 1}. {_safe(line.text)}", styles["Normal"]))

    document.build(
        story,
        onFirstPage=lambda canvas, doc: _header_footer(canvas, doc, font, project.title),
        onLaterPages=lambda canvas, doc: _header_footer(canvas, doc, font, project.title),
    )
    return output.getvalue()


def generate_export(*, project, episode, sections, user):
    cutoff = timezone.now() - timedelta(hours=1)
    if ExportJob.objects.filter(requested_by=user, created_at__gte=cutoff).count() >= settings.STUDIO_EXPORT_RATE_PER_HOUR:
        raise ExportError("Export request limit reached. Please try again later.")
    selected = [item for item in sections if item in ALL_SECTIONS] or list(ALL_SECTIONS)
    job = ExportJob.objects.create(
        workspace=project.workspace, project=project, episode=episode,
        sections=selected, status=ExportJob.Status.RUNNING, requested_by=user, started_at=timezone.now(),
    )
    try:
        pdf_bytes = render_project_pdf(project, episode, selected)
        filename = f"{project.title[:80].strip() or 'project'}-{job.id}.pdf"
        uploaded = ContentFile(pdf_bytes, name=filename)
        uploaded.content_type = "application/pdf"
        asset = create_asset(
            user=user, workspace=project.workspace, project=project,
            uploaded=uploaded, kind=Asset.Kind.EXPORT,
        )
        job.output_asset = asset
        job.status = ExportJob.Status.SUCCESS
        job.completed_at = timezone.now()
        job.save(update_fields=["output_asset", "status", "completed_at"])
        audit(workspace=project.workspace, actor=user, action="EXPORT_GENERATED", instance=asset, metadata={"exportId": str(job.id), "sections": selected})
    except Exception as exc:
        job.status = ExportJob.Status.ERROR
        job.error_message = str(exc)[:1000]
        job.completed_at = timezone.now()
        job.save(update_fields=["status", "error_message", "completed_at"])
        raise
    return job
