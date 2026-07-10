from django.conf import settings


def oauth_status(request):
    return {
        "google_oauth_enabled": settings.GOOGLE_OAUTH_ENABLED,
        "google_oauth_redirect_uri": "https://ml-staging-api.lexaailabs.com/accounts/google/login/callback/",
    }
