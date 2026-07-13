from django import forms
from django.contrib import admin
from django.contrib.auth import get_user_model
from django.contrib.auth.admin import GroupAdmin, UserAdmin
from django.contrib.auth.models import Group
from django.shortcuts import redirect
from django.urls import reverse
from django.utils import timezone

from .models import ApiSession, Card, GoogleOAuthAllowedUser, ImportLog, Lesson, ProviderCredential, UserApiAccess


admin.site.enable_nav_sidebar = False
admin.site.index_template = "admin/index.html"
admin.site.index_title = "Server dashboard"


User = get_user_model()
admin.site.unregister(User)
admin.site.unregister(Group)


@admin.register(User)
class MurrLexUserAdmin(UserAdmin):
    """Keep routine admins useful without letting them grant themselves power."""

    def get_readonly_fields(self, request, obj=None):
        readonly = list(super().get_readonly_fields(request, obj))
        if not request.user.is_superuser:
            readonly.extend(("is_staff", "is_superuser", "groups", "user_permissions"))
        return tuple(dict.fromkeys(readonly))

    def has_change_permission(self, request, obj=None):
        allowed = super().has_change_permission(request, obj)
        if obj is not None and obj.is_superuser and not request.user.is_superuser:
            return False
        return allowed

    def has_delete_permission(self, request, obj=None):
        if not request.user.is_superuser:
            return False
        return super().has_delete_permission(request, obj)


@admin.register(Group)
class MurrLexGroupAdmin(GroupAdmin):
    list_display = ("name", "members_total", "permissions_total")
    search_fields = ("name",)

    @admin.display(description="Members")
    def members_total(self, group):
        return group.user_set.count()

    @admin.display(description="Permissions")
    def permissions_total(self, group):
        return group.permissions.count()


@admin.register(GoogleOAuthAllowedUser)
class GoogleOAuthAllowedUserAdmin(admin.ModelAdmin):
    list_display = ("email", "google_sub", "is_active", "note", "updated_at")
    list_editable = ("is_active",)
    list_filter = ("is_active",)
    search_fields = ("email", "google_sub", "note")
    readonly_fields = ("created_at", "updated_at")


@admin.register(UserApiAccess)
class UserApiAccessAdmin(admin.ModelAdmin):
    list_display = ("username", "email", "account_active", "ai_api_enabled", "updated_at")
    list_editable = ("ai_api_enabled",)
    list_filter = ("ai_api_enabled", "user__is_active")
    search_fields = ("user__username", "user__email")
    readonly_fields = ("user", "updated_at")
    list_select_related = ("user",)
    actions = ("enable_ai_access", "disable_ai_access")

    def has_add_permission(self, request):
        return False

    def add_view(self, request, form_url="", extra_context=None):
        return redirect(reverse("admin:lessons_userapiaccess_changelist"))

    @admin.action(description="Enable AI access for selected users")
    def enable_ai_access(self, request, queryset):
        updated = queryset.update(ai_api_enabled=True, updated_at=timezone.now())
        self.message_user(request, f"AI access enabled for {updated} user(s).")

    @admin.action(description="Disable AI access for selected users")
    def disable_ai_access(self, request, queryset):
        updated = queryset.update(ai_api_enabled=False, updated_at=timezone.now())
        self.message_user(request, f"AI access disabled for {updated} user(s).")

    @admin.display(ordering="user__username", description="Username")
    def username(self, access):
        return access.user.get_username()

    @admin.display(ordering="user__email", description="Email")
    def email(self, access):
        return access.user.email

    @admin.display(boolean=True, ordering="user__is_active", description="Active account")
    def account_active(self, access):
        return access.user.is_active


class CardInline(admin.TabularInline):
    model = Card
    extra = 0
    fields = ("external_card_id", "native_value", "correct_value", "card_kind", "stars")
    readonly_fields = ()


@admin.register(Lesson)
class LessonAdmin(admin.ModelAdmin):
    list_display = (
        "title",
        "owner",
        "external_id",
        "card_kind",
        "source_language",
        "target_language",
        "lesson_info",
        "cards_total",
        "updated_at",
    )
    search_fields = ("title", "external_id", "owner__username", "source_language", "target_language", "lesson_info")
    list_filter = ("owner", "card_kind", "source_language", "target_language")
    readonly_fields = ("created_at", "updated_at")
    inlines = [CardInline]

    @admin.display(description="Cards")
    def cards_total(self, lesson: Lesson) -> int:
        return lesson.cards.count()


@admin.register(Card)
class CardAdmin(admin.ModelAdmin):
    list_display = (
        "external_card_id",
        "lesson",
        "card_kind",
        "source_language",
        "target_language",
        "stars",
        "updated_at",
    )
    search_fields = ("external_card_id", "native_value", "correct_value", "mistake", "lesson__title")
    list_filter = ("card_kind", "source_language", "target_language", "stars")
    readonly_fields = ("created_at", "updated_at")


@admin.register(ImportLog)
class ImportLogAdmin(admin.ModelAdmin):
    list_display = ("created_at", "status", "source", "external_lesson_id", "cards_count")
    search_fields = ("source", "external_lesson_id", "error_message")
    list_filter = ("status", "source")
    readonly_fields = ("created_at",)


@admin.register(ApiSession)
class ApiSessionAdmin(admin.ModelAdmin):
    list_display = ("user", "device_name", "created_at", "last_used_at", "expires_at", "revoked_at")
    search_fields = ("user__username", "user__email", "device_name")
    readonly_fields = ("public_id", "refresh_token_hash", "created_at", "last_used_at")


class ProviderCredentialAdminForm(forms.ModelForm):
    api_key = forms.CharField(required=False, widget=forms.PasswordInput(render_value=False))
    clear_key = forms.BooleanField(required=False, label="Remove saved key")

    class Meta:
        model = ProviderCredential
        fields = ("provider",)

    def clean(self):
        cleaned = super().clean()
        if not self.instance.pk and not cleaned.get("api_key"):
            raise forms.ValidationError("A provider key is required for a new provider configuration.")
        return cleaned

    def save(self, commit=True):
        credential = super().save(commit=False)
        if self.cleaned_data.get("clear_key"):
            credential.clear_api_key()
        elif self.cleaned_data.get("api_key"):
            credential.set_api_key(self.cleaned_data["api_key"])
        if commit:
            credential.save()
        return credential


@admin.register(ProviderCredential)
class ProviderCredentialAdmin(admin.ModelAdmin):
    form = ProviderCredentialAdminForm
    list_display = ("provider", "configured", "updated_at", "updated_by")
    readonly_fields = ("updated_at", "updated_by")
    fields = ("provider", "api_key", "clear_key", "updated_at", "updated_by")

    @admin.display(boolean=True, description="Configured")
    def configured(self, credential: ProviderCredential) -> bool:
        return credential.is_configured

    def save_model(self, request, obj, form, change):
        obj.updated_by = request.user
        super().save_model(request, obj, form, change)


