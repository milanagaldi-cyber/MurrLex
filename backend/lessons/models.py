import uuid

from django.conf import settings
from django.core.exceptions import ValidationError
from django.db import models
from django.db.models import Sum
from django.utils import timezone


class ImmutableQuerySet(models.QuerySet):
    def update(self, **kwargs):
        raise ValidationError("Immutable records cannot be updated.")

    def delete(self):
        raise ValidationError("Immutable records cannot be deleted.")


ImmutableManager = models.Manager.from_queryset(ImmutableQuerySet)


class AdminMfaPolicy(models.Model):
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="admin_mfa_policy",
    )
    mfa_required = models.BooleanField(
        default=False,
        verbose_name="Require admin 2FA",
        help_text="When enabled, this staff user must enroll and use TOTP to enter Django Admin.",
    )
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = "admin 2FA policy"
        verbose_name_plural = "admin 2FA policies"
        ordering = ["user__username"]
        permissions = [("reset_staff_mfa", "Can reset staff MFA")]

    def __str__(self) -> str:
        return self.user.get_username()


class UserSecurityProfile(models.Model):
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="security_profile",
    )
    is_break_glass = models.BooleanField(
        default=False,
        help_text="Emergency owner account that must not be used for daily work.",
    )
    internal_note = models.TextField(blank=True)
    deleted_at = models.DateTimeField(null=True, blank=True)
    deletion_reason = models.TextField(blank=True)
    access_reviewed_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = "user security profile"
        verbose_name_plural = "user security profiles"
        permissions = [
            ("manage_user_status", "Can block, unblock, and soft-delete users"),
            ("adjust_credits", "Can issue and revoke credit adjustments"),
            ("manage_subscriptions", "Can change subscription status"),
        ]

    @property
    def is_soft_deleted(self) -> bool:
        return self.deleted_at is not None

    def mark_deleted(self, reason: str) -> None:
        self.deleted_at = timezone.now()
        self.deletion_reason = reason

    def __str__(self) -> str:
        return self.user.get_username()


class UserApiAccess(models.Model):
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="api_access",
    )
    ai_api_enabled = models.BooleanField(default=False, verbose_name="AI API access")
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = "API access"
        verbose_name_plural = "API access"
        ordering = ["user__username"]

    def __str__(self) -> str:
        return f"{self.user.get_username()}: {'enabled' if self.ai_api_enabled else 'disabled'}"


class SubscriptionPlan(models.Model):
    class RefillPeriod(models.TextChoices):
        DAILY = "daily", "Every day"
        WEEKLY = "weekly", "Every week"
        MONTHLY = "monthly", "Every month"

    code = models.SlugField(max_length=32, unique=True)
    name = models.CharField(max_length=80)
    refill_period = models.CharField(max_length=16, choices=RefillPeriod.choices, default=RefillPeriod.MONTHLY)
    refill_credits = models.PositiveBigIntegerField(default=0)
    is_active = models.BooleanField(default=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["refill_credits", "name"]

    def __str__(self) -> str:
        return self.name


class Subscription(models.Model):
    class Status(models.TextChoices):
        FREE = "free", "Free"
        ACTIVE = "active", "Active"
        PAST_DUE = "past_due", "Past due"
        CANCELLED = "cancelled", "Cancelled"

    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="subscription",
    )
    plan_code = models.CharField(max_length=64, default="free")
    plan = models.ForeignKey(
        SubscriptionPlan,
        on_delete=models.PROTECT,
        related_name="subscriptions",
        null=True,
        blank=True,
    )
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.FREE)
    valid_until = models.DateTimeField(null=True, blank=True)
    credits_frozen = models.BooleanField(default=False)
    freeze_reason = models.TextField(blank=True)
    next_refill_at = models.DateTimeField(null=True, blank=True)
    last_refilled_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["user__username"]
        permissions = [("manage_subscription_status", "Can perform subscription status actions")]

    def __str__(self) -> str:
        return f"{self.user.get_username()}: {self.status}"


class CreditLedger(models.Model):
    class Reason(models.TextChoices):
        PURCHASE = "purchase", "Purchase"
        USAGE = "usage", "Usage"
        SUPPORT_BONUS = "support_bonus", "Support bonus"
        ADMIN_ADJUSTMENT = "admin_adjustment", "Admin adjustment"
        ADMIN_DEDUCTION = "admin_deduction", "Admin deduction"
        AUTO_REFILL = "auto_refill", "Automatic subscription refill"
        PURCHASE_SIMULATION = "purchase_simulation", "Simulated purchase"
        REVERSAL = "reversal", "Reversal"

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="credit_entries")
    amount = models.IntegerField(help_text="Positive values add credits; negative values spend or revoke them.")
    reason = models.CharField(max_length=32, choices=Reason.choices)
    note = models.TextField(blank=True)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="created_credit_entries",
        null=True,
        blank=True,
    )
    reversal_of = models.OneToOneField(
        "self",
        on_delete=models.PROTECT,
        related_name="reversal",
        null=True,
        blank=True,
    )
    created_at = models.DateTimeField(auto_now_add=True)
    objects = ImmutableManager()

    class Meta:
        ordering = ["-created_at", "-id"]
        permissions = [("issue_credit_adjustment", "Can issue controlled credit adjustments")]

    @classmethod
    def balance_for(cls, user) -> int:
        return cls.objects.filter(user=user).aggregate(total=Sum("amount"))["total"] or 0

    def save(self, *args, **kwargs):
        if self.pk and type(self).objects.filter(pk=self.pk).exists():
            raise ValidationError("Credit ledger entries are immutable. Create a reversal entry instead.")
        return super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        raise ValidationError("Credit ledger entries cannot be deleted.")

    def __str__(self) -> str:
        return f"{self.user}: {self.amount} ({self.reason})"


