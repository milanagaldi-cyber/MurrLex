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
from lessons import views as lesson_views

urlpatterns = [
    path("", lesson_views.home, name="home"),
    path("admin/", admin.site.urls),
    path("register/", lesson_views.register, name="register"),
    path("account/", lesson_views.account, name="account"),
    path("account/settings/", lesson_views.account_settings, name="account_settings"),
    path("premium/", lesson_views.premium, name="premium"),
    path("login/", auth_views.LoginView.as_view(template_name="registration/login.html"), name="login"),
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
    path("api/internal/import-lesson", lesson_views.internal_import_lesson, name="internal_import_lesson"),
    path("api/translate", lesson_views.translate_text, name="translate_text"),
]
