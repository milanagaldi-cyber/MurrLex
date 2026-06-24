from dataclasses import dataclass
from typing import Any

from django.db import transaction

from .models import Card, ImportLog, Lesson


class LessonImportError(ValueError):
    pass


@dataclass(frozen=True)
class LessonImportResult:
    lesson: Lesson
    cards_imported: int
    import_log: ImportLog


def import_lesson_payload(payload: dict[str, Any], source: str) -> LessonImportResult:
    if not isinstance(payload, dict):
        raise LessonImportError("Payload must be a JSON object.")

    external_id = str(payload.get("id", "")).strip()
    title = str(payload.get("title", "")).strip()
    cards_payload = payload.get("cards")

    if not external_id:
        raise LessonImportError("Lesson id is required.")
    if not title:
        raise LessonImportError("Lesson title is required.")
    if not isinstance(cards_payload, list) or not cards_payload:
        raise LessonImportError("Lesson cards must be a non-empty list.")

    lesson_card_kind = _text(payload.get("cardKind"))
    lesson_source_language = _text(payload.get("sourceLanguage"))
    lesson_target_language = _text(payload.get("targetLanguage"))
    lesson_info = _text(payload.get("lessonInfo")) or _text(payload.get("info"))

    if not lesson_card_kind:
        lesson_card_kind = _first_card_text(cards_payload, "cardKind")
    if not lesson_source_language:
        lesson_source_language = _first_card_text(cards_payload, "sourceLanguage")
    if not lesson_target_language:
        lesson_target_language = _first_card_text(cards_payload, "targetLanguage")

    with transaction.atomic():
        lesson, _created = Lesson.objects.update_or_create(
            external_id=external_id,
            defaults={
                "title": title,
                "card_kind": lesson_card_kind,
                "source_language": lesson_source_language,
                "target_language": lesson_target_language,
                "lesson_info": lesson_info,
                "raw_json": payload,
            },
        )

        lesson.cards.all().delete()
        cards = [
            _card_from_payload(lesson=lesson, card_payload=card_payload, index=index)
            for index, card_payload in enumerate(cards_payload, start=1)
        ]
        Card.objects.bulk_create(cards)

        import_log = ImportLog.objects.create(
            source=source,
            status=ImportLog.Status.SUCCESS,
            external_lesson_id=external_id,
            cards_count=len(cards),
            raw_payload=payload,
        )

    return LessonImportResult(lesson=lesson, cards_imported=len(cards), import_log=import_log)


def log_failed_import(*, source: str, raw_payload: Any, error_message: str, external_lesson_id: str = "") -> ImportLog:
    return ImportLog.objects.create(
        source=source,
        status=ImportLog.Status.ERROR,
        external_lesson_id=external_lesson_id,
        cards_count=0,
        error_message=error_message,
        raw_payload=raw_payload,
    )


def _card_from_payload(lesson: Lesson, card_payload: Any, index: int) -> Card:
    if not isinstance(card_payload, dict):
        raise LessonImportError(f"Card #{index} must be a JSON object.")

    external_card_id = _text(card_payload.get("id")) or str(index)
    return Card(
        lesson=lesson,
        external_card_id=external_card_id,
        native_value=_text(card_payload.get("nativeValue")),
        correct_value=_text(card_payload.get("correctValue")),
        hint=_text(card_payload.get("hint")),
        mistake=_text(card_payload.get("mistake")),
        card_kind=_text(card_payload.get("cardKind")),
        source_language=_text(card_payload.get("sourceLanguage")),
        target_language=_text(card_payload.get("targetLanguage")),
        raw_json=card_payload,
        stars=_int(card_payload.get("stars")),
    )


def _first_card_text(cards_payload: list[Any], field_name: str) -> str:
    for card_payload in cards_payload:
        if isinstance(card_payload, dict):
            value = _text(card_payload.get(field_name))
            if value:
                return value
    return ""


def _text(value: Any) -> str:
    if value is None:
        return ""
    return str(value).strip()


def _int(value: Any) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0
