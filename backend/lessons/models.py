import uuid

from django.conf import settings
from django.db import models


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


class Lesson(models.Model):
    external_id = models.CharField(max_length=255, unique=True)
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
