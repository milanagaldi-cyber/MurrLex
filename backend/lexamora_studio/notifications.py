import logging

from django.conf import settings
from django.core.mail import send_mail
from django.db import transaction


logger = logging.getLogger("lexamora_studio")


def notify_access_granted(*, user, entity_name, entity_kind, url, granted_by):
    email = (user.email or "").strip()
    if not email:
        return

    subject = f"Access granted to {entity_kind}: {entity_name}"
    message = (
        f"{granted_by.get_full_name() or granted_by.username} shared the {entity_kind} "
        f'"{entity_name}" with you in Lexamora Studio.\n\nOpen it: {url}'
    )

    def deliver():
        try:
            send_mail(subject, message, settings.DEFAULT_FROM_EMAIL, [email], fail_silently=False)
        except Exception:
            logger.exception("studio_access_email_failed", extra={"recipient_user_id": str(user.id), "entity_kind": entity_kind})

    transaction.on_commit(deliver)
