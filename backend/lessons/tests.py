import json

from django.contrib.auth import get_user_model
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
