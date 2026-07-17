from django.conf import settings
from django.core.mail import get_connection, send_mail
from django.core.management.base import BaseCommand, CommandError


class Command(BaseCommand):
    help = "Check SMTP connectivity and optionally send a test message without printing secrets."

    def add_arguments(self, parser):
        parser.add_argument("--send-to", dest="recipient", help="Send a test message to this address.")

    def handle(self, *args, **options):
        recipient = options.get("recipient")
        security = "SSL" if settings.EMAIL_USE_SSL else "STARTTLS" if settings.EMAIL_USE_TLS else "plain"
        self.stdout.write(
            f"SMTP: host={settings.EMAIL_HOST} port={settings.EMAIL_PORT} security={security} "
            f"user={'configured' if settings.EMAIL_HOST_USER else 'empty'} from={settings.DEFAULT_FROM_EMAIL}"
        )

        try:
            if recipient:
                sent = send_mail(
                    "MurrLex SMTP test",
                    "SMTP is configured correctly. No reply is required.",
                    settings.DEFAULT_FROM_EMAIL,
                    [recipient],
                    fail_silently=False,
                )
                if sent != 1:
                    raise CommandError("The mail backend did not confirm delivery.")
            else:
                connection = get_connection(fail_silently=False)
                connection.open()
                connection.close()
        except CommandError:
            raise
        except Exception as exc:
            raise CommandError(f"SMTP check failed: {exc.__class__.__name__}: {exc}") from exc

        suffix = f" Test message accepted for {recipient}." if recipient else " Connection succeeded."
        self.stdout.write(self.style.SUCCESS(f"SMTP check passed.{suffix}"))
