from django.core.management.base import BaseCommand

from lessons.premium import process_due_refills


class Command(BaseCommand):
    help = "Issue due automatic subscription credit refills"

    def handle(self, *args, **options):
        count = process_due_refills()
        self.stdout.write(self.style.SUCCESS(f"Processed {count} subscription refill(s)"))
