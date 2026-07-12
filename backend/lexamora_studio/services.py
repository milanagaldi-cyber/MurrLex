from django.db import transaction
from django.utils import timezone

from .models import Prompt, Revision, SubtitleLine, TranslationUnit, Workspace, WorkspaceMembership
from .revisions import record_revision


@transaction.atomic
def create_workspace(*, user, name, slug, description=""):
    workspace = Workspace.objects.create(
        name=name,
        slug=slug,
        description=description,
        owner=user,
        created_by=user,
        updated_by=user,
    )
    WorkspaceMembership.objects.create(
        workspace=workspace,
        user=user,
        role=WorkspaceMembership.Role.OWNER,
        status=WorkspaceMembership.Status.ACTIVE,
        can_use_ai=True,
        can_export=True,
        can_manage_members=True,
    )
    return workspace

@transaction.atomic
def update_dialogue_line(*, line, user, text, speaker=None, delivery=None):
    line.text = text
    if speaker is not None:
        line.speaker = speaker
    if delivery is not None:
        line.delivery = delivery
    line.updated_by = user
    line.full_clean()
    line.save()
    revision = record_revision(instance=line, user=user, operation="UPDATE")
    TranslationUnit.objects.filter(dialogue_line=line).update(status=TranslationUnit.Status.STALE)
    Prompt.objects.filter(blocks__source_dialogue=line).update(needs_review=True)
    return line


@transaction.atomic
def bulk_replace_subtitle_lines(*, track, user, texts):
    existing = {line.position: line for line in SubtitleLine.all_objects.select_for_update().filter(track=track)}
    now = timezone.now()
    active_positions = set()
    for position, text in enumerate(texts):
        clean = str(text).strip()
        if not clean:
            continue
        active_positions.add(position)
        line = existing.get(position)
        if line is None:
            SubtitleLine.objects.create(
                track=track, position=position, text=clean, created_by=user, updated_by=user,
            )
        else:
            line.text = clean
            line.deleted_at = None
            line.deleted_by = None
            line.updated_by = user
            line.save(update_fields=["text", "deleted_at", "deleted_by", "updated_by", "updated_at"])
    for position, line in existing.items():
        if position not in active_positions and line.deleted_at is None:
            line.deleted_at = now
            line.deleted_by = user
            line.updated_by = user
            line.save(update_fields=["deleted_at", "deleted_by", "updated_by", "updated_at"])
    return list(track.lines.all())


@transaction.atomic
def reorder_subtitle_lines(*, track, user, ordered_ids):
    lines = list(track.lines.select_for_update())
    by_id = {str(line.id): line for line in lines}
    normalized = [str(value) for value in ordered_ids]
    if len(normalized) != len(lines) or set(normalized) != set(by_id):
        raise ValueError("The reorder request must contain every active subtitle line exactly once.")
    for offset, line_id in enumerate(normalized):
        line = by_id[line_id]
        line.position = 100000 + offset
        line.save(update_fields=["position", "updated_at"])
    for position, line_id in enumerate(normalized):
        line = by_id[line_id]
        line.position = position
        line.updated_by = user
        line.save(update_fields=["position", "updated_by", "updated_at"])
    return [by_id[line_id] for line_id in normalized]


@transaction.atomic
def save_translation(*, dialogue_line, user, target_language, translated_text, status):
    source_revision = Revision.objects.filter(
        entity_type=dialogue_line._meta.label_lower, entity_id=dialogue_line.id
    ).order_by("-sequence").first()
    if source_revision is None:
        source_revision = record_revision(instance=dialogue_line, user=user, operation="TRANSLATION_SOURCE")
    unit, _ = TranslationUnit.all_objects.get_or_create(
        dialogue_line=dialogue_line,
        target_language=target_language,
        defaults={
            "source_text": dialogue_line.text,
            "translated_text": translated_text,
            "status": status,
            "source_revision": source_revision,
            "created_by": user,
            "updated_by": user,
        },
    )
    unit.source_text = dialogue_line.text
    unit.translated_text = translated_text
    unit.status = status
    unit.source_revision = source_revision
    unit.deleted_at = None
    unit.deleted_by = None
    unit.updated_by = user
    unit.full_clean()
    unit.save()
    return unit