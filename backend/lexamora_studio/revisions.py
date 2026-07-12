import difflib
import uuid

from django.db import transaction
from django.forms.models import model_to_dict

from .models import (
    AdditionalGeneration,
    AuditEvent,
    Character,
    DialogueLine,
    Episode,
    Project,
    Prompt,
    PromptBlock,
    Revision,
    Scene,
)


VERSIONED_MODELS = {
    "lexamora_studio.project": (Project, ["title", "concept", "original_language", "translation_languages", "rights_holder", "publication_info", "status"]),
    "lexamora_studio.character": (Character, ["name", "description", "visual_description", "position"]),
    "lexamora_studio.episode": (Episode, ["number", "title", "summary", "position"]),
    "lexamora_studio.scene": (Scene, ["number", "title", "hook", "description", "location", "actions", "performance_notes", "position", "status"]),
    "lexamora_studio.dialogueline": (DialogueLine, ["speaker", "text", "language", "delivery", "position", "status"]),
    "lexamora_studio.prompt": (Prompt, ["title", "status", "position", "needs_review"]),
    "lexamora_studio.promptblock": (PromptBlock, ["block_type", "content", "position"]),
    "lexamora_studio.additionalgeneration": (AdditionalGeneration, ["reason", "prompt", "position", "status"]),
}


def workspace_for(instance):
    if isinstance(instance, Project):
        return instance.workspace
    if isinstance(instance, Character):
        return instance.project.workspace
    if isinstance(instance, Episode):
        return instance.project.workspace
    if isinstance(instance, Scene):
        return instance.episode.project.workspace
    if isinstance(instance, DialogueLine):
        return instance.scene.episode.project.workspace
    if isinstance(instance, (Prompt, PromptBlock)):
        prompt = instance if isinstance(instance, Prompt) else instance.prompt
        return prompt.scene.episode.project.workspace
    if isinstance(instance, AdditionalGeneration):
        return instance.scene.episode.project.workspace
    raise ValueError("Unsupported revision entity.")


def snapshot_for(instance):
    key = instance._meta.label_lower
    fields = VERSIONED_MODELS[key][1]
    values = model_to_dict(instance, fields=fields)
    for name, value in list(values.items()):
        if isinstance(value, uuid.UUID):
            values[name] = str(value)
    return values


@transaction.atomic
def record_revision(*, instance, user, operation):
    entity_type = instance._meta.label_lower
    snapshot = snapshot_for(instance)
    latest = (
        Revision.objects.select_for_update()
        .filter(entity_type=entity_type, entity_id=instance.pk)
        .order_by("-sequence")
        .first()
    )
    previous = latest.snapshot if latest else {}
    changed = sorted(key for key in set(previous) | set(snapshot) if previous.get(key) != snapshot.get(key))
    return Revision.objects.create(
        workspace=workspace_for(instance),
        entity_type=entity_type,
        entity_id=instance.pk,
        sequence=(latest.sequence + 1) if latest else 1,
        operation=operation,
        snapshot=snapshot,
        changed_fields=changed,
        author=user,
    )


def revision_diff(older, newer):
    fields = sorted(set(older.snapshot) | set(newer.snapshot))
    result = {}
    for field in fields:
        before = older.snapshot.get(field)
        after = newer.snapshot.get(field)
        if before == after:
            continue
        if isinstance(before, str) or isinstance(after, str):
            result[field] = {
                "before": before,
                "after": after,
                "unified": list(difflib.unified_diff(
                    str(before or "").splitlines(),
                    str(after or "").splitlines(),
                    fromfile=f"revision-{older.sequence}",
                    tofile=f"revision-{newer.sequence}",
                    lineterm="",
                )),
            }
        else:
            result[field] = {"before": before, "after": after}
    return result


@transaction.atomic
def restore_revision(*, revision, user):
    model, allowed_fields = VERSIONED_MODELS[revision.entity_type]
    instance = model.all_objects.select_for_update().get(pk=revision.entity_id)
    for field in allowed_fields:
        if field in revision.snapshot:
            setattr(instance, field, revision.snapshot[field])
    instance.updated_by = user
    instance.full_clean()
    instance.save()
    record_revision(instance=instance, user=user, operation="RESTORE")
    return instance


def audit(*, workspace, actor, action, instance=None, metadata=None, request_id=""):
    return AuditEvent.objects.create(
        workspace=workspace,
        actor=actor,
        action=action,
        entity_type=instance._meta.label_lower if instance is not None else "",
        entity_id=instance.pk if instance is not None else None,
        request_id=request_id,
        metadata=metadata or {},
    )
