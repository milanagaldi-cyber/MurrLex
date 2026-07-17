import re

from allauth.core.exceptions import ImmediateHttpResponse
from allauth.socialaccount.adapter import DefaultSocialAccountAdapter
from allauth.socialaccount.models import SocialAccount
from django.conf import settings
from django.contrib.auth import get_user_model
from django.db import transaction
from django.http import HttpResponseForbidden
from django.utils import timezone
from google.auth.exceptions import GoogleAuthError
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token as google_id_token

from .models import GoogleOAuthAllowedUser
from .mfa_policy import user_requires_admin_mfa


class GoogleIdentityError(ValueError):
    pass


class GoogleOAuthGuardMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if request.path.startswith("/accounts/google/") and not settings.GOOGLE_OAUTH_ENABLED:
            return HttpResponseForbidden("Google sign-in is disabled.")
        return self.get_response(request)


def verify_google_id_token(raw_token: str) -> dict:
    if not raw_token:
        raise GoogleIdentityError("Google id_token is required.")
    if not settings.GOOGLE_OAUTH_CLIENT_ID:
        raise GoogleIdentityError("Google OAuth client is not configured.")
    try:
        claims = google_id_token.verify_oauth2_token(
            raw_token,
            google_requests.Request(),
            audience=settings.GOOGLE_OAUTH_CLIENT_ID,
        )
    except (GoogleAuthError, ValueError) as exc:
        raise GoogleIdentityError("Google id_token is invalid.") from exc
    if claims.get("iss") not in {"accounts.google.com", "https://accounts.google.com"}:
        raise GoogleIdentityError("Google token issuer is invalid.")
    if not claims.get("sub") or not claims.get("email"):
        raise GoogleIdentityError("Google identity is incomplete.")
    return claims


def is_google_identity_allowed(*, email: str, google_sub: str) -> bool:
    if not settings.GOOGLE_OAUTH_TEST_ALLOWLIST_ENABLED:
        return True
    normalized_email = email.strip().lower()
    if normalized_email in settings.GOOGLE_OAUTH_ALLOWED_EMAILS:
        return True
    if google_sub and google_sub in settings.GOOGLE_OAUTH_ALLOWED_SUBS:
        return True
    query = GoogleOAuthAllowedUser.objects.filter(is_active=True)
    return query.filter(email__iexact=normalized_email).exists() or (
        bool(google_sub) and query.filter(google_sub=google_sub).exists()
    )


def safe_google_profile(claims: dict) -> dict:
    return {
        "email": str(claims.get("email", "")).strip().lower(),
        "email_verified": bool(claims.get("email_verified")),
        "name": str(claims.get("name", ""))[:150],
        "picture": str(claims.get("picture", ""))[:500],
        "last_login_at": timezone.now().isoformat(),
    }


def _available_username(email: str) -> str:
    user_model = get_user_model()
    base = re.sub(r"[^a-zA-Z0-9_.-]", "-", email.split("@", 1)[0]).strip("-._") or "google-user"
    base = base[:140]
    candidate = base
    counter = 1
    while user_model.objects.filter(username__iexact=candidate).exists():
        counter += 1
        candidate = f"{base[:140-len(str(counter))]}-{counter}"
    return candidate


@transaction.atomic
def user_for_google_claims(claims: dict):
    if claims.get("email_verified") is not True:
        raise PermissionError("Google email is not verified.")
    google_sub = str(claims["sub"])
    email = str(claims["email"]).strip().lower()
    if not is_google_identity_allowed(email=email, google_sub=google_sub):
        raise PermissionError("Google account is not allowed for this staging environment.")

    account = SocialAccount.objects.select_related("user").filter(provider="google", uid=google_sub).first()
    if account is not None:
        user = account.user
    else:
        user_model = get_user_model()
        matches = list(user_model.objects.filter(email__iexact=email)[:2])
        if len(matches) > 1:
            raise PermissionError("Google account cannot be linked to an ambiguous local email.")
        user = matches[0] if matches else user_model(username=_available_username(email), email=email)
        if user.pk is None:
            user.set_unusable_password()
            user.first_name = str(claims.get("name", ""))[:150]
            user.is_active = True
            user.is_staff = False
            user.is_superuser = False
            user.save()
        account = SocialAccount.objects.create(provider="google", uid=google_sub, user=user)
    account.extra_data = safe_google_profile(claims)
    account.save(update_fields=["extra_data"])
    return user


class StagingSocialAccountAdapter(DefaultSocialAccountAdapter):
    def is_open_for_signup(self, request, sociallogin):
        # pre_social_login enforces the verified-email allowlist before a new user is saved.
        return settings.GOOGLE_OAUTH_ENABLED and sociallogin.account.provider == "google"

    def pre_social_login(self, request, sociallogin):
        if sociallogin.account.provider != "google":
            return
        if not settings.GOOGLE_OAUTH_ENABLED:
            raise ImmediateHttpResponse(HttpResponseForbidden("Google sign-in is disabled."))
        data = sociallogin.account.extra_data or {}
        email = str(data.get("email") or sociallogin.user.email or "").strip().lower()
        google_sub = str(sociallogin.account.uid or "")
        if data.get("email_verified") is not True:
            raise ImmediateHttpResponse(HttpResponseForbidden("Google email is not verified."))
        if not is_google_identity_allowed(email=email, google_sub=google_sub):
            raise ImmediateHttpResponse(
                HttpResponseForbidden("Google account is not allowed for this staging environment.")
            )
        sociallogin.account.extra_data = safe_google_profile(data)
        if sociallogin.is_existing:
            if sociallogin.user.is_staff and user_requires_admin_mfa(sociallogin.user):
                raise ImmediateHttpResponse(
                    HttpResponseForbidden("Protected staff accounts must use the secure Admin login.")
                )
            return
        matches = list(get_user_model().objects.filter(email__iexact=email)[:2])
        if len(matches) == 1 and matches[0].is_staff and user_requires_admin_mfa(matches[0]):
            raise ImmediateHttpResponse(
                HttpResponseForbidden("Protected staff accounts must use the secure Admin login.")
            )
        if len(matches) == 1 and not SocialAccount.objects.filter(user=matches[0], provider="google").exists():
            sociallogin.connect(request, matches[0])

    def save_user(self, request, sociallogin, form=None):
        user = super().save_user(request, sociallogin, form)
        user.is_staff = False
        user.is_superuser = False
        user.is_active = True
        user.save(update_fields=["is_staff", "is_superuser", "is_active"])
        return user
