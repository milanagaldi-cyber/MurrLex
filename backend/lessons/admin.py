from django import forms
from django.contrib import admin
from django.contrib.admin.helpers import ActionForm
from django.contrib.auth import get_user_model
from django.contrib.auth.admin import GroupAdmin, UserAdmin
from django.contrib.auth.models import Group
from django.shortcuts import redirect
from django.urls import reverse
from django.utils import timezone

from .audit import record_audit_event
from .mfa_policy import user_has_totp
from .models import (
    AdminAuditLog,
    ApiSession,
    AdminMfaPolicy,
    Card,
    CreditLedger,
    GoogleOAuthAllowedUser,
    ImportLog,
    Lesson,
    ProviderCredential,
    Subscription,
    SubscriptionPlan,
    UserApiAccess,
    UserSecurityProfile,
)


admin.site.enable_nav_sidebar = False
admin.site.index_template = "admin/index.html"
admin.site.index_title = "Server dashboard"


class CriticalActionForm(ActionForm):
    reason = forms.CharField(
        required=False,
        max_length=500,
        widget=forms.TextInput(attrs={"placeholder": "Required reason for critical actions"}),
    )


def require_action_reason(model_admin, request) -> str | None:
    reason = request.POST.get("reason", "").strip()
    if not reason:
        model_admin.message_user(request, "A reason is required for this action.", level="error")
        return None
    return reason


User = get_user_model()
admin.site.unregister(User)
admin.site.unregister(Group)


