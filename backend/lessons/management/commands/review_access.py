from allauth.mfa.models import Authenticator
from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand
from django.utils import timezone


class Command(BaseCommand):
    help = "Print a staff access review report without changing accounts."

    def handle(self, *args, **options):
        users = get_user_model().objects.filter(is_staff=True).prefetch_related("groups").order_by("username")
        self.stdout.write("username | superuser | active | 2FA required | TOTP | break-glass | last login | groups")
        for user in users:
            policy = user.admin_mfa_policy
            profile = user.security_profile
            totp = Authenticator.objects.filter(user=user, type=Authenticator.Type.TOTP).exists()
            groups = ",".join(user.groups.values_list("name", flat=True)) or "-"
            self.stdout.write(
                " | ".join(
                    [
                        user.get_username(),
                        str(user.is_superuser),
                        str(user.is_active),
                        str(policy.mfa_required),
                        str(totp),
                        str(profile.is_break_glass),
                        timezone.localtime(user.last_login).isoformat() if user.last_login else "never",
                        groups,
                    ]
                )
            )
