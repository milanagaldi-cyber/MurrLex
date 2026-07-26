import time

from django.core.management.base import BaseCommand
from django.db import close_old_connections
from django.utils import timezone

from lexamora_studio.models import MovieRenderJob
from lexamora_studio.movie_rendering import process_movie_render


class Command(BaseCommand):
    help = "Render queued MurrCut timelines to MP4 rough cuts."

    def add_arguments(self, parser):
        parser.add_argument("--poll-interval", type=float, default=1.0)
        parser.add_argument("--once", action="store_true")

    def handle(self, *args, **options):
        poll_interval = max(0.2, options["poll_interval"])
        MovieRenderJob.objects.filter(
            status=MovieRenderJob.Status.RUNNING, cancel_requested=True,
        ).update(status=MovieRenderJob.Status.CANCELLED, completed_at=timezone.now(), progress=0)
        MovieRenderJob.objects.filter(
            status=MovieRenderJob.Status.RUNNING, cancel_requested=False,
        ).update(
            status=MovieRenderJob.Status.QUEUED, started_at=None, progress=0,
        )
        while True:
            job_id = self._claim_next_job()
            if job_id is None:
                if options["once"]:
                    return
                close_old_connections()
                time.sleep(poll_interval)
                continue
            try:
                process_movie_render(job_id)
            except Exception as exc:
                self.stderr.write(self.style.ERROR(f"Movie render {job_id} failed: {exc}"))
            if options["once"] and not MovieRenderJob.objects.filter(status=MovieRenderJob.Status.QUEUED).exists():
                return

    @staticmethod
    def _claim_next_job():
        close_old_connections()
        job_id = MovieRenderJob.objects.filter(
            status=MovieRenderJob.Status.QUEUED, cancel_requested=False,
        ).order_by("created_at").values_list("id", flat=True).first()
        if job_id is None:
            return None
        claimed = MovieRenderJob.objects.filter(
            id=job_id, status=MovieRenderJob.Status.QUEUED, cancel_requested=False,
        ).update(status=MovieRenderJob.Status.RUNNING)
        return job_id if claimed else None