@admin.register(User)
class MurrLexUserAdmin(UserAdmin):
    """Keep routine admins useful without letting them grant themselves power."""

    action_form = CriticalActionForm
    actions = (
        "block_users", "unblock_users", "soft_delete_users", "grant_100_credits",
        "grant_100k_credits", "deduct_100k_credits", "assign_light", "assign_super",
        "assign_ultra", "freeze_credits", "unfreeze_credits",
    )
    list_display = (
        "username",
        "email",
        "is_active",
        "is_staff",
        "is_superuser",
        "mfa_enabled",
        "soft_deleted",
        "credit_balance",
    )
    list_filter = ("is_active", "is_staff", "is_superuser", "groups")

    class UserSecurityProfileInline(admin.StackedInline):
        model = UserSecurityProfile
        can_delete = False
        extra = 0
        fields = ("is_break_glass", "internal_note", "deleted_at", "deletion_reason", "access_reviewed_at")
        readonly_fields = ("deleted_at", "deletion_reason", "access_reviewed_at")

    class SubscriptionInline(admin.StackedInline):
        model = Subscription
        can_delete = False
        extra = 0
        fields = ("plan", "plan_code", "status", "valid_until", "credits_frozen", "freeze_reason", "next_refill_at", "last_refilled_at", "updated_at")
        readonly_fields = ("updated_at",)

    inlines = (UserSecurityProfileInline, SubscriptionInline)

    def get_actions(self, request):
        actions = super().get_actions(request)
        if not request.user.is_superuser:
            for name in (
                "grant_100k_credits", "deduct_100k_credits", "assign_light", "assign_super",
                "assign_ultra", "freeze_credits", "unfreeze_credits",
            ):
                actions.pop(name, None)
        return actions

    def get_inline_instances(self, request, obj=None):
        if obj is None or not request.user.is_superuser:
            return []
        return super().get_inline_instances(request, obj)

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
        return False

    def has_manage_user_status_permission(self, request):
        return request.user.has_perm("lessons.manage_user_status")

    def has_adjust_credits_permission(self, request):
        return request.user.has_perm("lessons.adjust_credits")

    @admin.display(boolean=True, description="2FA")
    def mfa_enabled(self, user):
        return user_has_totp(user)

    @admin.display(boolean=True, description="Soft deleted")
    def soft_deleted(self, user):
        profile, _ = UserSecurityProfile.objects.get_or_create(user=user)
        return profile.is_soft_deleted

    @admin.display(description="Credits")
    def credit_balance(self, user):
        return CreditLedger.balance_for(user)

    def _eligible_public_users(self, queryset):
        return queryset.filter(is_staff=False, is_superuser=False)

    @admin.action(description="Block selected users", permissions=["manage_user_status"])
    def block_users(self, request, queryset):
        reason = require_action_reason(self, request)
        if reason is None:
            return
        for user in self._eligible_public_users(queryset).filter(is_active=True):
            user.is_active = False
            user.save(update_fields=["is_active"])
            sessions_revoked = ApiSession.objects.filter(user=user, revoked_at__isnull=True).update(
                revoked_at=timezone.now()
            )
            record_audit_event(
                action="user_blocked",
                target=user,
                actor=request.user,
                request=request,
                old_value={"is_active": True},
                new_value={"is_active": False, "api_sessions_revoked": sessions_revoked},
                reason=reason,
            )

    @admin.action(description="Unblock selected users", permissions=["manage_user_status"])
    def unblock_users(self, request, queryset):
        reason = require_action_reason(self, request)
        if reason is None:
            return
        for user in self._eligible_public_users(queryset).filter(is_active=False):
            profile, _ = UserSecurityProfile.objects.get_or_create(user=user)
            if profile.is_soft_deleted:
                continue
            user.is_active = True
            user.save(update_fields=["is_active"])
            record_audit_event(
                action="user_unblocked",
                target=user,
                actor=request.user,
                request=request,
                old_value={"is_active": False},
                new_value={"is_active": True},
                reason=reason,
            )

    @admin.action(description="Mark selected users for deletion", permissions=["manage_user_status"])
    def soft_delete_users(self, request, queryset):
        reason = require_action_reason(self, request)
        if reason is None:
            return
        for user in self._eligible_public_users(queryset):
            profile, _ = UserSecurityProfile.objects.get_or_create(user=user)
            old_value = {"is_active": user.is_active, "deleted_at": None}
            profile.mark_deleted(reason)
            user.is_active = False
            user.save(update_fields=["is_active"])
            sessions_revoked = ApiSession.objects.filter(user=user, revoked_at__isnull=True).update(
                revoked_at=timezone.now()
            )
            profile.save(update_fields=["deleted_at", "deletion_reason", "updated_at"])
            record_audit_event(
                action="user_soft_deleted",
                target=user,
                actor=request.user,
                request=request,
                old_value=old_value,
                new_value={
                    "is_active": False,
                    "deleted_at": profile.deleted_at.isoformat(),
                    "api_sessions_revoked": sessions_revoked,
                },
                reason=reason,
            )

    @admin.action(description="Grant 100 credits", permissions=["adjust_credits"])
    def grant_100_credits(self, request, queryset):
        reason = require_action_reason(self, request)
        if reason is None:
            return
        for user in self._eligible_public_users(queryset):
            entry = CreditLedger.objects.create(
                user=user,
                amount=100,
                reason=CreditLedger.Reason.SUPPORT_BONUS,
                note=reason,
                created_by=request.user,
            )
            record_audit_event(
                action="credits_granted",
                target=user,
                actor=request.user,
                request=request,
                new_value={"amount": 100, "ledger_id": entry.pk},
                reason=reason,
            )

    def _credit_adjustment(self, request, queryset, amount):
        reason = require_action_reason(self, request)
        if reason is None or not request.user.is_superuser:
            return
        ledger_reason = CreditLedger.Reason.ADMIN_ADJUSTMENT if amount > 0 else CreditLedger.Reason.ADMIN_DEDUCTION
        for user in queryset:
            CreditLedger.objects.create(user=user, amount=amount, reason=ledger_reason, note=reason, created_by=request.user)

    @admin.action(description="Grant 100,000 credits (superuser)")
    def grant_100k_credits(self, request, queryset):
        self._credit_adjustment(request, queryset, 100_000)

    @admin.action(description="Deduct 100,000 credits (superuser)")
    def deduct_100k_credits(self, request, queryset):
        self._credit_adjustment(request, queryset, -100_000)

    def _assign_plan(self, request, queryset, code):
        reason = require_action_reason(self, request)
        if reason is None or not request.user.is_superuser:
            return
        plan = SubscriptionPlan.objects.get(code=code)
        now = timezone.now()
        from .premium import next_refill_time
        for user in queryset:
            subscription, _ = Subscription.objects.get_or_create(user=user)
            subscription.plan = plan
            subscription.plan_code = plan.code
            subscription.status = Subscription.Status.ACTIVE
            subscription.next_refill_at = next_refill_time(now, plan.refill_period)
            subscription.save()

    @admin.action(description="Assign Light plan (superuser)")
    def assign_light(self, request, queryset): self._assign_plan(request, queryset, "light")

    @admin.action(description="Assign Super plan (superuser)")
    def assign_super(self, request, queryset): self._assign_plan(request, queryset, "super")

    @admin.action(description="Assign Ultra plan (superuser)")
    def assign_ultra(self, request, queryset): self._assign_plan(request, queryset, "ultra")

    def _set_credit_freeze(self, request, queryset, frozen):
        reason = require_action_reason(self, request)
        if reason is None or not request.user.is_superuser:
            return
        for user in queryset:
            subscription, _ = Subscription.objects.get_or_create(user=user)
            subscription.credits_frozen = frozen
            subscription.freeze_reason = reason if frozen else ""
            subscription.save(update_fields=["credits_frozen", "freeze_reason", "updated_at"])

    @admin.action(description="Freeze AI credits (superuser)")
    def freeze_credits(self, request, queryset): self._set_credit_freeze(request, queryset, True)

    @admin.action(description="Unfreeze AI credits (superuser)")
    def unfreeze_credits(self, request, queryset): self._set_credit_freeze(request, queryset, False)

    def save_model(self, request, obj, form, change):
        old_value = {}
        if change and obj.pk:
            previous = self.model.objects.get(pk=obj.pk)
            old_value = {
                "is_staff": previous.is_staff,
                "is_superuser": previous.is_superuser,
                "is_active": previous.is_active,
            }
        super().save_model(request, obj, form, change)
        if request.user.is_superuser:
            record_audit_event(
                action="staff_user_changed" if change else "staff_user_created",
                target=obj,
                actor=request.user,
                request=request,
                old_value=old_value,
                new_value={
                    "is_staff": obj.is_staff,
                    "is_superuser": obj.is_superuser,
                    "is_active": obj.is_active,
                },
                reason="Django Admin user form",
            )

    def save_related(self, request, form, formsets, change):
        super().save_related(request, form, formsets, change)
        if request.user.is_superuser:
            user = form.instance
            record_audit_event(
                action="user_permissions_changed",
                target=user,
                actor=request.user,
                request=request,
                new_value={
                    "groups": list(user.groups.values_list("name", flat=True)),
                    "permissions": list(user.user_permissions.values_list("codename", flat=True)),
                },
                reason="Django Admin user permissions form",
            )


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

    def has_add_permission(self, request):
        return request.user.is_superuser

    def has_change_permission(self, request, obj=None):
        return request.user.is_superuser

    def has_delete_permission(self, request, obj=None):
        return request.user.is_superuser

    def save_related(self, request, form, formsets, change):
        super().save_related(request, form, formsets, change)
        group = form.instance
        record_audit_event(
            action="group_permissions_changed",
            target=group,
            actor=request.user,
            request=request,
            new_value={"permissions": list(group.permissions.values_list("codename", flat=True))},
            reason="Django Admin group permissions form",
        )


