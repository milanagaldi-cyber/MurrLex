"""
URL configuration for make_mistake_backend project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/5.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""

from django.contrib import admin
from django.contrib.auth import views as auth_views
from django.urls import include, path
from django.views.generic import RedirectView
from lessons.forms import UsernameOrEmailAuthenticationForm
from lessons import views as lesson_views
from lessons.mfa_admin import configure_secure_mfa_admin

configure_secure_mfa_admin()

admin.site.site_header = "MurrLex Admin"
admin.site.site_title = "MurrLex Admin"
admin.site.index_title = "Server control panel"

urlpatterns = [
    path("", lesson_views.home, name="home"),
    path("studio/", include("lexamora_studio.urls")),
    path("api/v1/studio/", include("lexamora_studio.api_urls")),
    path("accounts/", include("allauth.urls")),
    path(
        "admin/login/",
        RedirectView.as_view(url="/accounts/login/?next=/admin/", permanent=False),
        name="staff_admin_login",
    ),
    path("admin/help/", admin.site.admin_view(lesson_views.admin_help), name="admin_help"),
    path("admin/premium-control/", admin.site.admin_view(lesson_views.premium_control), name="premium_control"),
    path("admin/", admin.site.urls),
    path("register/", lesson_views.register, name="register"),
    path(
        "register/verification-sent/",
        lesson_views.registration_verification_sent,
        name="registration_verification_sent",
    ),
    path(
        "register/verify/<uidb64>/<token>/",
        lesson_views.verify_registration_email,
        name="verify_registration_email",
    ),
    path("account/", lesson_views.account, name="account"),
    path("account/settings/", lesson_views.account_settings, name="account_settings"),
    path("account/provider-keys/", lesson_views.provider_credentials, name="provider_credentials"),
    path("apps/", lesson_views.apps_dashboard, name="apps_dashboard"),
    path("apps/lessons/", lesson_views.web_lessons, name="web_lessons"),
    path("apps/lessons/new/", lesson_views.web_lesson_edit, name="web_lesson_new"),
    path("apps/lessons/<int:lesson_id>/", lesson_views.web_lesson_detail, name="web_lesson_detail"),
    path("apps/lessons/<int:lesson_id>/study/", lesson_views.web_study, name="web_study"),
    path("apps/lessons/<int:lesson_id>/edit/", lesson_views.web_lesson_edit, name="web_lesson_edit"),
    path("apps/lessons/<int:lesson_id>/cards/new/", lesson_views.web_card_edit, name="web_card_new"),
    path("apps/lessons/<int:lesson_id>/cards/<int:card_id>/edit/", lesson_views.web_card_edit, name="web_card_edit"),
    path("apps/delete/<str:object_type>/<int:object_id>/", lesson_views.web_delete, name="web_delete"),
    path("apps/translator/", lesson_views.web_translate, name="web_translate"),
    path("apps/speech/", lesson_views.web_speech, name="web_speech"),
    path("apps/transcribe/", lesson_views.web_transcribe, name="web_transcribe"),
    path("apps/image-text/", lesson_views.web_image_text, name="web_image_text"),
    path("apps/sync/", lesson_views.web_sync, name="web_sync"),
    path("premium/", lesson_views.premium, name="premium"),
    path(
        "login/",
        auth_views.LoginView.as_view(
            authentication_form=UsernameOrEmailAuthenticationForm,
            template_name="registration/login.html",
        ),
        name="login",
    ),
    path(
        "password-change/",
        auth_views.PasswordChangeView.as_view(template_name="registration/password_change_form.html"),
        name="password_change",
    ),
    path(
        "password-change/done/",
        auth_views.PasswordChangeDoneView.as_view(template_name="registration/password_change_done.html"),
        name="password_change_done",
    ),
    path("logout/", auth_views.LogoutView.as_view(), name="logout"),
    path("lab/", include("lessons.urls")),
    path("api/health", lesson_views.api_health, name="api_health"),
    path("api/ready", lesson_views.api_ready, name="api_ready"),
    path("api/auth/register", lesson_views.api_register, name="api_register"),
    path("api/auth/login", lesson_views.api_login, name="api_login"),
    path("api/auth/google/", lesson_views.api_google_login, name="api_google_login"),
    path("api/auth/refresh", lesson_views.api_refresh, name="api_refresh"),
    path("api/auth/logout", lesson_views.api_logout, name="api_logout"),
    path("api/ai/text", lesson_views.api_text, name="api_text"),
    path("api/ai/transcribe", lesson_views.api_transcribe, name="api_transcribe"),
    path("api/ai/speech", lesson_views.api_speech, name="api_speech"),
    path("api/ai/image-text", lesson_views.api_image_text, name="api_image_text"),
    path("api/sync", lesson_views.api_sync, name="api_sync"),
    path("api/internal/import-lesson", lesson_views.internal_import_lesson, name="internal_import_lesson"),
    path("api/translate", lesson_views.translate_text, name="translate_text"),
]
