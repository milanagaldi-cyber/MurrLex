import json
from datetime import timedelta

from django.conf import settings
from django.db import transaction
from django.utils import timezone

from lessons.ai_gateway import ProviderError, run_text

from .models import AiSuggestion, AiUsageLog, PromptBlock, Revision, TranslationUnit
from .revisions import audit, record_revision, workspace_for
from .services import save_translation


MODES = {
    "selected": "Improve only the selected blocks.",
    "non_dialogue": "Improve all supplied non-dialogue blocks.",
    "full": "Improve the complete supplied prompt while preserving meaning.",
    "cinematic": "Make the supplied prompt more cinematic and visually precise.",
    "precise": "Make the supplied prompt unambiguous and technically precise.",
    "shorter": "Make the supplied prompt shorter without losing required constraints.",
    "adapt_model": "Adapt the supplied prompt for the named generation model.",
    "fix_contradictions": "Resolve contradictions while preserving the intended scene.",
    "preserve_consistency": "Improve the prompt while preserving character and scene consistency.",
    "improve_translate_en": "Translate the supplied non-dialogue prompt content into natural production English and improve it for generation.",
}


class StudioAiError(Exception):
    def __init__(self, code, message):
        self.code = code
        super().__init__(message)


def _editable_blocks(prompt, selected_ids=None):
    blocks = prompt.blocks.exclude(block_type=PromptBlock.Type.DIALOGUE_REFERENCE)
    if selected_ids:
        blocks = blocks.filter(id__in=selected_ids)
    return list(blocks.order_by("position", "id"))


def _parse_blocks(raw_text, allowed_ids):
    clean = raw_text.strip()
    fence = chr(96) * 3
    if clean.startswith(fence):
        clean = clean.split("\n", 1)[-1]
        clean = clean.rsplit(fence, 1)[0].strip()
    try:
        parsed = json.loads(clean)
    except json.JSONDecodeError as exc:
        raise StudioAiError("invalid_provider_response", "AI returned an invalid structured suggestion.") from exc
    rows = parsed.get("blocks") if isinstance(parsed, dict) else None
    if not isinstance(rows, list):
        raise StudioAiError("invalid_provider_response", "AI suggestion does not contain blocks.")
    result = []
    seen = set()
    for row in rows:
        if not isinstance(row, dict):
            continue
        block_id = str(row.get("id", ""))
        content = str(row.get("content", "")).strip()
        if block_id in allowed_ids and block_id not in seen and content:
            result.append({"id": block_id, "content": content})
            seen.add(block_id)
    if not result:
        raise StudioAiError("invalid_provider_response", "AI returned no usable prompt blocks.")
    return result


def _parse_dialogue_translations(raw_text, allowed_ids):
    clean = raw_text.strip()
    fence = chr(96) * 3
    if clean.startswith(fence):
        clean = clean.split("\n", 1)[-1]
        clean = clean.rsplit(fence, 1)[0].strip()
    try:
        parsed = json.loads(clean)
    except json.JSONDecodeError as exc:
        raise StudioAiError("invalid_provider_response", "AI returned an invalid dialogue translation.") from exc
    rows = parsed.get("dialogue") if isinstance(parsed, dict) else None
    if not isinstance(rows, list):
        raise StudioAiError("invalid_provider_response", "AI translation does not contain dialogue rows.")
    result = []
    seen = set()
    for row in rows:
        if not isinstance(row, dict):
            continue
        block_id = str(row.get("id", ""))
        translation = str(row.get("translation", "")).strip()
        if block_id in allowed_ids and block_id not in seen and translation:
            result.append({"id": block_id, "translation": translation})
            seen.add(block_id)
    if not result:
        raise StudioAiError("invalid_provider_response", "AI returned no usable dialogue translations.")
    return result


def _check_rate(user):
    cutoff = timezone.now() - timedelta(minutes=1)
    if AiUsageLog.objects.filter(user=user, created_at__gte=cutoff).count() >= settings.STUDIO_AI_RATE_PER_MINUTE:
        raise StudioAiError("rate_limited", "AI request limit reached. Please wait before trying again.")


