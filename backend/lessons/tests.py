import json
from unittest.mock import patch

from cryptography.fernet import Fernet
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Group, Permission
from django.urls import reverse
from django.test import TestCase, override_settings

from allauth.socialaccount.models import SocialAccount

from .admin_roles import DAILY_ADMIN_GROUP, ROLE_NAMES, sync_admin_groups, sync_daily_admin_group
from .models import Card, GoogleOAuthAllowedUser, ImportLog, Lesson, ProviderCredential, UserApiAccess
from .provider_credentials import get_provider_api_key
from .services import import_lesson_payload
from .social_auth import GoogleIdentityError


@override_settings(
    JWT_SIGNING_KEY="mobile-api-test-key-at-least-32-bytes",
    JWT_ACCESS_MINUTES=15,
    JWT_REFRESH_DAYS=30,
    CREDENTIAL_ENCRYPTION_KEY=Fernet.generate_key().decode("ascii"),
    PUBLIC_SIGNUP_ENABLED=True,
    REGISTRATION_ALLOWLIST_ENABLED=False,
)
class MobileApiTests(TestCase):
    def create_user_and_login(self):
        response = self.client.post(
            "/api/auth/register",
            data=json.dumps(
                {
                    "username": "mobilelearner",
                    "email": "mobile@example.com",
                    "password": "StrongPass-2026!",
                    "deviceName": "Android test",
                }
            ),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201)
        return response.json()

    def test_mobile_register_creates_rotatable_session(self):
        payload = self.create_user_and_login()
        self.assertTrue(payload["accessToken"])
        self.assertTrue(payload["refreshToken"])
        self.assertEqual(payload["user"]["username"], "mobilelearner")

        refresh_response = self.client.post(
            "/api/auth/refresh",
            data=json.dumps({"refreshToken": payload["refreshToken"]}),
            content_type="application/json",
        )
        self.assertEqual(refresh_response.status_code, 200)
        self.assertNotEqual(refresh_response.json()["refreshToken"], payload["refreshToken"])

    def test_mobile_ai_text_requires_session(self):
        response = self.client.post(
            "/api/ai/text",
            data=json.dumps({"model": "gpt-5.4-mini", "prompt": "hello"}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 401)

    def enable_ai_for(self, username="mobilelearner"):
        user = get_user_model().objects.get(username=username)
        access, _ = UserApiAccess.objects.get_or_create(user=user)
        access.ai_api_enabled = True
        access.save()
        credential = ProviderCredential(provider=ProviderCredential.Provider.OPENAI)
        credential.set_api_key("sk-test")
        credential.save()
        return user

    @patch("lessons.views.run_text", return_value=("czesc", "gpt-5.4-mini"))
    def test_mobile_ai_text_uses_authenticated_session(self, mocked_run_text):
        session = self.create_user_and_login()
        self.enable_ai_for()
        response = self.client.post(
            "/api/ai/text",
            data=json.dumps({"model": "gpt-5.4-mini", "prompt": "Translate hello"}),
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {session['accessToken']}",
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["output"], "czesc")
        mocked_run_text.assert_called_once()

    def test_mobile_sync_is_scoped_to_authenticated_user(self):
        session = self.create_user_and_login()
        self.enable_ai_for()
        lesson = sample_lesson_payload(id="mobile-sync-1", title="Phone lesson")
        response = self.client.post(
            "/api/sync",
            data=json.dumps({"scope": "all", "lessons": [lesson]}),
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {session['accessToken']}",
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["lessons"][0]["title"], "Phone lesson")
        self.assertEqual(Lesson.objects.get(external_id="mobile-sync-1").owner.username, "mobilelearner")


@override_settings(CREDENTIAL_ENCRYPTION_KEY=Fernet.generate_key().decode("ascii"))
class WebAppsTests(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(username="weblearner", password="Strong-pass-2026!")
        self.client.force_login(self.user)

    def test_apps_are_hidden_without_ai_grant(self):
        self.assertEqual(self.client.get("/apps/").status_code, 403)

    def test_granted_user_can_create_personal_lesson(self):
        access, _ = UserApiAccess.objects.get_or_create(user=self.user)
        access.ai_api_enabled = True
        access.save()
        credential = ProviderCredential(provider=ProviderCredential.Provider.OPENAI)
        credential.set_api_key("sk-test")
        credential.save()
        response = self.client.post(
            "/apps/lessons/new/",
            data={"title": "Web lesson", "source_language": "Russian", "target_language": "Polish"},
        )
        self.assertEqual(response.status_code, 302)
        lesson = Lesson.objects.get(title="Web lesson")
        self.assertEqual(lesson.owner, self.user)
        Card.objects.create(lesson=lesson, external_card_id="1", native_value="cat", correct_value="kot")
        study = self.client.get(f"/apps/lessons/{lesson.id}/study/")
        self.assertEqual(study.status_code, 200)
        self.assertContains(study, "cat")
        self.assertContains(study, "kot")


@override_settings(CREDENTIAL_ENCRYPTION_KEY=Fernet.generate_key().decode("ascii"))
class ProviderCredentialTests(TestCase):
    def setUp(self):
        self.superuser = get_user_model().objects.create_superuser(
            username="provider-admin",
            email="provider-admin@example.com",
            password="Strong-admin-password-2026!",
        )
        self.public_user = get_user_model().objects.create_user(
            username="public-user",
            password="Strong-public-password-2026!",
        )

    def test_credential_is_encrypted_and_decryptable(self):
        credential = ProviderCredential(provider=ProviderCredential.Provider.OPENAI)
        credential.set_api_key("sk-server-secret")
        credential.save()

        self.assertNotIn("sk-server-secret", credential.encrypted_api_key)
        self.assertEqual(get_provider_api_key("openai"), "sk-server-secret")

    def test_only_superuser_can_manage_provider_keys(self):
        self.client.force_login(self.public_user)
        response = self.client.get("/account/provider-keys/")
        self.assertEqual(response.status_code, 403)

        self.client.force_login(self.superuser)
        response = self.client.post(
            "/account/provider-keys/",
            data={"provider": "openai", "api_key": "sk-server-secret"},
        )
        self.assertRedirects(response, "/account/provider-keys/")
        page = self.client.get("/account/provider-keys/")
        self.assertNotContains(page, "sk-server-secret")
        self.assertContains(page, "Configured")


def sample_lesson_payload(**overrides):
    payload = {
        "id": "lesson-001",
        "title": "Belarusian mistakes 1",
        "cardKind": "MK",
        "sourceLanguage": "Russian",
        "targetLanguage": "Belarusian",
        "lessonInfo": "Practice typical spelling mistakes. Type the correct Belarusian form.",
        "cards": [
            {
                "id": 1,
                "nativeValue": "молоко",
                "correctValue": "малако",
                "mistake": "молоко",
                "hint": "Use Belarusian spelling.",
                "cardKind": "MK",
                "sourceLanguage": "Russian",
                "targetLanguage": "Belarusian",
                "stars": 0,
            },
            {
                "id": 2,
                "nativeValue": "вода",
                "correctValue": "вада",
                "mistake": "вода",
                "hint": "Use Belarusian spelling.",
                "cardKind": "MK",
                "sourceLanguage": "Russian",
                "targetLanguage": "Belarusian",
                "stars": 1,
            },
        ],
    }
    payload.update(overrides)
    return payload


@override_settings(INTERNAL_IMPORT_TOKEN="test-token")
class InternalImportLessonApiTests(TestCase):
    endpoint = "/api/internal/import-lesson"

    def post_payload(self, payload, token="test-token"):
        headers = {}
        if token is not None:
            headers["HTTP_AUTHORIZATION"] = f"Bearer {token}"
        return self.client.post(
            self.endpoint,
            data=json.dumps(payload),
            content_type="application/json",
            **headers,
        )

    def test_successful_lesson_import(self):
        response = self.post_payload(sample_lesson_payload())

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            {"status": "ok", "lessonId": "lesson-001", "cardsImported": 2},
        )
        lesson = Lesson.objects.get(external_id="lesson-001")
        self.assertEqual(lesson.title, "Belarusian mistakes 1")
        self.assertEqual(
            lesson.lesson_info,
            "Practice typical spelling mistakes. Type the correct Belarusian form.",
        )
        self.assertEqual(lesson.cards.count(), 2)
        self.assertTrue(
            ImportLog.objects.filter(
                source="api",
                status=ImportLog.Status.SUCCESS,
                external_lesson_id="lesson-001",
                cards_count=2,
            ).exists()
        )

    def test_missing_token_rejected(self):
        response = self.post_payload(sample_lesson_payload(), token=None)

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.json()["status"], "error")
        self.assertEqual(Lesson.objects.count(), 0)

    def test_invalid_json_rejected(self):
        response = self.client.post(
            self.endpoint,
            data="{bad json",
            content_type="application/json",
            HTTP_AUTHORIZATION="Bearer test-token",
        )

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json()["status"], "error")
        self.assertIn("Invalid JSON", response.json()["error"])
        self.assertEqual(Lesson.objects.count(), 0)
        self.assertTrue(
            ImportLog.objects.filter(source="api", status=ImportLog.Status.ERROR).exists()
        )

    def test_lesson_without_cards_rejected(self):
        response = self.post_payload(sample_lesson_payload(cards=[]))

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json()["status"], "error")
        self.assertIn("cards", response.json()["error"])
        self.assertEqual(Lesson.objects.count(), 0)
        self.assertTrue(
            ImportLog.objects.filter(
                source="api",
                status=ImportLog.Status.ERROR,
                external_lesson_id="lesson-001",
            ).exists()
        )

    def test_imported_cards_are_linked_to_lesson(self):
        self.post_payload(sample_lesson_payload())

        lesson = Lesson.objects.get(external_id="lesson-001")
        cards = list(Card.objects.filter(lesson=lesson).order_by("external_card_id"))
        self.assertEqual(len(cards), 2)
        self.assertEqual(cards[0].lesson, lesson)
        self.assertEqual(cards[0].external_card_id, "1")
        self.assertEqual(cards[0].correct_value, "малако")
        self.assertEqual(cards[1].stars, 1)


