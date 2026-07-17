import io
from html import escape

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import Image as PdfImage
from reportlab.platypus import PageBreak, Paragraph, SimpleDocTemplate, Spacer


def build_episode_comic_pdf(episode, narrative):
    output = io.BytesIO()
    styles = getSampleStyleSheet()
    document = SimpleDocTemplate(output, pagesize=A4, leftMargin=16 * mm, rightMargin=16 * mm, topMargin=14 * mm, bottomMargin=14 * mm)
    story = [Paragraph(escape(f"{episode.project.title}: {episode.title}"), styles["Title"]), Spacer(1, 6 * mm)]
    if narrative:
        story.extend([Paragraph(escape(narrative).replace("\n", "<br/>"), styles["BodyText"]), Spacer(1, 7 * mm)])
    scenes = list(episode.scenes.prefetch_related("assets", "dialogue_lines__character").order_by("position", "number"))
    for index, scene in enumerate(scenes):
        story.append(Paragraph(escape(f"Scene {index + 1}. {scene.title}"), styles["Heading2"]))
        image = scene.assets.filter(content_type__startswith="image/", deleted_at__isnull=True).first()
        if image and image.file.name:
            try:
                with image.file.open("rb") as source:
                    data = io.BytesIO(source.read())
                story.extend([PdfImage(data, width=170 * mm, height=100 * mm, kind="proportional"), Spacer(1, 4 * mm)])
            except OSError:
                pass
        body = scene.description or scene.hook or ""
        if body:
            story.extend([Paragraph(escape(body).replace("\n", "<br/>"), styles["BodyText"]), Spacer(1, 3 * mm)])
        for line in scene.dialogue_lines.all():
            speaker = line.speaker or (line.character.name if line.character_id else "")
            story.append(Paragraph(f"<b>{escape(speaker)}</b>: {escape(line.text)}", styles["BodyText"]))
        if index + 1 < len(scenes):
            story.append(PageBreak())
    document.build(story)
    return output.getvalue()
