import hashlib
import io
from pathlib import Path

from PIL import Image, ImageOps, UnidentifiedImageError
from django.conf import settings
from django.core.files.base import ContentFile
from django.core.exceptions import ValidationError
from django.db import transaction
from django.utils import timezone

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
def create_asset(*, user, workspace, uploaded, kind, project=None, scene=None, character=None, prompt=None, prevent_duplicate=False):
    for related in (project,):
        if related is not None and related.workspace_id != workspace.id:
            raise ValidationError("Related object belongs to another workspace.")
    if scene is not None and scene.episode.project.workspace_id != workspace.id:
        raise ValidationError("Scene belongs to another workspace.")
    if character is not None and character.project.workspace_id != workspace.id:
        raise ValidationError("Character belongs to another workspace.")
    if prompt is not None and prompt.scene.episode.project.workspace_id != workspace.id:
        raise ValidationError("Prompt belongs to another workspace.")
    filename, content_type, image_data = validate_upload(uploaded)
    checksum = _hash_upload(uploaded)
    duplicate_query = Asset.objects.filter(
        workspace=workspace,
        original_filename=filename,
        size_bytes=uploaded.size,
        content_type=content_type,
    )
    if image_data is not None:
        duplicate_query = duplicate_query.filter(width=image_data[0], height=image_data[1])
    if prevent_duplicate and duplicate_query.exists():
        dimensions = f"{image_data[0]}x{image_data[1]}" if image_data else "unknown resolution"
        raise ValidationError(f"{filename} ({uploaded.size} bytes, {dimensions}) is already uploaded in this workspace.")
    asset = Asset(
        workspace=workspace,
        project=project,
        scene=scene,
        character=character,
        prompt=prompt,
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


@transaction.atomic
def crop_asset(*, asset, user, x, y, width, height):
    if not asset.project_id or not asset.content_type.startswith("image/") or not asset.file.name:
        raise ValidationError("Only active project images can be cropped.")
    try:
        with asset.file.open("rb") as source, Image.open(source) as image:
            image = ImageOps.exif_transpose(image)
            image_width, image_height = image.size
            left, top = int(x), int(y)
            crop_width, crop_height = int(width), int(height)
            if left < 0 or top < 0 or crop_width < 2 or crop_height < 2:
                raise ValueError
            if left + crop_width > image_width or top + crop_height > image_height:
                raise ValueError
            cropped = image.crop((left, top, left + crop_width, top + crop_height)).convert("RGB")
            output = io.BytesIO()
            cropped.save(output, "JPEG", quality=94, optimize=True)
    except (OSError, ValueError, TypeError) as exc:
        raise ValidationError("Choose a valid crop area inside the image.") from exc
    filename = f"{Path(asset.original_filename).stem}-crop.jpg"
    uploaded = ContentFile(output.getvalue(), name=filename)
    uploaded.content_type = "image/jpeg"
    cropped_asset = create_asset(
        user=user,
        workspace=asset.workspace,
        uploaded=uploaded,
        kind=Asset.Kind.OTHER,
        project=asset.project,
    )
    audit(
        workspace=asset.workspace,
        actor=user,
        action="ASSET_CROPPED",
        instance=cropped_asset,
        metadata={"sourceAssetId": str(asset.id), "crop": [left, top, crop_width, crop_height]},
    )
    return cropped_asset


@transaction.atomic
def trash_asset(*, asset, user):
    locked = Asset.all_objects.select_for_update().get(pk=asset.pk)
    if not locked.content_type.startswith("image/"):
        raise ValidationError("Only uploaded images can be moved to the image trash.")
    if locked.purged_at is not None:
        raise ValidationError("This image has already been permanently deleted.")
    locked.deleted_at = timezone.now()
    locked.deleted_by = user
    locked.updated_by = user
    locked.save(update_fields=["deleted_at", "deleted_by", "updated_by", "updated_at"])
    audit(workspace=locked.workspace, actor=user, action="ASSET_TRASHED", instance=locked)
    return locked


@transaction.atomic
def restore_asset(*, asset, user):
    locked = Asset.all_objects.select_for_update().get(pk=asset.pk)
    if locked.purged_at is not None:
        raise ValidationError("A permanently deleted image cannot be restored.")
    locked.deleted_at = None
    locked.deleted_by = None
    locked.updated_by = user
    locked.save(update_fields=["deleted_at", "deleted_by", "updated_by", "updated_at"])
    audit(workspace=locked.workspace, actor=user, action="ASSET_RESTORED", instance=locked)
    return locked


@transaction.atomic
def purge_asset(*, asset, user):
    locked = Asset.all_objects.select_for_update().get(pk=asset.pk)
    if not locked.content_type.startswith("image/"):
        raise ValidationError("Only uploaded images can be permanently deleted here.")
    if locked.purged_at is not None:
        return locked
    storage = locked.file.storage
    file_names = [name for name in (locked.file.name, locked.thumbnail.name) if name]
    locked.deleted_at = locked.deleted_at or timezone.now()
    locked.deleted_by = locked.deleted_by or user
    locked.purged_at = timezone.now()
    locked.purged_by = user
    locked.file.name = ""
    locked.thumbnail.name = ""
    locked.size_bytes = 0
    locked.checksum_sha256 = ""
    locked.width = None
    locked.height = None
    locked.updated_by = user
    locked.save(update_fields=[
        "deleted_at", "deleted_by", "purged_at", "purged_by", "file", "thumbnail",
        "size_bytes", "checksum_sha256", "width", "height", "updated_by", "updated_at",
    ])
    audit(workspace=locked.workspace, actor=user, action="ASSET_PURGED", instance=locked)

    def remove_files():
        for name in file_names:
            if storage.exists(name):
                storage.delete(name)

    transaction.on_commit(remove_files)
    return locked
