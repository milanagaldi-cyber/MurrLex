import json
from unittest.mock import patch

from cryptography.fernet import Fernet
from django.contrib.auth import get_user_model
from django.urls import reverse
from django.test import TestCase, override_settings

from .models import Card, ImportLog, Lesson, ProviderCredential, UserApiAccess
from .provider_credentials import get_provider_api_key
from .services import import_lesson_payload


@override_settings(
    JWT_SIGNING_KEY="mobile-api-test-key-at-least-32-bytes",
    JWT_ACCESS_MINUTES=15,
    JWT_REFRESH_DAYS=30,
    CREDENTIAL_ENCRYPTION_KEY=Fernet.generate_key().decode("ascii"),
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
            "/register/",
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

    def test_public_pages_include_theme_switcher(self):
        for path in ("/", "/login/", "/register/", "/premium/"):
            with self.subTest(path=path):
                response = self.client.get(path)
                self.assertContains(
                    response,
                    'data-theme-toggle aria-label="Switch theme"',
                    count=1,
                )
                self.assertContains(response, 'localStorage.setItem("theme", next)')


class AdminThemeTests(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_superuser(
            username="admin-theme-test",
            email="admin-theme@example.com",
            password="Strong-admin-theme-password-2026!",
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


class PremiumPageTests(TestCase):
    def test_premium_page_is_public_placeholder(self):
        response = self.client.get("/premium/")

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Premium preview")
        self.assertContains(response, "Advanced API access is coming soon")
        self.assertContains(response, "Higher API limits")


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
