import json
import re
from datetime import timedelta

from django.conf import settings
from django.db import transaction
from django.utils import timezone

from lessons.ai_gateway import ProviderError, run_text

from .models import AiSuggestion, AiUsageLog, Prompt, PromptBlock, Revision, TranslationUnit
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


def _parse_content(raw_text):
    clean = raw_text.strip()
    fence = chr(96) * 3
    if clean.startswith(fence):
        clean = clean.split("\n", 1)[-1]
        clean = clean.rsplit(fence, 1)[0].strip()
    try:
        parsed = json.loads(clean)
    except json.JSONDecodeError as exc:
        raise StudioAiError("invalid_provider_response", "AI returned an invalid prompt draft.") from exc
    content = str(parsed.get("content", "")).strip() if isinstance(parsed, dict) else ""
    if not content:
        raise StudioAiError("invalid_provider_response", "AI returned an empty prompt draft.")
    return content


def _check_rate(user):
    cutoff = timezone.now() - timedelta(minutes=1)
    if AiUsageLog.objects.filter(user=user, created_at__gte=cutoff).count() >= settings.STUDIO_AI_RATE_PER_MINUTE:
        raise StudioAiError("rate_limited", "AI request limit reached. Please wait before trying again.")


def preview_prompt_translation(
    *, prompt, user, content, target_language, text_model, scope,
    selection_start=None, selection_end=None, improve=False, prompt_improvement=False,
):
    from .ai_catalog import PROMPT_LANGUAGE_NAMES

    target = ((prompt.language or prompt.original_language) if prompt_improvement else target_language or "").strip().upper()
    if target not in Prompt.Language.values:
        raise StudioAiError("invalid_target_language", "Choose a target language from the list.")
    if scope not in {Prompt.TranslationScope.FULL, Prompt.TranslationScope.DIALOGUE, Prompt.TranslationScope.SELECTED}:
        raise StudioAiError("invalid_translation_scope", "Choose full prompt, dialogue, or selected text.")
    source = (content or "").strip()
    if not source:
        raise StudioAiError("empty_prompt", "Enter prompt text first.")
    _check_rate(user)

    before = after = ""
    supplied = source
    if scope == Prompt.TranslationScope.SELECTED:
        try:
            start = int(selection_start)
            end = int(selection_end)
        except (TypeError, ValueError) as exc:
            raise StudioAiError("selection_required", "Select text in the prompt first.") from exc
        if start < 0 or end <= start or end > len(source):
            raise StudioAiError("selection_required", "Select text in the prompt first.")
        before, supplied, after = source[:start], source[start:end], source[end:]

    task = (
        "Improve the supplied production prompt in its original language without translating it."
        if prompt_improvement else
        f"Improve the supplied translation in {PROMPT_LANGUAGE_NAMES[target]} ({target})."
        if improve else
        f"Translate the supplied text into {PROMPT_LANGUAGE_NAMES[target]} ({target})."
    )
    rules = [
        "Return JSON only with shape {\"content\": \"...\"}.",
        "Preserve meaning, names, formatting, tone, punctuation, and production terminology.",
        "Do not add explanations or information absent from the source.",
    ]
    if prompt_improvement:
        rules.extend([
            "Make the prompt precise, coherent and useful for media generation.",
            "Correct grammar, punctuation and awkward phrasing without changing factual constraints.",
            "Return the complete improved prompt in the original language.",
            "Never translate the prompt, even when project or prompt language metadata differs from the supplied text.",
        ])
    if scope == Prompt.TranslationScope.DIALOGUE:
        task = (
            f"Improve only the translated direct speech in {PROMPT_LANGUAGE_NAMES[target]} ({target}); keep all non-dialogue text unchanged."
            if improve else
            f"Translate only direct speech and dialogue into {PROMPT_LANGUAGE_NAMES[target]} ({target}); keep every other part unchanged."
        )
        rules.append("Return the complete prompt, including unchanged narrative text.")
        rules.append(
            f"In production directions, replace language labels such as 'in Polish' with "
            f"'in {PROMPT_LANGUAGE_NAMES[target]}'."
        )
    elif scope == Prompt.TranslationScope.FULL:
        rules.append("Return the complete translated prompt.")
    else:
        rules.append("Return only the translated selected fragment.")

    instruction = {"task": task, "rules": rules, "content": supplied}
    request_text = json.dumps(instruction, ensure_ascii=False)
    usage = AiUsageLog.objects.create(
        workspace=workspace_for(prompt), user=user, prompt=prompt,
        action="IMPROVE_PROMPT_PREVIEW" if prompt_improvement else ("IMPROVE_TRANSLATION" if improve else f"TRANSLATE_PREVIEW_{scope}"),
        model=text_model, status="STARTED", input_chars=len(request_text),
    )
    try:
        raw_text, selected_model = run_text(text_model, request_text)
        result = _parse_content(raw_text)
    except (ProviderError, StudioAiError) as exc:
        usage.status = "ERROR"
        usage.error_code = exc.code if isinstance(exc, StudioAiError) else "provider_error"
        usage.save(update_fields=["status", "error_code"])
        raise
    usage.model = selected_model
    usage.status = "SUCCESS"
    usage.output_chars = len(raw_text)
    usage.save(update_fields=["model", "status", "output_chars"])
    if scope == Prompt.TranslationScope.DIALOGUE:
        language_names = "|".join(re.escape(value) for value in PROMPT_LANGUAGE_NAMES.values())
        result = re.sub(
            rf"\bin\s+(?:{language_names})\b",
            f"in {PROMPT_LANGUAGE_NAMES[target]}",
            result,
            flags=re.IGNORECASE,
        )
    if scope == Prompt.TranslationScope.SELECTED:
        result = before + result + after
    return result, selected_model


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
    from .ai_catalog import default_prompt_template

    prompt_addition = default_prompt_template(prompt.prompt_type).content.strip()
    instruction = {
        "task": MODES[mode],
        "generation_model": prompt.ai_model.name,
        "default_prompt_addition": prompt_addition,
        "rules": [
            "Return JSON only with shape blocks containing id and content.",
            "Return only IDs supplied in editable_blocks.",
            "Do not add, rewrite, infer, or quote dialogue.",
            "Preserve factual scene and character constraints.",
            "The default prompt addition is fixed context. Do not rewrite or duplicate it in returned blocks.",
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
    AiSuggestion.objects.filter(prompt=prompt, status=AiSuggestion.Status.PENDING).update(
        status=AiSuggestion.Status.REJECTED, decided_by=user, decided_at=timezone.now(),
    )
    suggestion = AiSuggestion.objects.create(
        workspace=workspace_for(prompt), prompt=prompt, source_revision=source_revision,
        mode=mode, selected_block_ids=[str(block.id) for block in blocks],
        original_blocks=[{"id": str(block.id), "content": block.content} for block in blocks],
        suggested_blocks=suggested, raw_response=raw_text, model=selected_model, created_by=user,
    )
    audit(workspace=suggestion.workspace, actor=user, action="AI_SUGGESTION_CREATED", instance=prompt, metadata={"suggestionId": str(suggestion.id), "mode": mode, "model": selected_model})
    return suggestion


@transaction.atomic
def translate_prompt(*, prompt, user, target_language, text_model, scope):
    from .ai_catalog import PROMPT_LANGUAGE_NAMES

    target = (target_language or "").strip().upper()
    if target not in Prompt.Language.values:
        raise StudioAiError("invalid_target_language", "Choose a target language from the list.")
    if scope not in {Prompt.TranslationScope.FULL, Prompt.TranslationScope.DIALOGUE}:
        raise StudioAiError("invalid_translation_scope", "Choose full-text or dialogue-only translation.")
    _check_rate(user)
    all_blocks = list(prompt.blocks.select_related("source_dialogue").order_by("position", "id"))
    blocks = all_blocks if scope == Prompt.TranslationScope.FULL else [
        block for block in all_blocks if block.block_type == PromptBlock.Type.DIALOGUE_REFERENCE
    ]
    if not blocks:
        message = "This prompt has no dialogue to translate." if scope == Prompt.TranslationScope.DIALOGUE else "This prompt has no text to translate."
        raise StudioAiError("no_translatable_blocks", message)
    rows = [{"id": str(block.id), "type": block.block_type, "content": block.content} for block in blocks]
    instruction = {
        "task": f"Translate the supplied prompt blocks into {PROMPT_LANGUAGE_NAMES[target]} ({target}).",
        "rules": [
            "Return JSON only with shape blocks containing id and content.",
            "Return every supplied ID exactly once.",
            "Preserve meaning, names, formatting, speaker intent, tone, and punctuation.",
            "Do not add information that is absent from the source.",
        ],
        "blocks": rows,
    }
    if scope == Prompt.TranslationScope.DIALOGUE:
        instruction["rules"].append("The supplied blocks are direct speech. Translate only them; narrative is intentionally absent.")
    prompt_text = json.dumps(instruction, ensure_ascii=False)
    usage = AiUsageLog.objects.create(
        workspace=workspace_for(prompt), user=user, prompt=prompt, action=f"TRANSLATE_{scope}",
        model=text_model, status="STARTED", input_chars=len(prompt_text),
    )
    try:
        raw_text, selected_model = run_text(text_model, prompt_text)
        translated = _parse_blocks(raw_text, {str(block.id) for block in blocks})
    except (ProviderError, StudioAiError) as exc:
        usage.status = "ERROR"
        usage.error_code = exc.code if isinstance(exc, StudioAiError) else "provider_error"
        usage.save(update_fields=["status", "error_code"])
        raise

    translated_by_id = {row["id"]: row["content"] for row in translated}
    root_prompt = prompt.source_prompt or prompt
    translated_prompt = Prompt.objects.create(
        scene=prompt.scene,
        ai_model=prompt.ai_model,
        template=prompt.template,
        source_prompt=root_prompt,
        language=target,
        translation_scope=scope,
        prompt_type=prompt.prompt_type,
        title=f"{prompt.title or prompt.get_prompt_type_display()} [{target}]",
        status=Prompt.Status.DRAFT,
        position=prompt.scene.prompts.count(),
        created_by=user,
        updated_by=user,
    )
    for position, source_block in enumerate(all_blocks):
        content = translated_by_id.get(str(source_block.id), source_block.content)
        new_block = PromptBlock.objects.create(
            prompt=translated_prompt,
            block_type=source_block.block_type,
            content=content,
            source_dialogue=source_block.source_dialogue,
            position=position,
            created_by=user,
            updated_by=user,
        )
        record_revision(instance=new_block, user=user, operation="AI_TRANSLATION_CREATE")
        if source_block.source_dialogue_id and str(source_block.id) in translated_by_id:
            save_translation(
                dialogue_line=source_block.source_dialogue,
                user=user,
                target_language=target,
                translated_text=content,
                status=TranslationUnit.Status.DRAFT,
            )
    record_revision(instance=translated_prompt, user=user, operation="AI_TRANSLATION_CREATE")
    audit(
        workspace=workspace_for(prompt), actor=user, action="PROMPT_TRANSLATION_CREATED", instance=translated_prompt,
        metadata={
            "sourcePromptId": str(prompt.id), "targetLanguage": target,
            "scope": scope, "model": selected_model, "blockCount": len(translated),
        },
    )
    usage.model = selected_model
    usage.status = "SUCCESS"
    usage.output_chars = len(raw_text)
    usage.save(update_fields=["model", "status", "output_chars"])
    return translated_prompt, len(translated), selected_model


def translate_prompt_dialogue(*, prompt, user, target_language, text_model):
    return translate_prompt(
        prompt=prompt,
        user=user,
        target_language=target_language,
        text_model=text_model,
        scope=Prompt.TranslationScope.DIALOGUE,
    )


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


@transaction.atomic
def undo_suggestion(*, suggestion, user):
    suggestion = AiSuggestion.objects.select_for_update().select_related("prompt", "workspace").get(pk=suggestion.pk)
    if suggestion.status != AiSuggestion.Status.ACCEPTED:
        raise StudioAiError("not_undoable", "Only an applied improvement can be undone.")
    blocks = {
        str(block.id): block
        for block in suggestion.prompt.blocks.select_for_update()
    }
    suggested = {str(row.get("id")): str(row.get("content", "")).strip() for row in suggestion.suggested_blocks}
    originals = {str(row.get("id")): str(row.get("content", "")) for row in suggestion.original_blocks}
    for block_id, expected in suggested.items():
        block = blocks.get(block_id)
        if block is None or block.content != expected:
            raise StudioAiError("stale_undo", "The prompt changed after improvement and cannot be safely undone.")
    for block_id, content in originals.items():
        block = blocks.get(block_id)
        if block is None:
            continue
        block.content = content
        block.updated_by = user
        block.save(update_fields=["content", "updated_by", "updated_at"])
        record_revision(instance=block, user=user, operation="AI_UNDO")
    suggestion.prompt.updated_by = user
    suggestion.prompt.save(update_fields=["updated_by", "updated_at"])
    record_revision(instance=suggestion.prompt, user=user, operation="AI_UNDO")
    suggestion.status = AiSuggestion.Status.UNDONE
    suggestion.decided_by = user
    suggestion.decided_at = timezone.now()
    suggestion.save(update_fields=["status", "decided_by", "decided_at"])
    audit(
        workspace=suggestion.workspace, actor=user, action="AI_SUGGESTION_UNDONE",
        instance=suggestion.prompt, metadata={"suggestionId": str(suggestion.id)},
    )
    return suggestion
