from django.core.management.base import BaseCommand

from lessons.admin_roles import sync_daily_admin_group


class Command(BaseCommand):
    help = "Create or refresh the restricted Daily Admin permission group."

    def handle(self, *args, **options):
        group = sync_daily_admin_group()
        self.stdout.write(
            self.style.SUCCESS(
                f"{group.name} synchronized with {group.permissions.count()} permissions."
            )
        )