@transaction.atomic
def improve_prompt(*, prompt, user, mode, selected_block_ids, text_model):
    if mode not in MODES:
        raise StudioAiError("invalid_mode", "Unknown prompt improvement mode.")
    _check_rate(user)
    selected = [str(value) for value in selected_block_ids if value]
    blocks = _editable_blocks(prompt, selected if mode == "selected" else None)
    if mode == "selected" and not selected:
        raise StudioAiError("selection_required", "Select at least one non-dialogue block.")
    if not blocks:
        raise StudioAiError("no_editable_blocks", "This prompt has no editable non-dialogue blocks.")

    latest = Revision.objects.filter(entity_type=prompt._meta.label_lower, entity_id=prompt.id).order_by("-sequence").first()
    source_revision = latest or record_revision(instance=prompt, user=user, operation="AI_SOURCE")
    block_payload = [{"id": str(block.id), "type": block.block_type, "content": block.content} for block in blocks]
    instruction = {
        "task": MODES[mode],
        "generation_model": prompt.ai_model.name,
        "mandatory_template": prompt.template.content,
        "rules": [
            "Return JSON only with shape blocks containing id and content.",
            "Return only IDs supplied in editable_blocks.",
            "Do not add, rewrite, infer, or quote dialogue.",
            "Preserve factual scene and character constraints.",
            "The mandatory template is fixed context and must remain applicable to the improved result.",
        ],
        "editable_blocks": block_payload,
    }
    if mode == "improve_translate_en":
        instruction["rules"].append("Return every editable block in English even when the source is in another language.")
    prompt_text = json.dumps(instruction, ensure_ascii=False)
    usage = AiUsageLog.objects.create(
        workspace=workspace_for(prompt), user=user, prompt=prompt, action="IMPROVE_PROMPT",
        model=text_model, status="STARTED", input_chars=len(prompt_text),
    )
    try:
        raw_text, selected_model = run_text(text_model, prompt_text)
        suggested = _parse_blocks(raw_text, {str(block.id) for block in blocks})
    except (ProviderError, StudioAiError) as exc:
        usage.status = "ERROR"
        usage.error_code = exc.code if isinstance(exc, StudioAiError) else "provider_error"
        usage.save(update_fields=["status", "error_code"])
        raise
    usage.model = selected_model
    usage.status = "SUCCESS"
    usage.output_chars = len(raw_text)
    usage.save(update_fields=["model", "status", "output_chars"])
    suggestion = AiSuggestion.objects.create(
        workspace=workspace_for(prompt), prompt=prompt, source_revision=source_revision,
        mode=mode, selected_block_ids=[str(block.id) for block in blocks],
        suggested_blocks=suggested, raw_response=raw_text, model=selected_model, created_by=user,
    )
    audit(workspace=suggestion.workspace, actor=user, action="AI_SUGGESTION_CREATED", instance=prompt, metadata={"suggestionId": str(suggestion.id), "mode": mode, "model": selected_model})
    return suggestion


