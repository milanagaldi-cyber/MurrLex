from .models import AiUsageLog
from .usage import token_summary


def studio_token_context(request):
    if not getattr(request, "user", None) or not request.user.is_authenticated:
        return {}
    return {
        "studio_token_summary": token_summary(request.user),
        "studio_recent_ai_usage": AiUsageLog.objects.filter(user=request.user).select_related(
            "workspace",
        )[:5],
    }
