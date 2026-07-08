import json

from django.contrib.auth import get_user_model
from django.urls import reverse
from django.test import TestCase, override_settings

from .models import Card, ImportLog, Lesson
from .services import import_lesson_payload


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