class LessonImportServiceTests(TestCase):
    def test_lesson_info_alias_is_supported(self):
        result = import_lesson_payload(
            sample_lesson_payload(lessonInfo="", info="Alias lesson instructions."),
            source="test",
        )

        self.assertEqual(result.lesson.lesson_info, "Alias lesson instructions.")

    def test_duplicate_lesson_is_updated_safely(self):
        initial = sample_lesson_payload()
        updated = sample_lesson_payload(
            title="Updated title",
            cards=[
                {
                    "id": 10,
                    "nativeValue": "город",
                    "correctValue": "горад",
                    "mistake": "город",
                    "cardKind": "MK",
                }
            ],
        )

        first_result = import_lesson_payload(initial, source="test")
        second_result = import_lesson_payload(updated, source="test")

        self.assertEqual(first_result.lesson.id, second_result.lesson.id)
        self.assertEqual(Lesson.objects.count(), 1)
        lesson = Lesson.objects.get(external_id="lesson-001")
        self.assertEqual(lesson.title, "Updated title")
        self.assertEqual(lesson.cards.count(), 1)
        self.assertEqual(lesson.cards.first().external_card_id, "10")
        self.assertEqual(ImportLog.objects.filter(status=ImportLog.Status.SUCCESS).count(), 2)


