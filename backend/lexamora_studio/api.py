import json

from django.http import FileResponse, JsonResponse
from django.shortcuts import get_object_or_404
from django.core.exceptions import ValidationError
from django.views.decorators.http import require_http_methods

from make_mistake_backend.observability import current_request_id

from lessons.ai_gateway import ProviderError
from lessons.provider_credentials import user_has_ai_access

from .models import AccessEvent, AdditionalGeneration, AiModelProfile, AiSuggestion, Asset, DialogueLine, DocxImport, Episode, ExportJob, GenerationOutput, Project, Prompt, PromptBlock, Revision, Scene, SubtitleLine, SubtitleTrack, TranslationUnit
from .permissions import accessible_workspaces, has_capability
from .services import bulk_replace_subtitle_lines, create_workspace, reorder_subtitle_lines, save_translation, update_dialogue_line
from .storage import create_asset
from .exports import ExportError, generate_export
from .docx_imports import accept_docx_import, parse_docx
from .ai import StudioAiError, accept_suggestion, improve_prompt, reject_suggestion
from .revisions import VERSIONED_MODELS, audit, record_revision, restore_revision, revision_diff


def error(code, message, status=400, fields=None):
    return JsonResponse({"error": {"code": code, "message": message, "fields": fields or {}}}, status=status)


def payload(request):
    try:
        value = json.loads(request.body or b"{}")
    except (UnicodeDecodeError, json.JSONDecodeError):
        return None
    return value if isinstance(value, dict) else None


def require_user(request):
    return request.user if request.user.is_authenticated else None


def workspace_json(item):
    return {"id": str(item.id), "name": item.name, "slug": item.slug, "description": item.description, "updatedAt": item.updated_at.isoformat()}


def project_json(item):
    return {
        "id": str(item.id),
        "workspaceId": str(item.workspace_id),
        "type": item.project_type,
        "title": item.title,
        "concept": item.concept,
        "originalLanguage": item.original_language,
        "translationLanguages": item.translation_languages,
        "status": item.status,
        "updatedAt": item.updated_at.isoformat(),
    }


@require_http_methods(["GET", "POST"])
def workspaces(request):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    if request.method == "GET":
        items = accessible_workspaces(user)[:50]
        return JsonResponse({"results": [workspace_json(item) for item in items], "next": None})
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    name = str(data.get("name", "")).strip()
    slug = str(data.get("slug", "")).strip()
    if not name or not slug:
        return error("validation_error", "Name and slug are required.", fields={"name": "Required", "slug": "Required"})
    from .models import Workspace
    if Workspace.all_objects.filter(slug=slug).exists():
        return error("validation_error", "Slug is already in use.", fields={"slug": "Already in use"})
    item = create_workspace(user=user, name=name, slug=slug, description=str(data.get("description", "")).strip())
    return JsonResponse(workspace_json(item), status=201)