@admin.register(GoogleOAuthAllowedUser)
class GoogleOAuthAllowedUserAdmin(admin.ModelAdmin):
    list_display = ("email", "google_sub", "is_active", "note", "updated_at")
    list_editable = ("is_active",)
    list_filter = ("is_active",)
    search_fields = ("email", "google_sub", "note")
    readonly_fields = ("created_at", "updated_at")

    def save_model(self, request, obj, form, change):
        old_value = {}
        if change and obj.pk:
            previous = type(obj).objects.get(pk=obj.pk)
            old_value = {
                "email": previous.email,
                "google_sub": previous.google_sub,
                "is_active": previous.is_active,
            }
        super().save_model(request, obj, form, change)
        record_audit_event(
            action="login_allowlist_changed" if change else "login_allowlist_added",
            target=obj,
            actor=request.user,
            request=request,
            old_value=old_value,
            new_value={"email": obj.email, "google_sub": obj.google_sub, "is_active": obj.is_active},
            reason="Django Admin registration allowlist form",
        )

    def has_delete_permission(self, request, obj=None):
        return False

    def changelist_view(self, request, extra_context=None):
        extra_context = {
            **(extra_context or {}),
            "title": "Registration & Google allowlist",
        }
        return super().changelist_view(request, extra_context=extra_context)


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
        targets = list(queryset.select_related("user"))
        updated = queryset.update(ai_api_enabled=True, updated_at=timezone.now())
        for access in targets:
            record_audit_event(
                action="ai_api_access_enabled",
                target=access.user,
                actor=request.user,
                request=request,
                old_value={"ai_api_enabled": access.ai_api_enabled},
                new_value={"ai_api_enabled": True},
                reason="Django Admin bulk action",
            )
        self.message_user(request, f"AI access enabled for {updated} user(s).")

    @admin.action(description="Disable AI access for selected users")
    def disable_ai_access(self, request, queryset):
        targets = list(queryset.select_related("user"))
        updated = queryset.update(ai_api_enabled=False, updated_at=timezone.now())
        for access in targets:
            record_audit_event(
                action="ai_api_access_disabled",
                target=access.user,
                actor=request.user,
                request=request,
                old_value={"ai_api_enabled": access.ai_api_enabled},
                new_value={"ai_api_enabled": False},
                reason="Django Admin bulk action",
            )
        self.message_user(request, f"AI access disabled for {updated} user(s).")

    def save_model(self, request, obj, form, change):
        old_enabled = None
        if change and obj.pk:
            old_enabled = type(obj).objects.get(pk=obj.pk).ai_api_enabled
        super().save_model(request, obj, form, change)
        if old_enabled is not None and old_enabled != obj.ai_api_enabled:
            record_audit_event(
                action="ai_api_access_changed",
                target=obj.user,
                actor=request.user,
                request=request,
                old_value={"ai_api_enabled": old_enabled},
                new_value={"ai_api_enabled": obj.ai_api_enabled},
                reason="Django Admin access form",
            )

    @admin.display(ordering="user__username", description="Username")
    def username(self, access):
        return access.user.get_username()

    @admin.display(ordering="user__email", description="Email")
    def email(self, access):
        return access.user.email

    @admin.display(boolean=True, ordering="user__is_active", description="Active account")
    def account_active(self, access):
        return access.user.is_active