class AdminAuditLog(models.Model):
    actor = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        related_name="admin_audit_events",
        null=True,
        blank=True,
    )
    actor_label = models.CharField(max_length=150, blank=True)
    action = models.CharField(max_length=100)
    target_type = models.CharField(max_length=100)
    target_id = models.CharField(max_length=100, blank=True)
    old_value = models.JSONField(default=dict, blank=True)
    new_value = models.JSONField(default=dict, blank=True)
    reason = models.TextField(blank=True)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    objects = ImmutableManager()

    class Meta:
        ordering = ["-created_at", "-id"]
        verbose_name = "admin audit event"
        verbose_name_plural = "admin audit log"

    def save(self, *args, **kwargs):
        if self.pk and type(self).objects.filter(pk=self.pk).exists():
            raise ValidationError("Audit events are immutable.")
        return super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        raise ValidationError("Audit events cannot be deleted.")

    def __str__(self) -> str:
        return f"{self.created_at:%Y-%m-%d %H:%M} {self.action}"


class GoogleOAuthAllowedUser(models.Model):
    email = models.EmailField(blank=True, db_index=True)
    google_sub = models.CharField(max_length=255, blank=True, db_index=True)
    is_active = models.BooleanField(default=True)
    note = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["email", "google_sub", "id"]
        constraints = [
            models.CheckConstraint(
                condition=~models.Q(email="") | ~models.Q(google_sub=""),
                name="google_oauth_allowlist_has_identity",
            ),
            models.UniqueConstraint(
                fields=["email"], condition=~models.Q(email=""), name="google_oauth_unique_allowed_email",
            ),
            models.UniqueConstraint(
                fields=["google_sub"], condition=~models.Q(google_sub=""), name="google_oauth_unique_allowed_sub",
            ),
        ]

    def save(self, *args, **kwargs):
        self.email = self.email.strip().lower()
        self.google_sub = self.google_sub.strip()
        return super().save(*args, **kwargs)

    def __str__(self) -> str:
        return self.email or self.google_sub


class Lesson(models.Model):
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="murrlex_lessons",
        null=True,
        blank=True,
    )
    external_id = models.CharField(max_length=255)
    title = models.CharField(max_length=255)
    card_kind = models.CharField(max_length=20, blank=True)
    source_language = models.CharField(max_length=100, blank=True)
    target_language = models.CharField(max_length=100, blank=True)
    lesson_info = models.TextField(blank=True)
    raw_json = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["title", "external_id"]
        constraints = [
            models.UniqueConstraint(fields=["owner", "external_id"], name="unique_lesson_external_id_per_owner")
        ]

    def __str__(self) -> str:
        return self.title


class Card(models.Model):
    lesson = models.ForeignKey(Lesson, on_delete=models.CASCADE, related_name="cards")
    external_card_id = models.CharField(max_length=255)
    native_value = models.TextField(blank=True)
    correct_value = models.TextField(blank=True)
    hint = models.TextField(blank=True)
    mistake = models.TextField(blank=True)
    card_kind = models.CharField(max_length=20, blank=True)
    source_language = models.CharField(max_length=100, blank=True)
    target_language = models.CharField(max_length=100, blank=True)
    raw_json = models.JSONField(default=dict, blank=True)
    stars = models.PositiveSmallIntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["lesson", "external_card_id", "id"]
        constraints = [
            models.UniqueConstraint(
                fields=["lesson", "external_card_id"],
                name="unique_card_external_id_per_lesson",
            )
        ]

    def __str__(self) -> str:
        return f"{self.lesson.title}: {self.external_card_id}"


class ImportLog(models.Model):
    class Status(models.TextChoices):
        SUCCESS = "success", "Success"
        ERROR = "error", "Error"

    source = models.CharField(max_length=100, blank=True)
    status = models.CharField(max_length=20, choices=Status.choices)
    external_lesson_id = models.CharField(max_length=255, blank=True)
    cards_count = models.PositiveIntegerField(default=0)
    error_message = models.TextField(blank=True)
    raw_payload = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at", "-id"]

    def __str__(self) -> str:
        return f"{self.status}: {self.external_lesson_id or 'no lesson id'}"


class ApiSession(models.Model):
    """Revocable mobile login session. Provider credentials never live here."""

    public_id = models.UUIDField(default=uuid.uuid4, unique=True, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="api_sessions")
    refresh_token_hash = models.CharField(max_length=64, unique=True)
    device_name = models.CharField(max_length=160, blank=True)
    expires_at = models.DateTimeField()
    revoked_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    last_used_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-last_used_at", "-id"]

    @property
    def is_active(self) -> bool:
        from django.utils import timezone

        return self.revoked_at is None and self.expires_at > timezone.now()

    def __str__(self) -> str:
        return f"{self.user} ({self.public_id})"


class ProviderCredential(models.Model):
    class Provider(models.TextChoices):
        OPENAI = "openai", "OpenAI"
        ELEVENLABS = "elevenlabs", "ElevenLabs"
        GOOGLE = "google", "Google AI Studio"

    provider = models.CharField(max_length=32, choices=Provider.choices, unique=True)
    encrypted_api_key = models.TextField(blank=True, editable=False)
    updated_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        related_name="updated_provider_credentials",
        null=True,
        blank=True,
        editable=False,
    )
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["provider"]

    @property
    def is_configured(self) -> bool:
        return bool(self.encrypted_api_key)

    def set_api_key(self, value: str) -> None:
        from .provider_credentials import encrypt_api_key

        self.encrypted_api_key = encrypt_api_key(value)

    def clear_api_key(self) -> None:
        self.encrypted_api_key = ""

    def __str__(self) -> str:
        return self.get_provider_display()


