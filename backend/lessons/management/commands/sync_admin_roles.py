from django.core.management.base import BaseCommand

from lessons.admin_roles import sync_admin_groups


class Command(BaseCommand):
    help = "Create or refresh the standard MurrLex administrator groups."

    def handle(self, *args, **options):
        groups = sync_admin_groups()
        for group in groups.values():
            self.stdout.write(f"{group.name}: {group.permissions.count()} permissions")
        self.stdout.write(self.style.SUCCESS(f"Synchronized {len(groups)} administrator groups."))