@admin.register(AdminMfaPolicy)
class AdminMfaPolicyAdmin(admin.ModelAdmin):
    action_form = CriticalActionForm
    list_display = ("username", "email", "staff", "mfa_required", "mfa_enabled", "updated_at")
    list_editable = ("mfa_required",)
    list_filter = ("mfa_required", "user__is_staff", "user__is_active")
    search_fields = ("user__username", "user__email")
    readonly_fields = ("user", "updated_at")
    list_select_related = ("user",)
    actions = ("reset_selected_mfa",)

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return request.user.is_superuser

    def has_delete_permission(self, request, obj=None):
        return False

    def get_actions(self, request):
        actions = super().get_actions(request)
        if not request.user.is_superuser:
            actions.pop("reset_selected_mfa", None)
        return actions

    @admin.action(description="Reset 2FA for selected staff users")
    def reset_selected_mfa(self, request, queryset):
        from allauth.mfa.models import Authenticator

        reason = require_action_reason(self, request)
        if reason is None:
            return
        user_ids = queryset.filter(user__is_staff=True).values_list("user_id", flat=True)
        targets = list(User.objects.filter(pk__in=user_ids))
        removed, _ = Authenticator.objects.filter(
            user_id__in=user_ids,
            type__in=[Authenticator.Type.TOTP, Authenticator.Type.RECOVERY_CODES],
        ).delete()
        queryset.filter(user__is_staff=True).update(mfa_required=True, updated_at=timezone.now())
        for target in targets:
            record_audit_event(
                action="staff_mfa_reset",
                target=target,
                actor=request.user,
                request=request,
                old_value={"authenticators_removed": removed},
                new_value={"mfa_required": True, "mfa_enabled": False},
                reason=reason,
            )
        self.message_user(request, f"Removed {removed} MFA record(s). Enrollment remains required.")

    @admin.display(ordering="user__username", description="Username")
    def username(self, profile):
        return profile.user.get_username()

    @admin.display(ordering="user__email", description="Email")
    def email(self, profile):
        return profile.user.email

    @admin.display(boolean=True, ordering="user__is_staff", description="Staff")
    def staff(self, profile):
        return profile.user.is_staff

    @admin.display(boolean=True, description="2FA enabled")
    def mfa_enabled(self, profile):
        return user_has_totp(profile.user)

    def save_model(self, request, obj, form, change):
        old_required = None
        if change and obj.pk:
            old_required = type(obj).objects.get(pk=obj.pk).mfa_required
        super().save_model(request, obj, form, change)
        if old_required is not None and old_required != obj.mfa_required:
            record_audit_event(
                action="admin_2fa_policy_changed",
                target=obj.user,
                actor=request.user,
                request=request,
                old_value={"mfa_required": old_required},
                new_value={"mfa_required": obj.mfa_required},
                reason="Django Admin 2FA policy form",
            )


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
        record_audit_event(
            action="provider_credential_changed",
            target=obj,
            actor=request.user,
            request=request,
            new_value={"provider": obj.provider, "configured": obj.is_configured},
            reason="Provider credential form",
        )

    def has_add_permission(self, request):
        return request.user.is_superuser

    def has_change_permission(self, request, obj=None):
        return request.user.is_superuser

    def has_delete_permission(self, request, obj=None):
        return request.user.is_superuser


