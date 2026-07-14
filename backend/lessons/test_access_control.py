from io import StringIO

from django.contrib import admin
from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.core.management import call_command
from django.test import TestCase

from .admin_roles import sync_admin_groups
from .models import AdminAuditLog, CreditLedger, Subscription, UserSecurityProfile


class AccessRecordTests(TestCase):
    def test_new_user_receives_security_and_subscription_records(self):
        user = get_user_model().objects.create_user(username="new-person")

        self.assertTrue(UserSecurityProfile.objects.filter(user=user).exists())
        self.assertTrue(Subscription.objects.filter(user=user, status=Subscription.Status.FREE).exists())

    def test_standard_roles_include_controlled_custom_permissions(self):
        groups = sync_admin_groups()

        self.assertTrue(groups["Support"].permissions.filter(codename="manage_user_status").exists())
        self.assertTrue(groups["Billing"].permissions.filter(codename="adjust_credits").exists())
        self.assertTrue(groups["Billing"].permissions.filter(codename="manage_subscription_status").exists())
        self.assertFalse(groups["Support"].permissions.filter(codename="change_providercredential").exists())

    def test_staff_login_is_written_to_security_audit(self):
        staff = get_user_model().objects.create_user(username="audited-staff", is_staff=True)

        self.client.force_login(staff)

        self.assertTrue(AdminAuditLog.objects.filter(actor=staff, action="staff_login").exists())


class UserLifecycleTests(TestCase):
    def setUp(self):
        groups = sync_admin_groups()
        self.support = get_user_model().objects.create_user(
            username="support-daily",
            password="Strong-support-password-2026!",
            is_staff=True,
        )
        self.support.groups.add(groups["Support"])
        self.customer = get_user_model().objects.create_user(username="customer")
        self.client.force_login(self.support)

    def post_action(self, action, reason="Verified support request"):
        return self.client.post(
            "/admin/auth/user/",
            {
                "action": action,
                "reason": reason,
                "select_across": "0",
                "index": "0",
                "_selected_action": [str(self.customer.pk)],
            },
        )

    def test_support_can_block_user_and_audit_reason(self):
        response = self.post_action("block_users")

        self.assertEqual(response.status_code, 302)
        self.customer.refresh_from_db()
        self.assertFalse(self.customer.is_active)
        event = AdminAuditLog.objects.get(action="user_blocked")
        self.assertEqual(event.actor, self.support)
        self.assertEqual(event.reason, "Verified support request")

    def test_critical_action_without_reason_does_nothing(self):
        response = self.post_action("block_users", reason="")

        self.assertEqual(response.status_code, 302)
        self.customer.refresh_from_db()
        self.assertTrue(self.customer.is_active)
        self.assertFalse(AdminAuditLog.objects.filter(action="user_blocked").exists())

    def test_soft_delete_preserves_user_row(self):
        user_id = self.customer.pk

        self.post_action("soft_delete_users", reason="Confirmed deletion request")

        user = get_user_model().objects.get(pk=user_id)
        self.assertFalse(user.is_active)
        self.assertIsNotNone(user.security_profile.deleted_at)
        self.assertEqual(user.security_profile.deletion_reason, "Confirmed deletion request")

    def test_admin_disables_physical_user_deletion(self):
        user_admin = admin.site._registry[get_user_model()]
        request = type("Request", (), {"user": self.support})()

        self.assertFalse(user_admin.has_delete_permission(request, self.customer))


class ImmutableHistoryTests(TestCase):
    def setUp(self):
        self.owner = get_user_model().objects.create_superuser(
            username="owner",
            email="owner@example.com",
            password="Strong-owner-password-2026!",
        )
        self.customer = get_user_model().objects.create_user(username="credit-customer")

    def test_credit_ledger_cannot_be_rewritten_or_deleted(self):
        entry = CreditLedger.objects.create(
            user=self.customer,
            amount=100,
            reason=CreditLedger.Reason.ADMIN_ADJUSTMENT,
            created_by=self.owner,
        )

        entry.amount = 999999
        with self.assertRaises(ValidationError):
            entry.save()
        with self.assertRaises(ValidationError):
            entry.delete()
        with self.assertRaises(ValidationError):
            CreditLedger.objects.filter(pk=entry.pk).update(amount=999999)
        with self.assertRaises(ValidationError):
            CreditLedger.objects.filter(pk=entry.pk).delete()

    def test_audit_log_cannot_be_rewritten_or_deleted(self):
        event = AdminAuditLog.objects.create(
            actor=self.owner,
            actor_label=self.owner.username,
            action="test_event",
            target_type="auth.User",
            target_id=str(self.customer.pk),
        )

        event.reason = "rewritten"
        with self.assertRaises(ValidationError):
            event.save()
        with self.assertRaises(ValidationError):
            event.delete()
        with self.assertRaises(ValidationError):
            AdminAuditLog.objects.filter(pk=event.pk).update(reason="rewritten")

    def test_access_review_lists_staff_security_state(self):
        output = StringIO()

        call_command("review_access", stdout=output)

        self.assertIn("owner", output.getvalue())
        self.assertIn("break-glass", output.getvalue())
