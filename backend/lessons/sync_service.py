from __future__ import annotations

from django.db import transaction
from django.utils import timezone

from .models import Card, Lesson


def sync_timestamp() -> str:
    return timezone.now().strftime("%Y-%m-%d %H:%M:%S")


def _card_id_value(value: str):
    return int(value) if value.isdigit() else value


def card_payload(card: Card) -> dict:
    raw = dict(card.raw_json or {})
    raw.update(
        {
            "id": _card_id_value(card.external_card_id),
            "nativeValue": card.native_value,
            "correctValue": card.correct_value,
            "hint": card.hint,
            "mistake": card.mistake,
            "cardKind": card.card_kind,
            "sourceLanguage": card.source_language,
            "targetLanguage": card.target_language,
            "stars": card.stars,
        }
    )
    return raw


def lesson_payload(lesson: Lesson) -> dict:
    raw = dict(lesson.raw_json or {})
    raw.update(
        {
            "id": lesson.external_id,
            "title": lesson.title,
            "lessonInfo": lesson.lesson_info,
            "sourceLanguage": lesson.source_language,
            "targetLanguage": lesson.target_language,
            "cardKind": lesson.card_kind,
            "createdAt": raw.get("createdAt") or lesson.created_at.strftime("%Y-%m-%d %H:%M:%S"),
            "updatedAt": raw.get("updatedAt") or lesson.updated_at.strftime("%Y-%m-%d %H:%M:%S"),
            "cards": [card_payload(card) for card in lesson.cards.all()],
        }
    )
    return raw


def user_lessons_payload(user, lesson_id: str = "", card_id: str = "") -> list[dict]:
    lessons = Lesson.objects.filter(owner=user).prefetch_related("cards")
    if lesson_id:
        lessons = lessons.filter(external_id=lesson_id)
    payloads = [lesson_payload(lesson) for lesson in lessons]
    if card_id:
        for payload in payloads:
            payload["cards"] = [card for card in payload["cards"] if str(card.get("id", "")) == str(card_id)]
    return payloads


def mark_lesson_updated(lesson: Lesson) -> None:
    lesson.raw_json = {**(lesson.raw_json or {}), "updatedAt": sync_timestamp()}
    lesson.save(update_fields=["raw_json", "updated_at"])


def mark_card_updated(card: Card) -> None:
    card.raw_json = {**(card.raw_json or {}), "updatedAt": sync_timestamp()}
    card.save(update_fields=["raw_json", "updated_at"])
    mark_lesson_updated(card.lesson)


@transaction.atomic
def merge_mobile_lessons(user, payloads: list[dict], scope: str, lesson_id: str = "", card_id: str = "") -> None:
    for payload in payloads:
        if not isinstance(payload, dict):
            continue
        external_id = str(payload.get("id", "")).strip()
        if not external_id or (lesson_id and external_id != lesson_id):
            continue
        existing = Lesson.objects.filter(owner=user, external_id=external_id).first()
        incoming_updated = str(payload.get("updatedAt", ""))
        existing_updated = str((existing.raw_json or {}).get("updatedAt", "")) if existing else ""
        if existing and existing_updated and incoming_updated and incoming_updated < existing_updated:
            continue
        lesson, _ = Lesson.objects.update_or_create(
            owner=user,
            external_id=external_id,
            defaults={
                "title": str(payload.get("title", "Untitled lesson"))[:255],
                "card_kind": str(payload.get("cardKind", ""))[:20],
                "source_language": str(payload.get("sourceLanguage", ""))[:100],
                "target_language": str(payload.get("targetLanguage", ""))[:100],
                "lesson_info": str(payload.get("lessonInfo", "")),
                "raw_json": payload,
            },
        )
        incoming_cards = payload.get("cards", [])
        if not isinstance(incoming_cards, list):
            continue
        accepted_ids = set()
        for index, raw_card in enumerate(incoming_cards):
            if not isinstance(raw_card, dict):
                continue
            external_card_id = str(raw_card.get("id", index + 1)).strip()
            if card_id and external_card_id != str(card_id):
                continue
            accepted_ids.add(external_card_id)
            Card.objects.update_or_create(
                lesson=lesson,
                external_card_id=external_card_id,
                defaults={
                    "native_value": str(raw_card.get("nativeValue", raw_card.get("ru", ""))),
                    "correct_value": str(raw_card.get("correctValue", raw_card.get("pl", ""))),
                    "hint": str(raw_card.get("hint", "")),
                    "mistake": str(raw_card.get("mistake", "")),
                    "card_kind": str(raw_card.get("cardKind", payload.get("cardKind", "")))[:20],
                    "source_language": str(raw_card.get("sourceLanguage", payload.get("sourceLanguage", "")))[:100],
                    "target_language": str(raw_card.get("targetLanguage", payload.get("targetLanguage", "")))[:100],
                    "stars": max(0, min(7, int(raw_card.get("stars", 0) or 0))),
                    "raw_json": raw_card,
                },
            )
        if scope == "lesson" and accepted_ids:
            lesson.cards.exclude(external_card_id__in=accepted_ids).delete()
