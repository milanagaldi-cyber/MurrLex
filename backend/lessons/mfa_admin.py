from django.contrib import admin
from allauth.mfa.models import Authenticator


class SecureAuthenticatorAdmin(admin.ModelAdmin):
    list_display = ("user", "type", "created_at", "last_used_at")
    search_fields = ("user__username", "user__email")
    list_filter = ("type", "created_at")
    readonly_fields = ("user", "type", "created_at", "last_used_at")
    exclude = ("data",)

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


def configure_secure_mfa_admin():
    if admin.site.is_registered(Authenticator):
        admin.site.unregister(Authenticator)
    admin.site.register(Authenticator, SecureAuthenticatorAdmin)
