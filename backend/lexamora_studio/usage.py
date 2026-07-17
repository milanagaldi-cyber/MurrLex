from django.db.models import Sum

from .models import AiUsageLog, UserTokenQuota


DEFAULT_TOKEN_ALLOWANCE = 1_000_000


class TokenQuotaExceeded(Exception):
    pass


def token_summary(user):
    quota, _ = UserTokenQuota.objects.get_or_create(
        user=user, defaults={"allowance": DEFAULT_TOKEN_ALLOWANCE},
    )
    spent = AiUsageLog.objects.filter(user=user, status="SUCCESS").aggregate(
        value=Sum("total_tokens"),
    )["value"] or 0
    return {
        "allowance": quota.allowance,
        "spent": spent,
        "remaining": max(0, quota.allowance - spent),
    }


def require_token_quota(user):
    summary = token_summary(user)
    if summary["remaining"] <= 0:
        raise TokenQuotaExceeded("Your AI token allowance is exhausted")
    return summary
