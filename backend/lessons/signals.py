from django.conf import settings
from django.contrib.auth.signals import user_logged_in
from django.db.models.signals import post_migrate, post_save
from django.dispatch import receiver

from .admin_roles import sync_admin_groups
from .models import AdminMfaPolicy, Subscription, UserApiAccess, UserSecurityProfile


@receiver(post_save, sender=settings.AUTH_USER_MODEL)
def create_user_api_access(sender, instance, created, **kwargs):
    if created:
        UserApiAccess.objects.get_or_create(user=instance)
        AdminMfaPolicy.objects.get_or_create(user=instance)
        UserSecurityProfile.objects.get_or_create(user=instance)
        Subscription.objects.get_or_create(user=instance)


@receiver(post_migrate, dispatch_uid="lessons.sync_daily_admin_group")
def create_daily_admin_group(**kwargs):
    sync_admin_groups()


@receiver(user_logged_in, dispatch_uid="lessons.audit_staff_login")
def audit_staff_login(sender, request, user, **kwargs):
    if not user.is_staff:
        return
    from .audit import record_audit_event

    record_audit_event(
        action="staff_login",
        target=user,
        actor=user,
        request=request,
        new_value={"is_superuser": user.is_superuser},
        reason="Successful staff authentication",
    )
