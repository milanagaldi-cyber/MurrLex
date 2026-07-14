from allauth.mfa.models import Authenticator
from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError


class Command(BaseCommand):
    help = "Reset TOTP and recovery codes for a protected staff account."

    def add_arguments(self, parser):
        parser.add_argument("username")
        parser.add_argument("--reason", required=True)

    def handle(self, username, reason, **options):
        user_model = get_user_model()
        try:
            target = user_model.objects.get(username=username)
        except user_model.DoesNotExist as exc:
            raise CommandError("Target user does not exist.") from exc
        if not target.is_staff:
            raise CommandError("MFA recovery is restricted to staff accounts.")

        deleted, _ = Authenticator.objects.filter(
            user=target,
            type__in=[Authenticator.Type.TOTP, Authenticator.Type.RECOVERY_CODES],
        ).delete()
        target.admin_mfa_policy.mfa_required = True
        target.admin_mfa_policy.save(update_fields=["mfa_required", "updated_at"])
        self.stdout.write(
            self.style.SUCCESS(
                f"MFA reset for {target.get_username()} ({deleted} record(s)); "
                f"reason: {reason}. The next Admin visit requires enrollment."
            )
        )