@admin.register(UserSecurityProfile)
class UserSecurityProfileAdmin(admin.ModelAdmin):
    list_display = ("user", "mfa_status", "is_break_glass", "deleted_at", "access_reviewed_at")
    search_fields = ("user__username", "user__email", "internal_note")
    list_filter = ("is_break_glass", "deleted_at")
    readonly_fields = ("user", "deleted_at", "deletion_reason", "access_reviewed_at", "updated_at")

    @admin.display(boolean=True, description="2FA enabled")
    def mfa_status(self, profile):
        return user_has_totp(profile.user)

    def get_readonly_fields(self, request, obj=None):
        fields = list(super().get_readonly_fields(request, obj))
        if not request.user.is_superuser:
            fields.extend(["is_break_glass", "internal_note"])
        return tuple(dict.fromkeys(fields))

    def has_delete_permission(self, request, obj=None):
        return False

    def save_model(self, request, obj, form, change):
        old_value = {}
        if change:
            previous = UserSecurityProfile.objects.get(pk=obj.pk)
            old_value = {
                "internal_note": previous.internal_note,
                "is_break_glass": previous.is_break_glass,
            }
        super().save_model(request, obj, form, change)
        record_audit_event(
            action="user_security_profile_changed",
            target=obj.user,
            actor=request.user,
            request=request,
            old_value=old_value,
            new_value={"internal_note": obj.internal_note, "is_break_glass": obj.is_break_glass},
            reason="Django Admin security profile form",
        )


