import time
from concurrent.futures import ThreadPoolExecutor

from django.core.management.base import BaseCommand
from django.db import close_old_connections

from lexamora_studio.media_processing import process_media_asset
from lexamora_studio.models import Asset


class Command(BaseCommand):
    help = "Generate browser proxies, thumbnails and waveforms for uploaded media."

    def add_arguments(self, parser):
        parser.add_argument("--concurrency", type=int, default=2)
        parser.add_argument("--poll-interval", type=float, default=1.0)
        parser.add_argument("--once", action="store_true")

    def handle(self, *args, **options):
        concurrency = max(1, min(options["concurrency"], 4))
        poll_interval = max(0.2, options["poll_interval"])
        Asset.all_objects.filter(processing_status=Asset.ProcessingStatus.PROCESSING).update(
            processing_status=Asset.ProcessingStatus.QUEUED,
            processing_started_at=None,
        )
        futures = {}
        with ThreadPoolExecutor(max_workers=concurrency, thread_name_prefix="murrlex-media") as executor:
            while True:
                for future in [item for item in futures if item.done()]:
                    asset_id = futures.pop(future)
                    try:
                        future.result()
                    except Exception as exc:
                        self.stderr.write(self.style.ERROR(f"Media asset {asset_id} failed: {exc}"))
                while len(futures) < concurrency:
                    asset_id = self._claim_next_asset()
                    if asset_id is None:
                        break
                    futures[executor.submit(process_media_asset, asset_id)] = asset_id
                if options["once"] and not futures and not Asset.all_objects.filter(
                    processing_status=Asset.ProcessingStatus.QUEUED,
                ).exists():
                    return
                close_old_connections()
                time.sleep(poll_interval)

    @staticmethod
    def _claim_next_asset():
        close_old_connections()
        asset_id = Asset.all_objects.filter(
            processing_status=Asset.ProcessingStatus.QUEUED,
            deleted_at__isnull=True,
            purged_at__isnull=True,
        ).order_by("created_at").values_list("id", flat=True).first()
        if asset_id is None:
            return None
        claimed = Asset.all_objects.filter(
            id=asset_id, processing_status=Asset.ProcessingStatus.QUEUED,
        ).update(processing_status=Asset.ProcessingStatus.PROCESSING)
        return asset_id if claimed else None
