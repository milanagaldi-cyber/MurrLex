from django.db.models import Sum

from lessons.models import CreditLedger, Subscription
from lessons.premium import apply_due_refill

from .models import AiUsageLog, StudioUserPreference, UserTokenQuota


DEFAULT_TOKEN_ALLOWANCE = 1_000_000


class TokenQuotaExceeded(Exception):
    pass


def token_summary(user):
    subscription, _ = Subscription.objects.get_or_create(user=user)
    apply_due_refill(subscription)
    subscription.refresh_from_db()
    quota, _ = UserTokenQuota.objects.get_or_create(
        user=user, defaults={"allowance": DEFAULT_TOKEN_ALLOWANCE},
    )
    spent = AiUsageLog.objects.filter(user=user, status="SUCCESS").aggregate(
        value=Sum("total_tokens"),
    )["value"] or 0
    adjustments = CreditLedger.balance_for(user)
    allowance = max(0, quota.allowance + adjustments)
    return {
        "allowance": allowance,
        "base_allowance": quota.allowance,
        "credit_adjustments": adjustments,
        "spent": spent,
        "remaining": max(0, allowance - spent),
        "frozen": subscription.credits_frozen,
        "plan": subscription.plan.name if subscription.plan_id else "Free",
        "plan_code": subscription.plan.code if subscription.plan_id else "free",
        "next_refill_at": subscription.next_refill_at,
    }


def require_token_quota(user):
    preference = StudioUserPreference.objects.filter(user=user).only("ai_enabled").first()
    if preference is not None and not preference.ai_enabled:
        raise TokenQuotaExceeded("AI use is paused in your Lexamora Studio header")
    summary = token_summary(user)
    if summary["frozen"]:
        raise TokenQuotaExceeded("Your AI credits are temporarily frozen. Contact support.")
    if summary["remaining"] <= 0:
        raise TokenQuotaExceeded("Your AI token allowance is exhausted")
    return summary
