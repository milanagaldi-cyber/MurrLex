import logging

from django.core.exceptions import ValidationError
from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import close_old_connections
from django.utils import timezone
from django.utils.text import slugify

from lessons.ai_gateway import ProviderError, generate_image_with_usage

from .models import AiUsageLog, Asset, ImageGenerationJob
from .revisions import audit
from .storage import create_asset


logger = logging.getLogger(__name__)


def execute_image_generation_job(job_id):
    close_old_connections()
    job = ImageGenerationJob.objects.select_related(
        "prompt__scene__episode__project", "workspace", "requested_by", "model_profile",
    ).get(id=job_id)
    if job.status != ImageGenerationJob.Status.RUNNING:
        return
    prompt = job.prompt
    project = prompt.scene.episode.project
    reference_map = {
        str(asset.id): asset
        for asset in Asset.objects.filter(
            id__in=job.reference_asset_ids,
            workspace=job.workspace,
            content_type__startswith="image/",
        ).exclude(kind=Asset.Kind.GENERATION_OUTPUT)
    }
    reference_assets = [reference_map[item] for item in job.reference_asset_ids if item in reference_map][:3]
    references = []
    for reference in reference_assets:
        with reference.file.open("rb") as source:
            references.append((reference.original_filename, source.read(), reference.content_type))
    usage = AiUsageLog.objects.create(
        workspace=job.workspace,
        user=job.requested_by,
        prompt=prompt,
        action="GENERATE_IMAGE",
        model=job.model_profile.model_id,
        status="STARTED",
        input_chars=len(job.request_prompt),
    )
    try:
        image_bytes, model, provider_usage = generate_image_with_usage(
            job.request_prompt,
            model=job.model_profile.model_id,
            reference_images=references,
            **job.options,
        )
        extension = job.options["output_format"]
        content_type = {"png": "image/png", "jpeg": "image/jpeg", "webp": "image/webp"}[extension]
        uploaded = SimpleUploadedFile(
            f"openai-{slugify(prompt.title or 'prompt') or 'prompt'}-{timezone.now():%Y%m%d-%H%M%S}.{extension}",
            image_bytes,
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
        prompt.reference_assets.remove(asset)
        asset.ai_metadata = {
            "provider": "OpenAI",
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
        usage.output_chars = len(image_bytes)
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
            action="PROMPT_IMAGE_GENERATED",
            instance=prompt,
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
        _fail_job(job, "Image generation returned an unexpected error")
    finally:
        close_old_connections()


def _fail_job(job, message):
    job.status = ImageGenerationJob.Status.ERROR
    job.error_message = str(message)[:2000]
    job.finished_at = timezone.now()
    job.save(update_fields=["status", "error_message", "finished_at", "updated_at"])
