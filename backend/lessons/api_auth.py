import hashlib
import secrets
from datetime import timedelta

import jwt
from django.conf import settings
from django.contrib.auth import get_user_model
from django.db.models import Q
from django.http import JsonResponse
from django.utils import timezone

from .models import ApiSession


def json_error(message: str, status: int = 400) -> JsonResponse:
    return JsonResponse({"status": "error", "error": message}, status=status)


def hash_refresh_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def user_payload(user) -> dict:
    return {"id": user.id, "username": user.get_username(), "email": user.email}


def _access_token(user, session: ApiSession) -> str:
    now = timezone.now()
    return jwt.encode(
        {
            "typ": "access",
            "sub": str(user.id),
            "sid": str(session.public_id),
            "iat": now,
            "exp": now + timedelta(minutes=settings.JWT_ACCESS_MINUTES),
        },
        settings.JWT_SIGNING_KEY,
        algorithm="HS256",
    )


def _new_refresh_token() -> str:
    return secrets.token_urlsafe(48)


def create_session(user, device_name: str = "") -> tuple[ApiSession, str]:
    refresh_token = _new_refresh_token()
    session = ApiSession.objects.create(
        user=user,
        refresh_token_hash=hash_refresh_token(refresh_token),
        device_name=device_name.strip()[:160],
        expires_at=timezone.now() + timedelta(days=settings.JWT_REFRESH_DAYS),
    )
    return session, refresh_token


def token_payload(user, session: ApiSession, refresh_token: str) -> dict:
    return {
        "status": "ok",
        "accessToken": _access_token(user, session),
        "refreshToken": refresh_token,
        "expiresInSeconds": settings.JWT_ACCESS_MINUTES * 60,
        "user": user_payload(user),
    }


def rotate_refresh_session(refresh_token: str) -> tuple[ApiSession, str] | None:
    token_hash = hash_refresh_token(refresh_token)
    session = (
        ApiSession.objects.select_related("user")
        .filter(refresh_token_hash=token_hash, revoked_at__isnull=True, expires_at__gt=timezone.now())
        .first()
    )
    if session is None:
        return None
    next_token = _new_refresh_token()
    session.refresh_token_hash = hash_refresh_token(next_token)
    session.save(update_fields=["refresh_token_hash", "last_used_at"])
    return session, next_token


def revoke_refresh_session(refresh_token: str) -> None:
    ApiSession.objects.filter(
        refresh_token_hash=hash_refresh_token(refresh_token), revoked_at__isnull=True
    ).update(revoked_at=timezone.now())


def authenticate_mobile_request(request):
    header = request.headers.get("Authorization", "")
    if not header.startswith("Bearer "):
        return None
    try:
        payload = jwt.decode(header.removeprefix("Bearer ").strip(), settings.JWT_SIGNING_KEY, algorithms=["HS256"])
        if payload.get("typ") != "access":
            return None
        session = ApiSession.objects.select_related("user").get(public_id=payload["sid"])
    except (jwt.PyJWTError, ApiSession.DoesNotExist, KeyError, ValueError):
        return None
    if not session.is_active or str(session.user_id) != str(payload.get("sub", "")):
        return None
    return session.user


def authenticate_login(login_value: str, password: str):
    user_model = get_user_model()
    candidates = user_model.objects.filter(Q(username__iexact=login_value) | Q(email__iexact=login_value)).distinct()
    if candidates.count() != 1:
        return None
    user = candidates.first()
    return user if user and user.check_password(password) and user.is_active else None
