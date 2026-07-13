from django.conf import settings

from .provider_credentials import user_has_ai_access


def oauth_status(request):
    google_app = settings.SOCIALACCOUNT_PROVIDERS.get("google", {}).get("APP", {})
    return {
        "google_oauth_enabled": settings.GOOGLE_OAUTH_ENABLED and bool(google_app.get("client_id") and google_app.get("secret")),
        "google_oauth_redirect_uri": settings.GOOGLE_OAUTH_REDIRECT_URI,
        "public_signup_enabled": settings.PUBLIC_SIGNUP_ENABLED,
        "murrlex_ai_access": user_has_ai_access(request.user),
    }
