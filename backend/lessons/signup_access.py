from django.conf import settings

from .models import GoogleOAuthAllowedUser


def is_registration_email_allowed(email: str) -> bool:
    if not settings.REGISTRATION_ALLOWLIST_ENABLED:
        return True

    normalized_email = email.strip().lower()
    if normalized_email in settings.GOOGLE_OAUTH_ALLOWED_EMAILS:
        return True

    return GoogleOAuthAllowedUser.objects.filter(
        email__iexact=normalized_email,
        is_active=True,
    ).exists()
