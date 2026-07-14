def security_profile_for(user):
    from .models import AdminMfaPolicy

    profile, _ = AdminMfaPolicy.objects.get_or_create(user=user)
    return profile


def user_requires_admin_mfa(user) -> bool:
    if not user.is_authenticated or not user.is_staff:
        return False
    return security_profile_for(user).mfa_required


def user_has_totp(user) -> bool:
    from allauth.mfa.models import Authenticator

    return Authenticator.objects.filter(user=user, type=Authenticator.Type.TOTP).exists()
