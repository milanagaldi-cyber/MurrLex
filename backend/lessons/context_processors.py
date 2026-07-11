from django.conf import settings

from .provider_credentials import user_has_ai_access


def oauth_status(request):
    return {
        "google_oauth_enabled": settings.GOOGLE_OAUTH_ENABLED,
        "google_oauth_redirect_uri": "https://ml-staging-api.lexaailabs.com/accounts/google/login/callback/",
        "murrlex_ai_access": user_has_ai_access(request.user),
    }
