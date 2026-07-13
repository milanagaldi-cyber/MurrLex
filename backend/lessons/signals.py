from django.conf import settings
from django.db.models.signals import post_migrate, post_save
from django.dispatch import receiver

from .admin_roles import sync_admin_groups
from .models import UserApiAccess


@receiver(post_save, sender=settings.AUTH_USER_MODEL)
def create_user_api_access(sender, instance, created, **kwargs):
    if created:
        UserApiAccess.objects.get_or_create(user=instance)


@receiver(post_migrate, dispatch_uid="lessons.sync_daily_admin_group")
def create_daily_admin_group(**kwargs):
    sync_admin_groups()
