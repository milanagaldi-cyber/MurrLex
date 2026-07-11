from django.contrib.auth import get_user_model
from django import template
from django.utils import timezone

from lessons.models import ApiSession, Card, ImportLog, Lesson, ProviderCredential, UserApiAccess


register = template.Library()


@register.simple_tag(takes_context=True)
def admin_dashboard_stats(context):
    user = context["request"].user
    stats = {}

    if user.has_perm("auth.view_user"):
        stats["users"] = get_user_model().objects.count()
    if user.has_perm("lessons.view_userapiaccess"):
        stats["api_access"] = UserApiAccess.objects.count()
        stats["api_access_enabled"] = UserApiAccess.objects.filter(ai_api_enabled=True).count()
    if user.has_perm("lessons.view_providercredential"):
        stats["provider_credentials"] = ProviderCredential.objects.exclude(encrypted_api_key="").count()
    if user.has_perm("lessons.view_apisession"):
        stats["active_sessions"] = ApiSession.objects.filter(
            revoked_at__isnull=True,
            expires_at__gt=timezone.now(),
        ).count()
    if user.has_perm("lessons.view_lesson"):
        stats["lessons"] = Lesson.objects.count()
    if user.has_perm("lessons.view_card"):
        stats["cards"] = Card.objects.count()
    if user.has_perm("lessons.view_importlog"):
        stats["import_logs"] = ImportLog.objects.count()

    return stats
