from django.core.management.base import BaseCommand

from lessons.admin_roles import sync_admin_groups


class Command(BaseCommand):
    help = "Create or synchronize the standard MurrLex staff role groups."

    def handle(self, *args, **options):
        groups = sync_admin_groups()
        for name, group in groups.items():
            self.stdout.write(f"{name}: {group.permissions.count()} permissions")
        self.stdout.write(self.style.SUCCESS("MurrLex role groups are synchronized."))
