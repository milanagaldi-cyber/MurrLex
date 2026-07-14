from django.shortcuts import redirect
from django.urls import reverse

from .mfa_policy import user_has_totp, user_requires_admin_mfa


class StaffMFARequiredMiddleware:
    allowed_prefixes = (
        "/accounts/2fa/",
        "/accounts/logout/",
        "/admin/logout/",
        "/static/",
    )

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        user = request.user
        if (
            request.path.startswith("/admin/")
            and user.is_authenticated
            and user_requires_admin_mfa(user)
            and not any(request.path.startswith(prefix) for prefix in self.allowed_prefixes)
            and not user_has_totp(user)
        ):
            return redirect(f"{reverse('mfa_activate_totp')}?next={request.path}")
        return self.get_response(request)