@require_http_methods(["GET", "POST"])
def projects(request):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    if request.method == "GET":
        items = Project.objects.select_related("workspace").filter(workspace__in=accessible_workspaces(user))[:50]
        return JsonResponse({"results": [project_json(item) for item in items], "next": None})
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    workspace = get_object_or_404(accessible_workspaces(user), id=data.get("workspaceId"))
    if not has_capability(user, workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    title = str(data.get("title", "")).strip()
    project_type = str(data.get("type", ""))
    if not title or project_type not in Project.Type.values:
        return error("validation_error", "Valid title and type are required.")
    item = Project.objects.create(
        workspace=workspace,
        project_type=project_type,
        title=title,
        concept=str(data.get("concept", "")).strip(),
        original_language=str(data.get("originalLanguage", "")).strip(),
        translation_languages=data.get("translationLanguages", []),
        created_by=user,
        updated_by=user,
    )
    return JsonResponse(project_json(item), status=201)


@require_http_methods(["GET", "PATCH"])
def project_detail(request, project_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    item = get_object_or_404(
        Project.objects.select_related("workspace").filter(workspace__in=accessible_workspaces(user)),
        id=project_id,
    )
    if request.method == "GET":
        return JsonResponse(project_json(item))
    if not has_capability(user, item.workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    for api_name, field_name in {
        "title": "title",
        "concept": "concept",
        "originalLanguage": "original_language",
        "translationLanguages": "translation_languages",
        "status": "status",
    }.items():
        if api_name in data:
            setattr(item, field_name, data[api_name])
    item.updated_by = user
    item.full_clean()
    item.save()
    record_revision(instance=item, user=user, operation="UPDATE")
    return JsonResponse(project_json(item))


def scene_workspace(scene):
    return scene.episode.project.workspace


@require_http_methods(["GET", "PATCH"])
def scene_detail(request, scene_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=scene_id,
    )
    if request.method == "GET":
        return JsonResponse({
            "id": str(scene.id), "number": scene.number, "title": scene.title,
            "hook": scene.hook, "description": scene.description, "location": scene.location,
            "actions": scene.actions, "performanceNotes": scene.performance_notes,
            "status": scene.status, "position": scene.position,
        })
    if not has_capability(user, scene_workspace(scene), "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    for api_name, field_name in {
        "title": "title", "hook": "hook", "description": "description", "location": "location",
        "actions": "actions", "performanceNotes": "performance_notes", "status": "status",
    }.items():
        if api_name in data:
            setattr(scene, field_name, data[api_name])
    scene.updated_by = user
    scene.full_clean()
    scene.save()
    record_revision(instance=scene, user=user, operation="UPDATE")
    return JsonResponse({"id": str(scene.id), "status": scene.status, "updatedAt": scene.updated_at.isoformat()})


@require_http_methods(["GET", "POST"])
def scene_dialogue(request, scene_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=scene_id,
    )
    if request.method == "GET":
        return JsonResponse({"results": [
            {"id": str(line.id), "speaker": line.speaker, "text": line.text, "language": line.language, "delivery": line.delivery, "position": line.position, "status": line.status}
            for line in scene.dialogue_lines.all()
        ]})
    if not has_capability(user, scene_workspace(scene), "edit"):
        return error("permission_denied", "Only editors can change source dialogue.", 403)
    data = payload(request)
    if data is None or not str(data.get("text", "")).strip():
        return error("validation_error", "Dialogue text is required.")
    line = DialogueLine.objects.create(
        scene=scene, speaker=str(data.get("speaker", "")).strip(), text=str(data["text"]).strip(),
        language=str(data.get("language", "")).strip(), delivery=str(data.get("delivery", "")).strip(),
        position=scene.dialogue_lines.count(), created_by=user, updated_by=user,
    )
    return JsonResponse({"id": str(line.id), "text": line.text, "position": line.position}, status=201)


@require_http_methods(["PATCH"])
def dialogue_detail(request, line_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    line = get_object_or_404(
        DialogueLine.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=line_id,
    )
    if not has_capability(user, scene_workspace(line.scene), "edit"):
        return error("permission_denied", "Only editors can change source dialogue.", 403)
    data = payload(request)
    if data is None or not str(data.get("text", "")).strip():
        return error("validation_error", "Dialogue text is required.")
    line = update_dialogue_line(
        line=line, user=user, text=str(data["text"]).strip(),
        speaker=data.get("speaker"), delivery=data.get("delivery"),
    )
    return JsonResponse({"id": str(line.id), "text": line.text, "updatedAt": line.updated_at.isoformat()})


@require_http_methods(["GET", "POST"])
def scene_prompts(request, scene_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=scene_id,
    )
    if request.method == "GET":
        return JsonResponse({"results": [
            {"id": str(item.id), "model": item.ai_model.name, "type": item.prompt_type, "status": item.status, "needsReview": item.needs_review}
            for item in scene.prompts.select_related("ai_model")
        ]})
    if not has_capability(user, scene_workspace(scene), "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    model = get_object_or_404(AiModelProfile.objects.filter(is_active=True), id=data.get("aiModelId"))
    prompt_type = str(data.get("type", ""))
    if prompt_type not in Prompt.Type.values:
        return error("validation_error", "A valid prompt type is required.")
    prompt = Prompt.objects.create(
        scene=scene, ai_model=model, prompt_type=prompt_type, title=str(data.get("title", "")).strip(),
        position=scene.prompts.count(), created_by=user, updated_by=user,
    )
    for position, block in enumerate(data.get("blocks", [])):
        if isinstance(block, dict) and str(block.get("content", "")).strip() and block.get("type") in PromptBlock.Type.values:
            PromptBlock.objects.create(
                prompt=prompt, block_type=block["type"], content=str(block["content"]).strip(),
                position=position, created_by=user, updated_by=user,
            )
    record_revision(instance=prompt, user=user, operation="CREATE")
    return JsonResponse({"id": str(prompt.id), "model": model.name, "type": prompt.prompt_type}, status=201)
def docx_import_json(item):
    return {
        "id": str(item.id), "workspaceId": str(item.workspace_id), "status": item.status,
        "sourceAssetId": str(item.source_asset_id), "projectId": str(item.project_id) if item.project_id else None,
        "parsedData": item.parsed_data, "warnings": item.warnings, "createdAt": item.created_at.isoformat(),
    }


@require_http_methods(["POST"])
def docx_imports(request):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    workspace = get_object_or_404(accessible_workspaces(user), id=request.POST.get("workspaceId"))
    if not has_capability(user, workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    uploaded = request.FILES.get("file")
    if uploaded is None:
        return error("validation_error", "A DOCX file is required.", fields={"file": "Required"})
    try:
        parsed_data, warnings = parse_docx(uploaded)
        source_asset = create_asset(user=user, workspace=workspace, uploaded=uploaded, kind=Asset.Kind.SOURCE_DOCUMENT)
    except ValidationError as exc:
        return error("validation_error", "; ".join(exc.messages), fields={"file": exc.messages})
    item = DocxImport.objects.create(workspace=workspace, source_asset=source_asset, parsed_data=parsed_data, warnings=warnings, requested_by=user)
    return JsonResponse(docx_import_json(item), status=201)


@require_http_methods(["GET"])
def docx_import_detail(request, import_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    item = get_object_or_404(DocxImport.objects.filter(workspace__in=accessible_workspaces(user)), id=import_id)
    return JsonResponse(docx_import_json(item))


@require_http_methods(["POST"])
def docx_import_accept(request, import_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    item = get_object_or_404(DocxImport.objects.select_related("workspace").filter(workspace__in=accessible_workspaces(user)), id=import_id)
    if not has_capability(user, item.workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    project = accept_docx_import(draft=item, user=user)
    item.refresh_from_db()
    result = docx_import_json(item)
    result["project"] = project_json(project)
    return JsonResponse(result)

def asset_json(item):
    return {
        "id": str(item.id), "workspaceId": str(item.workspace_id), "kind": item.kind,
        "filename": item.original_filename, "contentType": item.content_type,
        "sizeBytes": item.size_bytes, "checksumSha256": item.checksum_sha256,
        "width": item.width, "height": item.height, "hasThumbnail": bool(item.thumbnail),
    }


@require_http_methods(["POST"])
def assets(request):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    workspace = get_object_or_404(accessible_workspaces(user), id=request.POST.get("workspaceId"))
    if not has_capability(user, workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    uploaded = request.FILES.get("file")
    if uploaded is None:
        return error("validation_error", "A file is required.", fields={"file": "Required"})
    kind = request.POST.get("kind", "")
    if kind not in Asset.Kind.values:
        return error("validation_error", "A valid asset kind is required.")
    project = None
    scene = None
    if request.POST.get("projectId"):
        project = get_object_or_404(Project.objects.filter(workspace=workspace), id=request.POST["projectId"])
    if request.POST.get("sceneId"):
        scene = get_object_or_404(Scene.objects.filter(episode__project__workspace=workspace), id=request.POST["sceneId"])
    try:
        item = create_asset(user=user, workspace=workspace, uploaded=uploaded, kind=kind, project=project, scene=scene)
    except ValidationError as exc:
        return error("validation_error", "; ".join(exc.messages), fields={"file": exc.messages})
    return JsonResponse(asset_json(item), status=201)


def _accessible_asset(user, asset_id):
    return get_object_or_404(
        Asset.objects.filter(workspace__in=accessible_workspaces(user)),
        id=asset_id,
    )


@require_http_methods(["GET"])
def asset_detail(request, asset_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    return JsonResponse(asset_json(_accessible_asset(user, asset_id)))


@require_http_methods(["GET"])
def asset_view(request, asset_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    item = _accessible_asset(user, asset_id)
    AccessEvent.objects.create(workspace=item.workspace, actor=user, asset=item, action="VIEW", request_id=current_request_id())
    audit(workspace=item.workspace, actor=user, action="ASSET_VIEW", instance=item, metadata={"filename": item.original_filename})
    return FileResponse(item.file.open("rb"), as_attachment=False, filename=item.original_filename, content_type=item.content_type)


@require_http_methods(["GET"])
def asset_download(request, asset_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    item = _accessible_asset(user, asset_id)
    AccessEvent.objects.create(workspace=item.workspace, actor=user, asset=item, action="DOWNLOAD", request_id=current_request_id())
    audit(workspace=item.workspace, actor=user, action="ASSET_DOWNLOAD", instance=item, metadata={"filename": item.original_filename})
    return FileResponse(item.file.open("rb"), as_attachment=True, filename=item.original_filename, content_type=item.content_type)


@require_http_methods(["GET"])
def asset_thumbnail(request, asset_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    item = _accessible_asset(user, asset_id)
    if not item.thumbnail:
        return error("thumbnail_unavailable", "This asset has no thumbnail.", 404)
    return FileResponse(item.thumbnail.open("rb"), content_type="image/jpeg")


@require_http_methods(["GET", "POST"])
def scene_generations(request, scene_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=scene_id,
    )
    if request.method == "GET":
        return JsonResponse({"results": [
            {"id": str(item.id), "reason": item.reason, "prompt": item.prompt, "status": item.status, "outputs": item.outputs.count()}
            for item in scene.additional_generations.prefetch_related("outputs")
        ]})
    if not has_capability(user, scene_workspace(scene), "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    data = payload(request)
    if data is None or not str(data.get("reason", "")).strip() or not str(data.get("prompt", "")).strip():
        return error("validation_error", "Reason and prompt are required.")
    source_asset = None
    if data.get("sourceAssetId"):
        source_asset = get_object_or_404(Asset.objects.filter(workspace=scene_workspace(scene)), id=data["sourceAssetId"])
    item = AdditionalGeneration.objects.create(
        scene=scene, reason=str(data["reason"]).strip(), prompt=str(data["prompt"]).strip(),
        source_asset=source_asset, position=scene.additional_generations.count(),
        created_by=user, updated_by=user,
    )
    return JsonResponse({"id": str(item.id), "status": item.status}, status=201)


@require_http_methods(["POST"])
def generation_outputs(request, generation_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    generation = get_object_or_404(
        AdditionalGeneration.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=generation_id,
    )
    workspace = generation.scene.episode.project.workspace
    if not has_capability(user, workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    asset = get_object_or_404(Asset.objects.filter(workspace=workspace), id=data.get("assetId"))
    item = GenerationOutput.objects.create(
        generation=generation, asset=asset, model_metadata=data.get("modelMetadata", {}),
        position=generation.outputs.count(), created_by=user, updated_by=user,
    )
    return JsonResponse({"id": str(item.id), "isFinal": item.is_final}, status=201)


@require_http_methods(["POST"])
def generation_output_final(request, output_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    item = get_object_or_404(
        GenerationOutput.objects.select_related("generation__scene__episode__project__workspace").filter(
            generation__scene__episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=output_id,
    )
    workspace = item.generation.scene.episode.project.workspace
    if not has_capability(user, workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    item.generation.outputs.update(is_final=False)
    item.is_final = True
    item.updated_by = user
    item.save(update_fields=["is_final", "updated_by", "updated_at"])
    return JsonResponse({"id": str(item.id), "isFinal": True})

def _entity_key(entity_type):
    key = f"lexamora_studio.{entity_type.lower()}"
    return key if key in VERSIONED_MODELS else None


@require_http_methods(["GET"])
def entity_revisions(request, entity_type, entity_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    key = _entity_key(entity_type)
    if key is None:
        return error("unknown_entity_type", "Unknown versioned entity type.", 404)
    revisions = Revision.objects.filter(
        workspace__in=accessible_workspaces(user), entity_type=key, entity_id=entity_id
    ).select_related("author").order_by("-sequence")
    return JsonResponse({"results": [
        {
            "id": str(item.id), "sequence": item.sequence, "operation": item.operation,
            "changedFields": item.changed_fields, "author": item.author.get_username(),
            "createdAt": item.created_at.isoformat(),
        }
        for item in revisions
    ]})


@require_http_methods(["GET"])
def revision_compare(request, entity_type, entity_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    key = _entity_key(entity_type)
    if key is None:
        return error("unknown_entity_type", "Unknown versioned entity type.", 404)
    scoped = Revision.objects.filter(
        workspace__in=accessible_workspaces(user), entity_type=key, entity_id=entity_id
    )
    older = get_object_or_404(scoped, id=request.GET.get("from"))
    newer = get_object_or_404(scoped, id=request.GET.get("to"))
    return JsonResponse({"from": older.sequence, "to": newer.sequence, "fields": revision_diff(older, newer)})


@require_http_methods(["POST"])
def revision_restore(request, entity_type, entity_id, revision_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    key = _entity_key(entity_type)
    if key is None:
        return error("unknown_entity_type", "Unknown versioned entity type.", 404)
    revision = get_object_or_404(
        Revision.objects.select_related("workspace").filter(workspace__in=accessible_workspaces(user)),
        id=revision_id, entity_type=key, entity_id=entity_id,
    )
    if not has_capability(user, revision.workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    instance = restore_revision(revision=revision, user=user)
    audit(workspace=revision.workspace, actor=user, action="REVISION_RESTORE", instance=instance, metadata={"revisionId": str(revision.id)})
    return JsonResponse({"id": str(instance.pk), "restoredRevision": revision.sequence})

def suggestion_json(item):
    current = {str(block.id): block.content for block in item.prompt.blocks.all()}
    return {
        "id": str(item.id), "promptId": str(item.prompt_id), "status": item.status,
        "mode": item.mode, "model": item.model, "sourceRevisionId": str(item.source_revision_id),
        "blocks": [
            {"id": row["id"], "before": current.get(str(row["id"]), ""), "suggested": row["content"]}
            for row in item.suggested_blocks
        ],
        "createdAt": item.created_at.isoformat(),
    }


def _ai_error_response(exc):
    if isinstance(exc, ProviderError):
        return error("provider_error", str(exc), 502)
    status = 429 if exc.code == "rate_limited" else 409 if exc.code in {"stale_suggestion", "already_decided"} else 400
    return error(exc.code, str(exc), status)


@require_http_methods(["GET", "POST"])
def prompt_improve(request, prompt_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model").prefetch_related("blocks").filter(
            scene__episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    if request.method == "GET":
        items = prompt.ai_suggestions.select_related("source_revision").prefetch_related("prompt__blocks")[:30]
        return JsonResponse({"results": [suggestion_json(item) for item in items]})
    if not has_capability(user, workspace, "use_ai"):
        return error("permission_denied", "AI capability is required.", 403)
    if not user_has_ai_access(user):
        return error("ai_access_required", "Server AI access is not enabled for this account.", 403)
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    try:
        suggestion = improve_prompt(
            prompt=prompt, user=user, mode=str(data.get("mode", "non_dialogue")),
            selected_block_ids=data.get("selectedBlockIds", []),
            text_model=str(data.get("textModel", "gpt-5.4-mini")),
        )
    except (StudioAiError, ProviderError) as exc:
        return _ai_error_response(exc)
    return JsonResponse(suggestion_json(suggestion), status=201)


def _suggestion_for_user(user, suggestion_id):
    return get_object_or_404(
        AiSuggestion.objects.select_related("workspace", "prompt").prefetch_related("prompt__blocks").filter(
            workspace__in=accessible_workspaces(user)
        ),
        id=suggestion_id,
    )


@require_http_methods(["POST"])
def suggestion_accept(request, suggestion_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    suggestion = _suggestion_for_user(user, suggestion_id)
    if not has_capability(user, suggestion.workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    try:
        suggestion = accept_suggestion(suggestion=suggestion, user=user)
    except StudioAiError as exc:
        return _ai_error_response(exc)
    return JsonResponse(suggestion_json(suggestion))


@require_http_methods(["POST"])
def suggestion_reject(request, suggestion_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    suggestion = _suggestion_for_user(user, suggestion_id)
    if not has_capability(user, suggestion.workspace, "edit"):
        return error("permission_denied", "Edit permission is required.", 403)
    try:
        suggestion = reject_suggestion(suggestion=suggestion, user=user)
    except StudioAiError as exc:
        return _ai_error_response(exc)
    return JsonResponse(suggestion_json(suggestion))

def subtitle_track_json(track):
    return {
        "id": str(track.id), "episodeId": str(track.episode_id), "language": track.language,
        "kind": track.kind, "status": track.status, "lineCount": track.lines.count(),
    }


@require_http_methods(["GET", "POST"])
def episode_subtitle_tracks(request, episode_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    episode = get_object_or_404(
        Episode.objects.select_related("project__workspace").filter(project__workspace__in=accessible_workspaces(user)),
        id=episode_id,
    )
    workspace = episode.project.workspace
    if request.method == "GET":
        return JsonResponse({"results": [subtitle_track_json(track) for track in episode.subtitle_tracks.all()]})
    if not has_capability(user, workspace, "translate"):
        return error("permission_denied", "Translation permission is required.", 403)
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    language = str(data.get("language", "")).strip().lower()[:16]
    kind = str(data.get("kind", SubtitleTrack.Kind.WORKING))
    if not language or kind not in SubtitleTrack.Kind.values:
        return error("validation_error", "Valid language and track kind are required.")
    track, created = SubtitleTrack.objects.get_or_create(
        episode=episode, language=language, kind=kind,
        defaults={"status": SubtitleTrack.Status.DRAFT, "created_by": user, "updated_by": user},
    )
    return JsonResponse(subtitle_track_json(track), status=201 if created else 200)


def _subtitle_track_for_user(user, track_id):
    return get_object_or_404(
        SubtitleTrack.objects.select_related("episode__project__workspace").filter(
            episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=track_id,
    )


@require_http_methods(["GET", "POST"])
def subtitle_lines(request, track_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    track = _subtitle_track_for_user(user, track_id)
    if request.method == "GET":
        return JsonResponse({"results": [
            {"id": str(line.id), "position": line.position, "text": line.text, "startMs": line.start_ms, "endMs": line.end_ms}
            for line in track.lines.all()
        ]})
    if not has_capability(user, track.episode.project.workspace, "translate"):
        return error("permission_denied", "Translation permission is required.", 403)
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    lines = data.get("lines")
    if isinstance(lines, str):
        lines = lines.splitlines()
    if not isinstance(lines, list):
        return error("validation_error", "Lines must be a list or multiline string.")
    result = bulk_replace_subtitle_lines(track=track, user=user, texts=lines)
    return JsonResponse({"results": [{"id": str(line.id), "position": line.position, "text": line.text} for line in result]})


@require_http_methods(["POST"])
def subtitle_lines_reorder(request, track_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    track = _subtitle_track_for_user(user, track_id)
    if not has_capability(user, track.episode.project.workspace, "translate"):
        return error("permission_denied", "Translation permission is required.", 403)
    data = payload(request)
    if data is None or not isinstance(data.get("lineIds"), list):
        return error("validation_error", "lineIds must be a list.")
    try:
        lines = reorder_subtitle_lines(track=track, user=user, ordered_ids=data["lineIds"])
    except ValueError as exc:
        return error("validation_error", str(exc))
    return JsonResponse({"results": [{"id": str(line.id), "position": line.position} for line in lines]})


def translation_json(line, unit):
    return {
        "dialogueLineId": str(line.id), "speaker": line.speaker, "source": line.text,
        "targetLanguage": unit.target_language if unit else "",
        "translation": unit.translated_text if unit else "",
        "status": unit.status if unit else TranslationUnit.Status.DRAFT,
        "translationId": str(unit.id) if unit else None,
    }


@require_http_methods(["GET"])
def project_translations(request, project_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    project = get_object_or_404(Project.objects.filter(workspace__in=accessible_workspaces(user)), id=project_id)
    language = request.GET.get("targetLanguage", "").strip().lower()[:16]
    if not language:
        return error("validation_error", "targetLanguage is required.")
    lines = DialogueLine.objects.filter(scene__episode__project=project).select_related("scene").prefetch_related("translations")
    results = []
    for line in lines:
        unit = next((item for item in line.translations.all() if item.target_language == language), None)
        results.append(translation_json(line, unit))
    return JsonResponse({"projectId": str(project.id), "targetLanguage": language, "results": results})


@require_http_methods(["PUT", "PATCH"])
def dialogue_translation(request, line_id, target_language):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    line = get_object_or_404(
        DialogueLine.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__workspace__in=accessible_workspaces(user)
        ),
        id=line_id,
    )
    workspace = line.scene.episode.project.workspace
    if not has_capability(user, workspace, "translate"):
        return error("permission_denied", "Translation permission is required.", 403)
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    status = str(data.get("status", TranslationUnit.Status.DRAFT))
    if status not in TranslationUnit.Status.values or status == TranslationUnit.Status.STALE:
        return error("validation_error", "A valid editable translation status is required.")
    unit = save_translation(
        dialogue_line=line, user=user, target_language=target_language.lower()[:16],
        translated_text=str(data.get("translation", "")).strip(), status=status,
    )
    return JsonResponse(translation_json(line, unit))


def export_json(item):
    return {
        "id": str(item.id), "projectId": str(item.project_id),
        "episodeId": str(item.episode_id) if item.episode_id else None,
        "sections": item.sections, "status": item.status,
        "assetId": str(item.output_asset_id) if item.output_asset_id else None,
        "error": item.error_message if item.status == ExportJob.Status.ERROR else "",
        "createdAt": item.created_at.isoformat(),
        "completedAt": item.completed_at.isoformat() if item.completed_at else None,
    }


@require_http_methods(["POST"])
def project_export_pdf(request, project_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    project = get_object_or_404(
        Project.objects.prefetch_related(
            "characters", "episodes__scenes__dialogue_lines", "episodes__scenes__prompts__blocks",
            "episodes__scenes__additional_generations__outputs__asset", "episodes__subtitle_tracks__lines",
        ).filter(workspace__in=accessible_workspaces(user)),
        id=project_id,
    )
    if not has_capability(user, project.workspace, "export"):
        return error("permission_denied", "Export capability is required.", 403)
    data = payload(request)
    if data is None:
        return error("invalid_json", "A JSON object is required.")
    episode = None
    if data.get("episodeId"):
        episode = get_object_or_404(Episode.objects.filter(project=project), id=data["episodeId"])
    sections = data.get("sections", [])
    if not isinstance(sections, list):
        return error("validation_error", "sections must be a list.")
    try:
        job = generate_export(project=project, episode=episode, sections=sections, user=user)
    except ExportError as exc:
        return error("export_error", str(exc), 429 if "limit" in str(exc).lower() else 400)
    except Exception:
        return error("export_failed", "PDF generation failed.", 500)
    return JsonResponse(export_json(job), status=201)


def _export_for_user(user, export_id):
    return get_object_or_404(
        ExportJob.objects.select_related("workspace", "output_asset").filter(
            workspace__in=accessible_workspaces(user)
        ),
        id=export_id,
    )


@require_http_methods(["GET"])
def export_detail(request, export_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    return JsonResponse(export_json(_export_for_user(user, export_id)))


@require_http_methods(["GET"])
def export_download(request, export_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    job = _export_for_user(user, export_id)
    if job.status != ExportJob.Status.SUCCESS or job.output_asset is None:
        return error("export_unavailable", "Export is not ready for download.", 409)
    asset = job.output_asset
    AccessEvent.objects.create(workspace=job.workspace, actor=user, asset=asset, action="DOWNLOAD", request_id=current_request_id())
    audit(workspace=job.workspace, actor=user, action="EXPORT_DOWNLOAD", instance=asset, metadata={"exportId": str(job.id)})
    return FileResponse(asset.file.open("rb"), as_attachment=True, filename=asset.original_filename, content_type="application/pdf")