def translate_prompt_dialogue(*, prompt, user, target_language, text_model):
    target = (target_language or "").strip().lower()[:16]
    if not target:
        raise StudioAiError("target_language_required", "Enter a target language code for dialogue translation.")
    _check_rate(user)
    blocks = list(
        prompt.blocks.filter(block_type=PromptBlock.Type.DIALOGUE_REFERENCE)
        .select_related("source_dialogue")
        .order_by("position", "id")
    )
    if not blocks:
        raise StudioAiError("no_dialogue", "This prompt has no dialogue reference blocks to translate.")
    rows = [
        {
            "id": str(block.id),
            "speaker": block.source_dialogue.speaker if block.source_dialogue else "",
            "text": block.source_dialogue.text if block.source_dialogue else block.content,
        }
        for block in blocks
    ]
    instruction = {
        "task": f"Translate only the supplied direct speech into language code {target}.",
        "rules": [
            "Return JSON only with shape dialogue containing id and translation.",
            "Return every supplied ID exactly once.",
            "Do not translate, rewrite, or return any narrative prompt content.",
            "Preserve speaker intent, tone, names, and punctuation.",
        ],
        "dialogue": rows,
    }
    prompt_text = json.dumps(instruction, ensure_ascii=False)
    usage = AiUsageLog.objects.create(
        workspace=workspace_for(prompt), user=user, prompt=prompt, action="TRANSLATE_DIALOGUE",
        model=text_model, status="STARTED", input_chars=len(prompt_text),
    )
    try:
        raw_text, selected_model = run_text(text_model, prompt_text)
        translated = _parse_dialogue_translations(raw_text, {str(block.id) for block in blocks})
    except (ProviderError, StudioAiError) as exc:
        usage.status = "ERROR"
        usage.error_code = exc.code if isinstance(exc, StudioAiError) else "provider_error"
        usage.save(update_fields=["status", "error_code"])
        raise

    by_id = {str(block.id): block for block in blocks}
    with transaction.atomic():
        for row in translated:
            block = by_id[row["id"]]
            block.translated_content = row["translation"]
            block.translation_language = target
            block.translation_model = selected_model
            block.updated_by = user
            block.save(update_fields=[
                "translated_content", "translation_language", "translation_model", "updated_by", "updated_at",
            ])
            record_revision(instance=block, user=user, operation="AI_DIALOGUE_TRANSLATION")
            if block.source_dialogue_id:
                save_translation(
                    dialogue_line=block.source_dialogue,
                    user=user,
                    target_language=target,
                    translated_text=row["translation"],
                    status=TranslationUnit.Status.DRAFT,
                )
        audit(
            workspace=workspace_for(prompt), actor=user, action="PROMPT_DIALOGUE_TRANSLATED", instance=prompt,
            metadata={"targetLanguage": target, "model": selected_model, "blockCount": len(translated)},
        )
    usage.model = selected_model
    usage.status = "SUCCESS"
    usage.output_chars = len(raw_text)
    usage.save(update_fields=["model", "status", "output_chars"])
    return len(translated), selected_model


@transaction.atomic
def accept_suggestion(*, suggestion, user):
    suggestion = AiSuggestion.objects.select_for_update().select_related("prompt", "workspace").get(pk=suggestion.pk)
    if suggestion.status != AiSuggestion.Status.PENDING:
        raise StudioAiError("already_decided", "This suggestion has already been decided.")
    latest = Revision.objects.filter(entity_type=suggestion.prompt._meta.label_lower, entity_id=suggestion.prompt_id).order_by("-sequence").first()
    if latest and latest.id != suggestion.source_revision_id:
        raise StudioAiError("stale_suggestion", "The prompt changed after this suggestion was created.")
    editable = {
        str(block.id): block
        for block in suggestion.prompt.blocks.exclude(block_type=PromptBlock.Type.DIALOGUE_REFERENCE).select_for_update()
    }
    for row in suggestion.suggested_blocks:
        block = editable.get(str(row.get("id", "")))
        if block is None:
            continue
        block.content = str(row.get("content", "")).strip()
        block.updated_by = user
        block.save(update_fields=["content", "updated_by", "updated_at"])
        record_revision(instance=block, user=user, operation="AI_ACCEPT")
    suggestion.prompt.needs_review = False
    suggestion.prompt.updated_by = user
    suggestion.prompt.save(update_fields=["needs_review", "updated_by", "updated_at"])
    record_revision(instance=suggestion.prompt, user=user, operation="AI_ACCEPT")
    suggestion.status = AiSuggestion.Status.ACCEPTED
    suggestion.decided_by = user
    suggestion.decided_at = timezone.now()
    suggestion.save(update_fields=["status", "decided_by", "decided_at"])
    audit(workspace=suggestion.workspace, actor=user, action="AI_SUGGESTION_ACCEPTED", instance=suggestion.prompt, metadata={"suggestionId": str(suggestion.id)})
    return suggestion


def reject_suggestion(*, suggestion, user):
    if suggestion.status != AiSuggestion.Status.PENDING:
        raise StudioAiError("already_decided", "This suggestion has already been decided.")
    suggestion.status = AiSuggestion.Status.REJECTED
    suggestion.decided_by = user
    suggestion.decided_at = timezone.now()
    suggestion.save(update_fields=["status", "decided_by", "decided_at"])
    audit(workspace=suggestion.workspace, actor=user, action="AI_SUGGESTION_REJECTED", instance=suggestion.prompt, metadata={"suggestionId": str(suggestion.id)})
    return suggestion
