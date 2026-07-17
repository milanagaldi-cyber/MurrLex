from calendar import monthrange
from datetime import timedelta

from django.db import transaction
from django.utils import timezone

from .models import CreditLedger, Subscription, SubscriptionPlan


def next_refill_time(moment, period):
    if period == SubscriptionPlan.RefillPeriod.DAILY:
        return moment + timedelta(days=1)
    if period == SubscriptionPlan.RefillPeriod.WEEKLY:
        return moment + timedelta(days=7)
    month = moment.month + 1
    year = moment.year
    if month == 13:
        month = 1
        year += 1
    day = min(moment.day, monthrange(year, month)[1])
    return moment.replace(year=year, month=month, day=day)


@transaction.atomic
def apply_due_refill(subscription, *, now=None):
    now = now or timezone.now()
    subscription = Subscription.objects.select_for_update().select_related("plan").get(pk=subscription.pk)
    plan = subscription.plan
    if not plan or not plan.is_active or subscription.status != Subscription.Status.ACTIVE:
        return None
    if subscription.valid_until and subscription.valid_until <= now:
        return None
    if subscription.next_refill_at is None:
        subscription.next_refill_at = next_refill_time(now, plan.refill_period)
        subscription.save(update_fields=["next_refill_at", "updated_at"])
        return None
    if subscription.next_refill_at > now:
        return None
    entry = CreditLedger.objects.create(
        user=subscription.user,
        amount=plan.refill_credits,
        reason=CreditLedger.Reason.AUTO_REFILL,
        note=f"{plan.name} automatic refill",
    )
    subscription.last_refilled_at = now
    while subscription.next_refill_at <= now:
        subscription.next_refill_at = next_refill_time(subscription.next_refill_at, plan.refill_period)
    subscription.save(update_fields=["last_refilled_at", "next_refill_at", "updated_at"])
    return entry


def process_due_refills(*, now=None):
    now = now or timezone.now()
    processed = 0
    subscriptions = Subscription.objects.filter(
        status=Subscription.Status.ACTIVE,
        plan__is_active=True,
        next_refill_at__lte=now,
    ).select_related("plan", "user")
    for subscription in subscriptions.iterator():
        if apply_due_refill(subscription, now=now):
            processed += 1
    return processed
