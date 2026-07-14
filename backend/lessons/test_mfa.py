from io import StringIO

from allauth.account.models import EmailAddress
from allauth.mfa.models import Authenticator
from allauth.mfa.totp.internal.auth import TOTP
from django.contrib.auth import get_user_model
from django.core.management import call_command
from django.test import TestCase

from .models import AdminAuditLog


class StaffMFAEnforcementTests(TestCase):
    password = "Strong-staff-password-2026!"

    def setUp(self):
        self.staff = get_user_model().objects.create_user(
            username="protected-staff",
            email="staff@example.com",
            password=self.password,
            is_staff=True,
        )
        self.staff.admin_mfa_policy.mfa_required = True
        self.staff.admin_mfa_policy.save(update_fields=["mfa_required", "updated_at"])
        EmailAddress.objects.create(user=self.staff, email=self.staff.email, primary=True, verified=True)

    def test_admin_login_uses_allauth_flow(self):
        response = self.client.get("/admin/login/")

        self.assertEqual(response.status_code, 302)
        self.assertEqual(response["Location"], "/accounts/login/?next=/admin/")

    def test_required_staff_without_totp_is_redirected_to_enrollment(self):
        self.client.force_login(self.staff)

        response = self.client.get("/admin/")

        self.assertEqual(response.status_code, 302)
        self.assertTrue(response["Location"].startswith("/accounts/2fa/totp/activate/"))

    def test_verified_required_staff_can_open_enrollment_after_secure_login(self):
        login_response = self.client.post(
            "/accounts/login/?next=/admin/",
            data={"login": self.staff.username, "password": self.password},
        )
        self.assertEqual(login_response.status_code, 302)

        admin_response = self.client.get("/admin/")
        self.assertTrue(admin_response["Location"].startswith("/accounts/2fa/totp/activate/"))

        response = self.client.get(admin_response["Location"])
        self.assertEqual(response.status_code, 200)

    def test_required_staff_with_totp_reaches_admin(self):
        TOTP.activate(self.staff, "JBSWY3DPEHPK3PXP")
        self.client.force_login(self.staff)

        response = self.client.get("/admin/")

        self.assertEqual(response.status_code, 200)

    def test_secure_password_login_prompts_for_second_factor(self):
        TOTP.activate(self.staff, "JBSWY3DPEHPK3PXP")

        response = self.client.post(
            "/accounts/login/?next=/admin/",
            data={"login": self.staff.username, "password": self.password},
        )

        self.assertEqual(response.status_code, 302)
        self.assertTrue(response["Location"].startswith("/accounts/2fa/authenticate/"))

    def test_public_login_rejects_required_staff_credentials(self):
        response = self.client.post(
            "/login/",
            data={"username": self.staff.username, "password": self.password},
        )

        self.assertEqual(response.status_code, 200)
        self.assertNotIn("_auth_user_id", self.client.session)
        self.assertContains(response, "secure Admin login")

    def test_staff_without_policy_keeps_existing_login_behavior(self):
        unprotected = get_user_model().objects.create_user(
            username="unprotected-staff",
            password=self.password,
            is_staff=True,
        )

        response = self.client.post(
            "/login/",
            data={"username": unprotected.username, "password": self.password},
        )

        self.assertEqual(response.status_code, 302)
        self.assertIn("_auth_user_id", self.client.session)

    def test_reset_command_removes_mfa_and_keeps_policy_required(self):
        owner = get_user_model().objects.create_superuser(
            username="recovery-owner",
            email="owner@example.com",
            password=self.password,
        )
        Authenticator.objects.create(
            user=self.staff,
            type=Authenticator.Type.TOTP,
            data={"secret": "test-secret"},
        )
        output = StringIO()

        call_command(
            "reset_staff_mfa",
            self.staff.username,
            actor=owner.username,
            reason="Lost phone",
            stdout=output,
        )

        self.assertFalse(Authenticator.objects.filter(user=self.staff).exists())
        self.staff.admin_mfa_policy.refresh_from_db()
        self.assertTrue(self.staff.admin_mfa_policy.mfa_required)
        self.assertTrue(
            AdminAuditLog.objects.filter(
                actor=owner,
                action="staff_mfa_reset",
                target_id=str(self.staff.pk),
            ).exists()
        )
        self.assertIn("Lost phone", output.getvalue())
