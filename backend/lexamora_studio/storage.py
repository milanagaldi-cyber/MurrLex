import hashlib
import io
from pathlib import Path

from PIL import Image, ImageOps, UnidentifiedImageError
from django.conf import settings
from django.core.files.base import ContentFile
from django.core.exceptions import ValidationError
from django.db import transaction

from .models import Asset
from .revisions import audit


ALLOWED_UPLOADS = {
    ".jpg": {"image/jpeg"},
    ".jpeg": {"image/jpeg"},
    ".png": {"image/png"},
    ".webp": {"image/webp"},
    ".pdf": {"application/pdf"},
    ".docx": {"application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
    ".mp4": {"video/mp4"},
}
IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}


def _hash_upload(uploaded):
    digest = hashlib.sha256()
    for chunk in uploaded.chunks():
        digest.update(chunk)
    uploaded.seek(0)
    return digest.hexdigest()


def _validated_image(uploaded):
    try:
        with Image.open(uploaded) as image:
            image.verify()
        uploaded.seek(0)
        with Image.open(uploaded) as image:
            image = ImageOps.exif_transpose(image)
            width, height = image.size
            thumbnail = image.convert("RGB")
            thumbnail.thumbnail((640, 640))
            output = io.BytesIO()
            thumbnail.save(output, "JPEG", quality=85, optimize=True)
    except (UnidentifiedImageError, OSError, ValueError) as exc:
        raise ValidationError("The uploaded image is invalid or damaged.") from exc
    finally:
        uploaded.seek(0)
    return width, height, output.getvalue()


def validate_upload(uploaded):
    filename = Path(uploaded.name).name
    suffix = Path(filename).suffix.lower()
    content_type = (getattr(uploaded, "content_type", "") or "").lower()
    if suffix not in ALLOWED_UPLOADS or content_type not in ALLOWED_UPLOADS[suffix]:
        raise ValidationError("Unsupported file type.")
    if uploaded.size <= 0 or uploaded.size > settings.STUDIO_MAX_UPLOAD_BYTES:
        raise ValidationError(f"File size must be between 1 and {settings.STUDIO_MAX_UPLOAD_BYTES} bytes.")
    image_data = _validated_image(uploaded) if content_type in IMAGE_TYPES else None
    return filename, content_type, image_data


@transaction.atomic
def create_asset(*, user, workspace, uploaded, kind, project=None, scene=None, character=None):
    for related in (project,):
        if related is not None and related.workspace_id != workspace.id:
            raise ValidationError("Related object belongs to another workspace.")
    if scene is not None and scene.episode.project.workspace_id != workspace.id:
        raise ValidationError("Scene belongs to another workspace.")
    if character is not None and character.project.workspace_id != workspace.id:
        raise ValidationError("Character belongs to another workspace.")
    filename, content_type, image_data = validate_upload(uploaded)
    checksum = _hash_upload(uploaded)
    asset = Asset(
        workspace=workspace,
        project=project,
        scene=scene,
        character=character,
        kind=kind,
        original_filename=filename,
        content_type=content_type,
        size_bytes=uploaded.size,
        checksum_sha256=checksum,
        created_by=user,
        updated_by=user,
    )
    asset.file.save(filename, uploaded, save=False)
    if image_data is not None:
        asset.width, asset.height, thumbnail = image_data
        asset.thumbnail.save("thumbnail.jpg", ContentFile(thumbnail), save=False)
    asset.full_clean()
    asset.save()
    audit(workspace=workspace, actor=user, action="ASSET_UPLOAD", instance=asset, metadata={"filename": filename, "sizeBytes": uploaded.size})
    return asset
