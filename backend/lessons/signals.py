from django.conf import settings
from django.db.models.signals import post_save
from django.dispatch import receiver

from .models import UserApiAccess


@receiver(post_save, sender=settings.AUTH_USER_MODEL)
def create_user_api_access(sender, instance, created, **kwargs):
    if created:
        UserApiAccess.objects.get_or_create(user=instance)
