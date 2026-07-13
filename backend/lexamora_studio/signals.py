from django.db.models.signals import post_save
from django.dispatch import receiver

from lessons.models import UserApiAccess

from .models import ProjectMembership


@receiver(post_save, sender=ProjectMembership)
def grant_project_member_ai_access(sender, instance, **kwargs):
    if not instance.is_active:
        return
    access, _ = UserApiAccess.objects.get_or_create(user=instance.user)
    if not access.ai_api_enabled:
        access.ai_api_enabled = True
        access.save(update_fields=["ai_api_enabled", "updated_at"])
