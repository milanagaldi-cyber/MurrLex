import json

from django.http import FileResponse, JsonResponse
from django.shortcuts import get_object_or_404
from django.core.exceptions import ValidationError
from django.views.decorators.http import require_http_methods

from .models import AccessEvent, AdditionalGeneration, AiModelProfile, Asset, DialogueLine, GenerationOutput, Project, Prompt, PromptBlock, Revision, Scene
from .permissions import accessible_workspaces, has_capability
from .services import create_workspace, update_dialogue_line
from .storage import create_asset
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
def asset_download(request, asset_id):
    user = require_user(request)
    if user is None:
        return error("authentication_required", "Login is required.", 401)
    item = _accessible_asset(user, asset_id)
    AccessEvent.objects.create(workspace=item.workspace, actor=user, asset=item, action="DOWNLOAD")
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