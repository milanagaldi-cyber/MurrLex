from __future__ import annotations

from typing import Any


def request_ip(request) -> str | None:
    if request is None:
        return None
    forwarded = request.META.get("HTTP_X_FORWARDED_FOR", "")
    if forwarded:
        return forwarded.split(",", 1)[0].strip() or None
    return request.META.get("REMOTE_ADDR") or None


def record_audit_event(
    *,
    action: str,
    target,
    actor=None,
    actor_label: str = "",
    request=None,
    old_value: dict[str, Any] | None = None,
    new_value: dict[str, Any] | None = None,
    reason: str = "",
):
    from .models import AdminAuditLog

    return AdminAuditLog.objects.create(
        actor=actor,
        actor_label=actor_label or (actor.get_username() if actor else ""),
        action=action,
        target_type=target._meta.label if hasattr(target, "_meta") else type(target).__name__,
        target_id=str(getattr(target, "pk", "") or ""),
        old_value=old_value or {},
        new_value=new_value or {},
        reason=reason,
        ip_address=request_ip(request),
    )
