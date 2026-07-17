import json
from urllib.parse import urlparse

from allauth.account.models import EmailAddress
from django.contrib.auth import get_user_model
from django.core import mail
from django.test import TestCase, override_settings

from .account_adapter import ClosedAllauthSignupAdapter
from .social_auth import StagingSocialAccountAdapter


@override_settings(
    PUBLIC_SIGNUP_ENABLED=True,
    REGISTRATION_ALLOWLIST_ENABLED=False,
    REGISTRATION_EMAIL_VERIFICATION_REQUIRED=True,
    PUBLIC_BASE_URL="https://testserver",
    EMAIL_BACKEND="django.core.mail.backends.locmem.EmailBackend",
)
class RegistrationEmailVerificationTests(TestCase):
    data = {
        "username": "pending-user",
        "email": "pending@example.com",
        "password1": "StrongPass-2026!",
        "password2": "StrongPass-2026!",
    }

    def test_web_registration_requires_email_link_before_login(self):
        response = self.client.post("/register/", data=self.data)

        self.assertRedirects(response, "/register/verification-sent/")
        user = get_user_model().objects.get(username="pending-user")
        self.assertFalse(user.is_active)
        self.assertNotIn("_auth_user_id", self.client.session)
        self.assertEqual(len(mail.outbox), 1)

        link = next(line for line in mail.outbox[0].body.splitlines() if line.startswith("https://"))
        verify_response = self.client.get(urlparse(link).path)

        self.assertContains(verify_response, "Email confirmed")
        user.refresh_from_db()
        self.assertTrue(user.is_active)
        self.assertTrue(EmailAddress.objects.get(user=user, email=user.email).verified)

    def test_invalid_verification_link_does_not_activate_user(self):
        self.client.post("/register/", data=self.data)
        user = get_user_model().objects.get(username="pending-user")

        response = self.client.get(f"/register/verify/{user.pk}/invalid-token/")

        self.assertContains(response, "Link is not valid")
        user.refresh_from_db()
        self.assertFalse(user.is_active)

    def test_mobile_registration_returns_no_tokens_until_verified(self):
        response = self.client.post(
            "/api/auth/register",
            data=json.dumps(
                {"username": "mobile-pending", "email": "mobile@example.com", "password": "StrongPass-2026!"}
            ),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 202)
        self.assertEqual(response.json()["status"], "verification_required")
        self.assertNotIn("accessToken", response.json())
        self.assertFalse(get_user_model().objects.get(username="mobile-pending").is_active)
        self.assertEqual(len(mail.outbox), 1)


class SignupAdapterTests(TestCase):
    def test_direct_allauth_password_signup_is_closed(self):
        self.assertFalse(ClosedAllauthSignupAdapter().is_open_for_signup(None))

    @override_settings(GOOGLE_OAUTH_ENABLED=True)
    def test_google_signup_remains_available_to_social_allowlist_flow(self):
        sociallogin = type("SocialLogin", (), {"account": type("Account", (), {"provider": "google"})()})()
        self.assertTrue(StagingSocialAccountAdapter().is_open_for_signup(None, sociallogin))