@admin.register(Subscription)
class SubscriptionAdmin(admin.ModelAdmin):
    action_form = CriticalActionForm
    actions = ("activate_subscriptions", "cancel_subscriptions")
    list_display = ("user", "plan", "status", "credits_frozen", "next_refill_at", "valid_until", "updated_at")
    search_fields = ("user__username", "user__email", "plan_code")
    list_filter = ("status", "plan", "credits_frozen")
    readonly_fields = ("user", "updated_at", "last_refilled_at")

    def has_manage_subscription_status_permission(self, request):
        return request.user.is_superuser

    @admin.action(description="Activate selected subscriptions", permissions=["manage_subscription_status"])
    def activate_subscriptions(self, request, queryset):
        self._set_status(request, queryset, Subscription.Status.ACTIVE)

    @admin.action(description="Cancel selected subscriptions", permissions=["manage_subscription_status"])
    def cancel_subscriptions(self, request, queryset):
        self._set_status(request, queryset, Subscription.Status.CANCELLED)

    def _set_status(self, request, queryset, status):
        reason = require_action_reason(self, request)
        if reason is None:
            return
        for subscription in queryset.select_related("user"):
            old_status = subscription.status
            subscription.status = status
            subscription.save(update_fields=["status", "updated_at"])
            record_audit_event(
                action="subscription_status_changed",
                target=subscription,
                actor=request.user,
                request=request,
                old_value={"status": old_status},
                new_value={"status": status},
                reason=reason,
            )

    def has_add_permission(self, request):
        return False

    def get_readonly_fields(self, request, obj=None):
        if request.user.is_superuser:
            return self.readonly_fields
        return tuple(field.name for field in self.model._meta.fields)

    def has_delete_permission(self, request, obj=None):
        return False


@admin.register(SubscriptionPlan)
class SubscriptionPlanAdmin(admin.ModelAdmin):
    list_display = ("name", "code", "refill_period", "refill_credits", "is_active", "updated_at")
    list_editable = ("refill_period", "refill_credits", "is_active")
    search_fields = ("name", "code")

    def has_module_permission(self, request):
        return request.user.is_superuser

    def has_view_permission(self, request, obj=None):
        return request.user.is_superuser

    def has_add_permission(self, request):
        return request.user.is_superuser

    def has_change_permission(self, request, obj=None):
        return request.user.is_superuser

    def has_delete_permission(self, request, obj=None):
        return request.user.is_superuser


@admin.register(CreditLedger)
class CreditLedgerAdmin(admin.ModelAdmin):
    action_form = CriticalActionForm
    actions = ("reverse_adjustments",)
    list_display = ("created_at", "user", "amount", "reason", "created_by", "reversal_of")
    search_fields = ("user__username", "user__email", "note")
    list_filter = ("reason", "created_at")
    readonly_fields = ("user", "amount", "reason", "note", "created_by", "reversal_of", "created_at")

    def has_adjust_credits_permission(self, request):
        return request.user.is_superuser

    @admin.action(description="Reverse selected credit adjustments", permissions=["adjust_credits"])
    def reverse_adjustments(self, request, queryset):
        reason = require_action_reason(self, request)
        if reason is None:
            return
        for entry in queryset.filter(reversal_of__isnull=True):
            if hasattr(entry, "reversal"):
                continue
            reversal = CreditLedger.objects.create(
                user=entry.user,
                amount=-entry.amount,
                reason=CreditLedger.Reason.REVERSAL,
                note=reason,
                created_by=request.user,
                reversal_of=entry,
            )
            record_audit_event(
                action="credits_reversed",
                target=entry.user,
                actor=request.user,
                request=request,
                old_value={"ledger_id": entry.pk, "amount": entry.amount},
                new_value={"ledger_id": reversal.pk, "amount": reversal.amount},
                reason=reason,
            )

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


@admin.register(AdminAuditLog)
class AdminAuditLogAdmin(admin.ModelAdmin):
    list_display = ("created_at", "actor_label", "action", "target_type", "target_id", "ip_address")
    search_fields = ("actor_label", "action", "target_type", "target_id", "reason")
    list_filter = ("action", "target_type", "created_at")
    readonly_fields = (
        "actor",
        "actor_label",
        "action",
        "target_type",
        "target_id",
        "old_value",
        "new_value",
        "reason",
        "ip_address",
        "created_at",
    )

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