@override_settings(PUBLIC_SIGNUP_ENABLED=False)
class HomePageTests(TestCase):
    def test_home_page_is_public(self):
        response = self.client.get("/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "MurrLex Server")

    def test_home_page_contains_site_map_links(self):
        response = self.client.get("/")

        expected_links = [
            "/api/health",
            "/login/",
            "/account/",
            "/premium/",
            "/admin/",
            "/lab/import-json/",
            "/lab/lessons/",
            "/lab/imports/",
            "/api/internal/import-lesson",
            "/api/translate",
        ]
        for href in expected_links:
            self.assertContains(response, f'href="{href}"')
        self.assertNotContains(response, 'href="/register/"')

    def test_signed_in_home_replaces_public_card_with_available_apps(self):
        user = get_user_model().objects.create_user(
            username="home-user",
            email="home-user@example.com",
            password="Strong-home-password-2026!",
        )
        self.client.force_login(user)
        response = self.client.get("/")
        self.assertContains(response, "Available")
        self.assertNotContains(response, "Available to you")
        self.assertContains(response, 'href="/account/"')
        self.assertContains(response, 'href="/studio/"')
        self.assertContains(response, 'href="/apps/"')
        self.assertContains(response, 'href="/premium/"')
        self.assertNotContains(response, "<h2>Public access</h2>")

    def test_public_pages_include_theme_switcher(self):
        for path in ("/", "/login/", "/register/", "/premium/"):
            with self.subTest(path=path):
                response = self.client.get(path)
                expected_status = 403 if path == "/register/" else 200
                self.assertContains(
                    response,
                    'data-theme-toggle aria-label="Switch theme"',
                    count=1,
                    status_code=expected_status,
                )
                self.assertContains(
                    response,
                    'localStorage.setItem("theme", next)',
                    status_code=expected_status,
                )

    def test_public_pages_include_safe_back_button(self):
        for path in ("/", "/login/", "/premium/"):
            with self.subTest(path=path):
                response = self.client.get(path)
                self.assertContains(response, '<button class="global-back"', count=1)
                self.assertContains(response, 'data-back-fallback="/"')
                self.assertContains(response, "previousPageIsLocal")


class AdminThemeTests(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_superuser(
            username="admin-theme-test",
            email="admin-theme@example.com",
            password="Strong-admin-theme-password-2026!",
        )
        self.target_user = get_user_model().objects.create_user(
            username="ai-access-target",
            email="ai-target@example.com",
            password="Strong-target-password-2026!",
        )
        self.client.force_login(self.user)

    def test_admin_page_includes_working_theme_toggle(self):
        response = self.client.get("/admin/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "data-admin-theme-toggle")
        self.assertContains(response, "data-admin-theme-icon")
        self.assertContains(response, 'return mode === "light" ? "light" : "dark"')
        self.assertContains(response, "localStorage.setItem(\"theme\", mode)")
        self.assertContains(response, "admin/js/theme.js")

    def test_admin_has_back_button_and_links_are_not_underlined(self):
        response = self.client.get("/admin/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "data-admin-history-back")
        self.assertContains(response, "previousPageIsLocal")
        self.assertContains(response, "text-decoration: none !important")
        self.assertContains(response, "admin-user-avatar")
        self.assertNotContains(response, "Welcome,")
        self.assertContains(response, '>Home</a>')
        self.assertContains(response, '>Help</a>')

    def test_admin_help_explains_roles_and_is_staff_protected(self):
        response = self.client.get("/admin/help/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "No user becomes a Django superuser automatically")
        self.assertContains(response, "Superadmin group")
        self.assertContains(response, "Working with users")
        self.assertContains(response, "Build 2026.07.17.2")
        self.assertContains(response, "Server administration")
        self.assertContains(response, "Workspace roles")
        self.assertContains(response, "Project roles")
        self.client.logout()
        denied = self.client.get("/admin/help/")
        self.assertEqual(denied.status_code, 302)

    def test_admin_login_also_has_back_button(self):
        self.client.logout()

        response = self.client.get("/admin/login/")

        self.assertRedirects(
            response,
            "/accounts/login/?next=/admin/",
            fetch_redirect_response=False,
        )
        secure_login = self.client.get(response["Location"])
        self.assertEqual(secure_login.status_code, 200)
        self.assertContains(secure_login, "data-history-back")
        self.assertContains(secure_login, "Back")

    def test_ai_access_is_managed_from_user_list(self):
        changelist = self.client.get("/admin/lessons/userapiaccess/")
        self.assertEqual(changelist.status_code, 200)
        self.assertContains(changelist, "ai-target@example.com")
        self.assertNotContains(changelist, 'href="/admin/lessons/userapiaccess/add/"')

        add_page = self.client.get("/admin/lessons/userapiaccess/add/")
        self.assertRedirects(add_page, "/admin/lessons/userapiaccess/", fetch_redirect_response=False)

    def test_ai_access_bulk_enable_action(self):
        access = self.target_user.api_access
        self.assertFalse(access.ai_api_enabled)
        response = self.client.post(
            "/admin/lessons/userapiaccess/",
            {"action": "enable_ai_access", "_selected_action": [str(access.id)]},
            follow=True,
        )
        self.assertEqual(response.status_code, 200)
        access.refresh_from_db()
        self.assertTrue(access.ai_api_enabled)


class PremiumPageTests(TestCase):
    def test_premium_page_is_public_placeholder(self):
        response = self.client.get("/premium/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Premium preview")
        self.assertContains(response, "Advanced API access is coming soon")
        self.assertContains(response, "Higher API limits")


@override_settings(
    GOOGLE_OAUTH_ENABLED=True,
    GOOGLE_OAUTH_CLIENT_ID="test-client.apps.googleusercontent.com",
    GOOGLE_OAUTH_TEST_ALLOWLIST_ENABLED=True,
    GOOGLE_OAUTH_ALLOWED_EMAILS=["allowed@gmail.com"],
    GOOGLE_OAUTH_ALLOWED_SUBS=[],
    JWT_SIGNING_KEY="google-api-test-key-at-least-32-bytes",
)
class GoogleOAuthApiTests(TestCase):
    def claims(self, **overrides):
        values = {
            "sub": "google-sub-1",
            "email": "allowed@gmail.com",
            "email_verified": True,
            "name": "Allowed User",
            "picture": "https://example.com/avatar.png",
            "iss": "https://accounts.google.com",
        }
        values.update(overrides)
        return values

    @override_settings(GOOGLE_OAUTH_ENABLED=False)
    def test_disabled_google_oauth_is_rejected(self):
        response = self.client.post("/api/auth/google/", data=json.dumps({"id_token": "token"}), content_type="application/json")
        self.assertEqual(response.status_code, 403)

    @patch("lessons.views.verify_google_id_token")
    def test_allowed_google_identity_creates_basic_user_and_mobile_session(self, verify):
        verify.return_value = self.claims()
        response = self.client.post("/api/auth/google/", data=json.dumps({"id_token": "token"}), content_type="application/json")
        self.assertEqual(response.status_code, 200)
        user = get_user_model().objects.get(email="allowed@gmail.com")
        self.assertFalse(user.is_staff)
        self.assertFalse(user.is_superuser)
        self.assertFalse(UserApiAccess.objects.filter(user=user, ai_api_enabled=True).exists())
        self.assertEqual(SocialAccount.objects.get(provider="google", uid="google-sub-1").user, user)
        self.assertIn("accessToken", response.json())

    @patch("lessons.views.verify_google_id_token")
    def test_unlisted_identity_is_rejected(self, verify):
        verify.return_value = self.claims(email="other@gmail.com")
        response = self.client.post("/api/auth/google/", data=json.dumps({"id_token": "token"}), content_type="application/json")
        self.assertEqual(response.status_code, 403)
        self.assertEqual(response.json()["detail"], "Google account is not allowed for this staging environment.")

    @patch("lessons.views.verify_google_id_token")
    def test_unverified_email_is_rejected(self, verify):
        verify.return_value = self.claims(email_verified=False)
        response = self.client.post("/api/auth/google/", data=json.dumps({"id_token": "token"}), content_type="application/json")
        self.assertEqual(response.status_code, 403)

    @patch("lessons.views.verify_google_id_token", side_effect=GoogleIdentityError("Google id_token is invalid."))
    def test_invalid_token_is_rejected_without_logging_token(self, verify):
        response = self.client.post("/api/auth/google/", data=json.dumps({"id_token": "secret-token"}), content_type="application/json")
        self.assertEqual(response.status_code, 401)

    @patch("lessons.views.verify_google_id_token")
    def test_existing_google_sub_reuses_same_user(self, verify):
        user = get_user_model().objects.create_user("existing-google", email="old@gmail.com")
        SocialAccount.objects.create(user=user, provider="google", uid="google-sub-1", extra_data={})
        verify.return_value = self.claims()
        response = self.client.post("/api/auth/google/", data=json.dumps({"id_token": "token"}), content_type="application/json")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["user"]["id"], user.id)

    @override_settings(GOOGLE_OAUTH_ALLOWED_EMAILS=[])
    @patch("lessons.views.verify_google_id_token")
    def test_database_allowlist_is_case_insensitive(self, verify):
        GoogleOAuthAllowedUser.objects.create(email="Allowed@Gmail.com")
        verify.return_value = self.claims(email="ALLOWED@GMAIL.COM")
        response = self.client.post("/api/auth/google/", data=json.dumps({"id_token": "token"}), content_type="application/json")
        self.assertEqual(response.status_code, 200)

    @override_settings(GOOGLE_OAUTH_ALLOWED_EMAILS=[], GOOGLE_OAUTH_ALLOWED_SUBS=[])
    @patch("lessons.views.verify_google_id_token")
    def test_empty_enabled_allowlist_fails_closed(self, verify):
        verify.return_value = self.claims()
        response = self.client.post("/api/auth/google/", data=json.dumps({"id_token": "token"}), content_type="application/json")
        self.assertEqual(response.status_code, 403)


class LoginAuthenticationTests(TestCase):
    def test_login_page_shows_google_setup_hint_without_credentials(self):
        response = self.client.get("/login/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Username or Email")
        self.assertContains(response, "Google sign-in needs server credentials")
        self.assertNotContains(response, "Continue with Google")

    @override_settings(
        GOOGLE_OAUTH_ENABLED=True,
        SOCIALACCOUNT_PROVIDERS={
            "google": {
                "APP": {
                    "client_id": "test-client-id",
                    "secret": "test-secret",
                    "key": "",
                }
            }
        },
    )
    def test_login_page_shows_google_button_when_enabled(self):
        response = self.client.get("/login/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Continue with Google")
        self.assertContains(response, 'action="/accounts/google/login/')

    def test_google_login_route_is_connected(self):
        self.assertEqual(reverse("google_login"), "/accounts/google/login/")

    @override_settings(GOOGLE_OAUTH_ENABLED=False)
    def test_disabled_browser_google_route_is_rejected(self):
        self.assertEqual(self.client.post("/accounts/google/login/").status_code, 403)

    def test_login_accepts_username(self):
        user_model = get_user_model()
        user = user_model.objects.create_user(
            username="learner",
            email="learner@example.com",
            password="StrongPass-2026!",
        )

        response = self.client.post(
            "/login/",
            data={"username": "learner", "password": "StrongPass-2026!"},
        )

        self.assertRedirects(response, "/account/")
        self.assertEqual(int(self.client.session["_auth_user_id"]), user.id)

    def test_login_accepts_email(self):
        user_model = get_user_model()
        user = user_model.objects.create_user(
            username="learner",
            email="learner@example.com",
            password="StrongPass-2026!",
        )

        response = self.client.post(
            "/login/",
            data={"username": "learner@example.com", "password": "StrongPass-2026!"},
        )

        self.assertRedirects(response, "/account/")
        self.assertEqual(int(self.client.session["_auth_user_id"]), user.id)

    def test_login_accepts_email_case_insensitively(self):
        user_model = get_user_model()
        user = user_model.objects.create_user(
            username="learner",
            email="learner@example.com",
            password="StrongPass-2026!",
        )

        response = self.client.post(
            "/login/",
            data={"username": "LEARNER@example.com", "password": "StrongPass-2026!"},
        )

        self.assertRedirects(response, "/account/")
        self.assertEqual(int(self.client.session["_auth_user_id"]), user.id)

    def test_login_rejects_unknown_email(self):
        self.client.post(
            "/login/",
            data={"username": "missing@example.com", "password": "StrongPass-2026!"},
        )

        self.assertNotIn("_auth_user_id", self.client.session)

    def test_login_rejects_ambiguous_email(self):
        user_model = get_user_model()
        user_model.objects.create_user(
            username="first",
            email="shared@example.com",
            password="StrongPass-2026!",
        )
        user_model.objects.create_user(
            username="second",
            email="shared@example.com",
            password="StrongPass-2026!",
        )

        response = self.client.post(
            "/login/",
            data={"username": "shared@example.com", "password": "StrongPass-2026!"},
        )

        self.assertEqual(response.status_code, 200)
        self.assertNotIn("_auth_user_id", self.client.session)


@override_settings(PUBLIC_SIGNUP_ENABLED=False)
class ClosedSignupTests(TestCase):
    def test_closed_registration_page_explains_invite_only_access(self):
        response = self.client.get("/register/")

        self.assertEqual(response.status_code, 403)
        self.assertContains(response, "Closed testing", status_code=403)
        self.assertContains(response, "Google email", status_code=403)

    def test_closed_web_registration_does_not_create_user(self):
        response = self.client.post(
            "/register/",
            data={
                "username": "outsider",
                "email": "outsider@example.com",
                "password1": "StrongPass-2026!",
                "password2": "StrongPass-2026!",
            },
        )

        self.assertEqual(response.status_code, 403)
        self.assertFalse(get_user_model().objects.filter(username="outsider").exists())

    def test_closed_mobile_registration_does_not_create_user(self):
        response = self.client.post(
            "/api/auth/register",
            data=json.dumps(
                {
                    "username": "outsider",
                    "email": "outsider@example.com",
                    "password": "StrongPass-2026!",
                }
            ),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 403)
        self.assertFalse(get_user_model().objects.filter(username="outsider").exists())

    def test_login_page_hides_public_registration_link(self):
        response = self.client.get("/login/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Closed testing")
        self.assertNotContains(response, 'href="/register/"')


@override_settings(
    PUBLIC_SIGNUP_ENABLED=True,
    REGISTRATION_ALLOWLIST_ENABLED=True,
    GOOGLE_OAUTH_ALLOWED_EMAILS=[],
)
class AllowlistedRegistrationTests(TestCase):
    registration_data = {
        "username": "invited-user",
        "email": "invited@example.com",
        "password1": "StrongPass-2026!",
        "password2": "StrongPass-2026!",
    }

    def test_login_and_registration_pages_offer_self_registration(self):
        home_page = self.client.get("/")
        login_page = self.client.get("/login/")
        registration_page = self.client.get("/register/")

        self.assertContains(home_page, 'href="/register/"')
        self.assertContains(home_page, "Register")
        self.assertEqual(login_page.status_code, 200)
        self.assertContains(login_page, 'href="/register/"')
        self.assertContains(login_page, "Create your account")
        self.assertEqual(registration_page.status_code, 200)
        self.assertContains(registration_page, "choose your own username and password")

    def test_invited_email_can_register_case_insensitively(self):
        GoogleOAuthAllowedUser.objects.create(email="Invited@Example.com")
        data = {**self.registration_data, "email": "INVITED@example.com"}

        response = self.client.post("/register/", data=data)

        self.assertRedirects(response, "/account/")
        user = get_user_model().objects.get(username="invited-user")
        self.assertEqual(user.email, "invited@example.com")
        self.assertEqual(int(self.client.session["_auth_user_id"]), user.pk)

    def test_unknown_or_disabled_email_cannot_register(self):
        GoogleOAuthAllowedUser.objects.create(
            email="disabled@example.com",
            is_active=False,
        )

        for email in ("outsider@example.com", "disabled@example.com"):
            with self.subTest(email=email):
                data = {
                    **self.registration_data,
                    "username": email.split("@", 1)[0],
                    "email": email,
                }
                response = self.client.post("/register/", data=data)
                self.assertEqual(response.status_code, 200)
                self.assertContains(response, "This email is not invited")
                self.assertFalse(get_user_model().objects.filter(email__iexact=email).exists())

    def test_mobile_registration_uses_the_same_allowlist(self):
        GoogleOAuthAllowedUser.objects.create(email="mobile-invited@example.com")
        allowed = self.client.post(
            "/api/auth/register",
            data=json.dumps(
                {
                    "username": "mobile-invited",
                    "email": "mobile-invited@example.com",
                    "password": "StrongPass-2026!",
                    "deviceName": "Android",
                }
            ),
            content_type="application/json",
        )
        denied = self.client.post(
            "/api/auth/register",
            data=json.dumps(
                {
                    "username": "mobile-outsider",
                    "email": "mobile-outsider@example.com",
                    "password": "StrongPass-2026!",
                    "deviceName": "Android",
                }
            ),
            content_type="application/json",
        )

        self.assertEqual(allowed.status_code, 201)
        self.assertEqual(denied.status_code, 400)
        self.assertIn("email", denied.json()["fields"])
        self.assertFalse(get_user_model().objects.filter(username="mobile-outsider").exists())


@override_settings(PUBLIC_SIGNUP_ENABLED=True, REGISTRATION_ALLOWLIST_ENABLED=False)
class PublicAccountTests(TestCase):
    def test_register_page_is_public(self):
        response = self.client.get("/register/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Create account")

    def test_registration_creates_user_and_logs_them_in(self):
        response = self.client.post(
            "/register/",
            data={
                "username": "newlearner",
                "email": "newlearner@example.com",
                "password1": "StrongPass-2026!",
                "password2": "StrongPass-2026!",
            },
        )

        self.assertRedirects(response, "/account/")
        user_model = get_user_model()
        user = user_model.objects.get(username="newlearner")
        self.assertEqual(user.email, "newlearner@example.com")
        self.assertEqual(int(self.client.session["_auth_user_id"]), user.id)

    def test_duplicate_email_is_rejected(self):
        user_model = get_user_model()
        user_model.objects.create_user(
            username="existing",
            email="learner@example.com",
            password="test-password",
        )

        response = self.client.post(
            "/register/",
            data={
                "username": "otherlearner",
                "email": "LEARNER@example.com",
                "password1": "StrongPass-2026!",
                "password2": "StrongPass-2026!",
            },
        )

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "A user with this email already exists.")
        self.assertFalse(user_model.objects.filter(username="otherlearner").exists())

    def test_account_requires_login(self):
        response = self.client.get("/account/")

        self.assertEqual(response.status_code, 302)
        self.assertTrue(response["Location"].startswith("/login/"))

    def test_logged_in_user_can_open_account(self):
        user_model = get_user_model()
        user = user_model.objects.create_user(
            username="learner",
            email="learner@example.com",
            password="test-password",
        )
        self.client.force_login(user)

        response = self.client.get("/account/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "User cabinet")
        self.assertContains(response, "learner@example.com")
        self.assertContains(response, "Current plan")
        self.assertContains(response, "Free")
        self.assertContains(response, "Premium area")

    def test_account_settings_requires_login(self):
        response = self.client.get("/account/settings/")

        self.assertEqual(response.status_code, 302)
        self.assertTrue(response["Location"].startswith("/login/"))

    def test_logged_in_user_can_update_account_settings(self):
        user_model = get_user_model()
        user = user_model.objects.create_user(
            username="learner",
            email="learner@example.com",
            password="test-password",
        )
        self.client.force_login(user)

        response = self.client.post(
            "/account/settings/",
            data={
                "username": "updatedlearner",
                "email": "updated@example.com",
                "display_name": "Updated Learner",
            },
        )

        self.assertRedirects(response, "/account/")
        user.refresh_from_db()
        self.assertEqual(user.username, "updatedlearner")
        self.assertEqual(user.email, "updated@example.com")
        self.assertEqual(user.first_name, "Updated Learner")

    def test_account_settings_rejects_duplicate_email(self):
        user_model = get_user_model()
        user_model.objects.create_user(
            username="existing",
            email="existing@example.com",
            password="test-password",
        )
        user = user_model.objects.create_user(
            username="learner",
            email="learner@example.com",
            password="test-password",
        )
        self.client.force_login(user)

        response = self.client.post(
            "/account/settings/",
            data={
                "username": "learner",
                "email": "EXISTING@example.com",
                "display_name": "",
            },
        )

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "A user with this email already exists.")
        user.refresh_from_db()
        self.assertEqual(user.email, "learner@example.com")

    def test_logged_in_user_can_change_password(self):
        user_model = get_user_model()
        user = user_model.objects.create_user(
            username="learner",
            email="learner@example.com",
            password="OldPass-2026!",
        )
        self.client.force_login(user)

        response = self.client.post(
            "/password-change/",
            data={
                "old_password": "OldPass-2026!",
                "new_password1": "NewPass-2026!",
                "new_password2": "NewPass-2026!",
            },
        )

        self.assertRedirects(response, "/password-change/done/")
        user.refresh_from_db()
        self.assertTrue(user.check_password("NewPass-2026!"))


@override_settings(INTERNAL_IMPORT_TOKEN="test-token")
class LabUiAuthTests(TestCase):
    def test_lab_requires_login(self):
        response = self.client.get("/lab/lessons/")

        self.assertEqual(response.status_code, 302)
        self.assertTrue(response["Location"].startswith("/login/"))

    def test_logged_in_user_can_open_lab(self):
        user_model = get_user_model()
        user = user_model.objects.create_user(username="methodist", password="test-password")
        self.client.force_login(user)

        response = self.client.get("/lab/lessons/")

        self.assertEqual(response.status_code, 200)

    def test_api_still_uses_bearer_token_not_login(self):
        user_model = get_user_model()
        user = user_model.objects.create_user(username="methodist", password="test-password")
        self.client.force_login(user)

        response = self.client.post(
            "/api/internal/import-lesson",
            data=json.dumps(sample_lesson_payload()),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 401)

class OperationalEndpointTests(TestCase):
    def test_health_response_has_generated_request_id(self):
        response = self.client.get("/api/health")
        self.assertEqual(response.status_code, 200)
        self.assertRegex(response["X-Request-ID"], r"^[a-f0-9]{32}$")

    def test_valid_caller_request_id_is_preserved(self):
        response = self.client.get("/api/health", HTTP_X_REQUEST_ID="smoke-test-1234")
        self.assertEqual(response["X-Request-ID"], "smoke-test-1234")

    def test_readiness_checks_database_and_private_storage(self):
        import tempfile
        from pathlib import Path

        with tempfile.TemporaryDirectory() as directory:
            with self.settings(STUDIO_PRIVATE_MEDIA_ROOT=Path(directory)):
                response = self.client.get("/api/ready")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["status"], "ready")
        self.assertEqual(response.json()["checks"], {"database": True, "private_storage": True})

    @patch("django.db.backends.utils.CursorWrapper.execute", side_effect=RuntimeError("database unavailable"))
    def test_readiness_returns_503_without_leaking_exception(self, _execute):
        response = self.client.get("/api/ready")
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json()["status"], "unavailable")
        self.assertNotContains(response, "database unavailable", status_code=503)

class AccountAppNavigationTests(TestCase):
    def setUp(self):
        from .models import UserApiAccess

        users = get_user_model()
        self.user = users.objects.create_user("studio-app-user", password="strong-pass")
        access, _ = UserApiAccess.objects.get_or_create(user=self.user)
        access.ai_api_enabled = True
        access.save(update_fields=["ai_api_enabled"])
        ProviderCredential.objects.create(
            provider=ProviderCredential.Provider.OPENAI,
            encrypted_api_key="test-encrypted-key",
        )
        self.client.force_login(self.user)

    def test_account_lists_lexamora_studio_as_separate_app(self):
        response = self.client.get("/account/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "MurrLex Apps")
        self.assertContains(response, "Lexamora Studio")
        self.assertContains(response, "/studio/")

    def test_apps_dashboard_has_studio_card(self):
        response = self.client.get("/apps/")
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Lexamora Studio")
        self.assertContains(response, "Open Studio")


class DailyAdminRoleTests(TestCase):
    def setUp(self):
        users = get_user_model()
        self.superuser = users.objects.create_superuser(
            "role-owner", "owner@example.com", "strong-pass"
        )
        self.daily_admin = users.objects.create_user(
            "daily-admin", "daily@example.com", "strong-pass", is_staff=True
        )
        self.regular_user = users.objects.create_user(
            "regular-user", "regular@example.com", "strong-pass"
        )
        self.group = sync_daily_admin_group()
        self.daily_admin.groups.add(self.group)

    def test_role_contains_routine_permissions_without_server_secrets(self):
        permissions = set(
            self.group.permissions.values_list(
                "content_type__app_label", "codename"
            )
        )

        self.assertIn(("auth", "change_user"), permissions)
        self.assertIn(("lessons", "change_userapiaccess"), permissions)
        self.assertIn(("lessons", "change_lesson"), permissions)
        self.assertNotIn(("auth", "change_group"), permissions)
        self.assertNotIn(("lessons", "change_providercredential"), permissions)

    def test_role_sync_is_idempotent(self):
        synced_again = sync_daily_admin_group()

        self.assertEqual(synced_again.pk, self.group.pk)
        self.assertEqual(Group.objects.filter(name=DAILY_ADMIN_GROUP).count(), 1)

    def test_daily_admin_can_manage_regular_user_but_not_superuser(self):
        self.client.force_login(self.daily_admin)

        regular_response = self.client.get(
            reverse("admin:auth_user_change", args=[self.regular_user.pk])
        )
        owner_response = self.client.get(
            reverse("admin:auth_user_change", args=[self.superuser.pk])
        )
        provider_response = self.client.get(
            reverse("admin:lessons_providercredential_changelist")
        )

        self.assertEqual(regular_response.status_code, 200)
        self.assertEqual(owner_response.status_code, 200)
        self.assertTrue(regular_response.context["has_change_permission"])
        self.assertFalse(owner_response.context["has_change_permission"])
        self.assertEqual(provider_response.status_code, 403)
        self.assertNotContains(regular_response, 'name="is_superuser"')


class StandardAdminGroupTests(TestCase):
    def setUp(self):
        self.groups = sync_admin_groups()

    def permission_names(self, role_name):
        return set(
            self.groups[role_name].permissions.values_list(
                "content_type__app_label", "codename"
            )
        )

    def test_standard_groups_are_created_with_distinct_permission_sets(self):
        self.assertEqual(set(self.groups), set(ROLE_NAMES))

        support = self.permission_names("Support")
        billing = self.permission_names("Billing")
        content = self.permission_names("Content")
        readonly = self.permission_names("ReadOnly")
        developer = self.permission_names("Developer")

        self.assertIn(("auth", "change_user"), support)
        self.assertNotIn(("lessons", "change_providercredential"), support)
        self.assertIn(("lessons", "change_userapiaccess"), billing)
        self.assertNotIn(("auth", "change_user"), billing)
        self.assertIn(("lessons", "change_lesson"), content)
        self.assertIn(("lexamora_studio", "change_workspace"), content)
        self.assertIn(("auth", "view_user"), readonly)
        self.assertNotIn(("auth", "change_user"), readonly)
        self.assertNotIn(("lessons", "view_providercredential"), readonly)
        self.assertIn(("lessons", "view_providercredential"), developer)
        self.assertNotIn(("lessons", "change_providercredential"), developer)
        self.assertEqual(
            self.groups["Superadmin"].permissions.count(),
            Permission.objects.count(),
        )

    def test_superuser_can_open_group_dashboard_and_create_custom_group(self):
        user = get_user_model().objects.create_superuser(
            "group-owner", "group-owner@example.com", "strong-pass"
        )
        self.client.force_login(user)

        dashboard = self.client.get("/admin/")
        group_list = self.client.get("/admin/auth/group/")
        created = self.client.post(
            "/admin/auth/group/add/",
            {"name": "Custom Operations", "permissions": []},
        )

        self.assertEqual(dashboard.status_code, 200)
        self.assertContains(dashboard, "Groups &amp; Roles", html=True)
        self.assertContains(dashboard, "Support, Billing, Content, ReadOnly, Developer, and Superadmin")
        self.assertEqual(group_list.status_code, 200)
        self.assertContains(group_list, "Permissions")
        self.assertRedirects(created, "/admin/auth/group/")
        self.assertTrue(Group.objects.filter(name="Custom Operations").exists())
