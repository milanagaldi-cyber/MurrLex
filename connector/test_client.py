import json
import os
import unittest
from unittest.mock import MagicMock, patch

from .client import ConnectorConfigError, save_lesson_to_make_mistakes


class SaveLessonToMakeMistakesTests(unittest.TestCase):
    def test_requires_import_url(self):
        with patch.dict(os.environ, {"INTERNAL_IMPORT_TOKEN": "token"}, clear=True):
            with self.assertRaises(ConnectorConfigError):
                save_lesson_to_make_mistakes({"id": "lesson"})

    def test_requires_import_token(self):
        with patch.dict(os.environ, {"DJANGO_IMPORT_URL": "http://example.test/import"}, clear=True):
            with self.assertRaises(ConnectorConfigError):
                save_lesson_to_make_mistakes({"id": "lesson"})

    def test_posts_lesson_to_configured_endpoint(self):
        response = MagicMock()
        response.read.return_value = b'{"status":"ok","lessonId":"lesson-1","cardsImported":1}'
        response.__enter__.return_value = response

        with patch("connector.client.urlopen", return_value=response) as urlopen_mock:
            result = save_lesson_to_make_mistakes(
                {"id": "lesson-1", "title": "Lesson 1", "cards": [{"id": 1}]},
                import_url="http://example.test/api/internal/import-lesson",
                import_token="secret-token",
            )

        self.assertEqual(result["status"], "ok")
        request = urlopen_mock.call_args.args[0]
        self.assertEqual(request.full_url, "http://example.test/api/internal/import-lesson")
        self.assertEqual(request.get_method(), "POST")
        self.assertEqual(request.headers["Authorization"], "Bearer secret-token")
        self.assertEqual(request.headers["Content-type"], "application/json")
        self.assertEqual(json.loads(request.data.decode("utf-8"))["id"], "lesson-1")


if __name__ == "__main__":
    unittest.main()
