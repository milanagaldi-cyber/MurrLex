import time
from concurrent.futures import ThreadPoolExecutor

from django.core.management.base import BaseCommand
from django.db import close_old_connections
from django.utils import timezone

from lexamora_studio.image_jobs import execute_image_generation_job
from lexamora_studio.models import ImageGenerationJob


class Command(BaseCommand):
    help = "Process persistent OpenAI image generation jobs."

    def add_arguments(self, parser):
        parser.add_argument("--concurrency", type=int, default=5)
        parser.add_argument("--poll-interval", type=float, default=0.5)
        parser.add_argument("--once", action="store_true")

    def handle(self, *args, **options):
        concurrency = max(1, min(options["concurrency"], 10))
        poll_interval = max(0.1, options["poll_interval"])
        ImageGenerationJob.objects.filter(status=ImageGenerationJob.Status.RUNNING).update(
            status=ImageGenerationJob.Status.QUEUED,
            started_at=None,
        )
        futures = {}
        with ThreadPoolExecutor(max_workers=concurrency, thread_name_prefix="murrlex-image") as executor:
            while True:
                finished = {future for future in futures if future.done()}
                for future in finished:
                    job_id = futures.pop(future)
                    try:
                        future.result()
                    except Exception as exc:
                        ImageGenerationJob.objects.filter(
                            id=job_id,
                            status=ImageGenerationJob.Status.RUNNING,
                        ).update(
                            status=ImageGenerationJob.Status.ERROR,
                            error_message=str(exc)[:2000],
                            finished_at=timezone.now(),
                        )
                        self.stderr.write(self.style.ERROR(f"Image generation worker task failed: {exc}"))
                while len(futures) < concurrency:
                    job_id = self._claim_next_job()
                    if job_id is None:
                        break
                    futures[executor.submit(execute_image_generation_job, job_id)] = job_id
                if options["once"] and not futures and not ImageGenerationJob.objects.filter(
                    status=ImageGenerationJob.Status.QUEUED,
                ).exists():
                    return
                close_old_connections()
                time.sleep(poll_interval)

    @staticmethod
    def _claim_next_job():
        close_old_connections()
        job_id = ImageGenerationJob.objects.filter(
            status=ImageGenerationJob.Status.QUEUED,
        ).order_by("created_at").values_list("id", flat=True).first()
        if job_id is None:
            return None
        claimed = ImageGenerationJob.objects.filter(
            id=job_id,
            status=ImageGenerationJob.Status.QUEUED,
        ).update(status=ImageGenerationJob.Status.RUNNING, started_at=timezone.now())
        return job_id if claimed else None
