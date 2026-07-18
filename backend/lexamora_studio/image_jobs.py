import logging

from django.core.exceptions import ValidationError
from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import close_old_connections
from django.utils import timezone
from django.utils.text import slugify

from lessons.ai_gateway import (
    ProviderError,
    generate_google_image_with_usage,
    generate_google_video_with_usage,
    generate_image_with_usage,
)

from .models import AiUsageLog, Asset, ImageGenerationJob
from .revisions import audit
from .storage import create_asset


logger = logging.getLogger(__name__)


def execute_image_generation_job(job_id):
    close_old_connections()
    job = ImageGenerationJob.objects.select_related(
        "prompt__scene__episode__project", "project", "workspace", "requested_by", "model_profile",
    ).get(id=job_id)
    if job.status != ImageGenerationJob.Status.RUNNING:
        return
    prompt = job.prompt
    project = job.project or prompt.scene.episode.project
    reference_map = {
        str(asset.id): asset
        for asset in Asset.objects.filter(
            id__in=job.reference_asset_ids,
            workspace=job.workspace,
            content_type__startswith="image/",
        )
    }
    defaults = job.model_profile.defaults if isinstance(job.model_profile.defaults, dict) else {}
    max_references = max(0, min(int(defaults.get("max_references", 3)), 20))
    reference_assets = [reference_map[item] for item in job.reference_asset_ids if item in reference_map][:max_references]
    references = []
    for reference in reference_assets:
        with reference.file.open("rb") as source:
            references.append((reference.original_filename, source.read(), reference.content_type))
    usage = AiUsageLog.objects.create(
        workspace=job.workspace,
        user=job.requested_by,
        prompt=prompt,
        action="GENERATE_VIDEO" if job.model_profile.media_type == job.model_profile.MediaType.VIDEO else "GENERATE_IMAGE",
        model=job.model_profile.model_id,
        status="STARTED",
        input_chars=len(job.request_prompt),
    )
    try:
        provider_options = {
            key: value for key, value in job.options.items()
            if key not in {"composition_preset", "first_frame_asset_id", "last_frame_asset_id"}
        }
        provider = job.model_profile.provider.strip().lower()
        if "google" in provider and job.model_profile.media_type == job.model_profile.MediaType.VIDEO:
            first_frame = reference_map.get(str(job.options.get("first_frame_asset_id") or ""))
            last_frame = reference_map.get(str(job.options.get("last_frame_asset_id") or ""))
            first_frame_image = next((item for asset, item in zip(reference_assets, references) if first_frame and asset.id == first_frame.id), None)
            last_frame_image = next((item for asset, item in zip(reference_assets, references) if last_frame and asset.id == last_frame.id), None)
            media_bytes, model, provider_usage, content_type, extension = generate_google_video_with_usage(
                job.request_prompt, model=job.model_profile.model_id, reference_images=references,
                first_frame_image=first_frame_image, last_frame_image=last_frame_image, **provider_options,
            )
        elif "google" in provider:
            media_bytes, model, provider_usage, content_type, extension = generate_google_image_with_usage(
                job.request_prompt, model=job.model_profile.model_id, reference_images=references, **provider_options,
            )
        else:
            openai_options = {
                key: value for key, value in provider_options.items()
                if key in {"size", "quality", "output_format", "output_compression", "background", "moderation"}
            }
            media_bytes, model, provider_usage = generate_image_with_usage(
                job.request_prompt, model=job.model_profile.model_id, reference_images=references, **openai_options,
            )
            extension = job.options.get("output_format", "png")
            content_type = {"png": "image/png", "jpeg": "image/jpeg", "webp": "image/webp"}[extension]
        uploaded = SimpleUploadedFile(
            f"ai-{slugify(prompt.title if prompt else project.title) or 'media'}-{timezone.now():%Y%m%d-%H%M%S}.{extension}",
            media_bytes,
            content_type=content_type,
        )
        asset = create_asset(
            user=job.requested_by,
            workspace=job.workspace,
            uploaded=uploaded,
            kind=Asset.Kind.GENERATION_OUTPUT,
            project=project,
            prompt=prompt,
        )
        if prompt is not None:
            prompt.reference_assets.remove(asset)
        asset.ai_metadata = {
            "provider": job.model_profile.provider,
            "model": model,
            "requestPrompt": job.request_prompt,
            "settings": job.options,
            "referenceAssetIds": [str(item.id) for item in reference_assets],
            "referenceCount": len(reference_assets),
            "generationJobId": str(job.id),
        }
        asset.save(update_fields=["ai_metadata", "updated_at"])
        usage.model = model
        usage.status = "SUCCESS"
        usage.output_chars = len(media_bytes)
        usage.input_tokens = provider_usage.get("input_tokens", 0)
        usage.output_tokens = provider_usage.get("output_tokens", 0)
        usage.total_tokens = provider_usage.get("total_tokens", 0)
        usage.save(update_fields=[
            "model", "status", "output_chars", "input_tokens", "output_tokens", "total_tokens",
        ])
        job.status = ImageGenerationJob.Status.SUCCESS
        job.result_asset = asset
        job.provider_model = model
        job.finished_at = timezone.now()
        job.save(update_fields=["status", "result_asset", "provider_model", "finished_at", "updated_at"])
        audit(
            workspace=job.workspace,
            actor=job.requested_by,
            action=("PROMPT_" if prompt else "PROJECT_") + ("VIDEO_GENERATED" if content_type.startswith("video/") else "IMAGE_GENERATED"),
            instance=prompt or project,
            metadata={
                "assetId": str(asset.id),
                "jobId": str(job.id),
                "model": model,
                "requestPrompt": job.request_prompt,
                "settings": job.options,
                "referenceAssetIds": [str(item.id) for item in reference_assets],
            },
        )
    except (ProviderError, ValidationError) as exc:
        usage.status = "ERROR"
        usage.error_code = "provider_error"
        usage.save(update_fields=["status", "error_code"])
        message = "; ".join(exc.messages) if isinstance(exc, ValidationError) else str(exc)
        _fail_job(job, message)
    except Exception:
        logger.exception("image_generation_job_failed", extra={"job_id": str(job.id)})
        usage.status = "ERROR"
        usage.error_code = "unexpected_error"
        usage.save(update_fields=["status", "error_code"])
        _fail_job(job, "Media generation returned an unexpected error")
    finally:
        close_old_connections()


def _fail_job(job, message):
    job.status = ImageGenerationJob.Status.ERROR
    job.error_message = str(message)[:2000]
    job.finished_at = timezone.now()
    job.save(update_fields=["status", "error_message", "finished_at", "updated_at"])
