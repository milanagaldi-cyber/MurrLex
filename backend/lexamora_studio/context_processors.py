from .models import AiUsageLog, StudioUserPreference
from .usage import token_summary


def studio_token_context(request):
    if not getattr(request, "user", None) or not request.user.is_authenticated:
        return {}
    preference = StudioUserPreference.objects.filter(user=request.user).first()
    return {
        "studio_token_summary": token_summary(request.user),
        "studio_recent_ai_usage": AiUsageLog.objects.filter(user=request.user).select_related(
            "workspace",
        )[:5],
        "studio_speech_preferences": {
            "language": preference.speech_language if preference else "",
            "continuous": preference.speech_continuous if preference else False,
            "interim": preference.speech_interim if preference else True,
        },
    }
