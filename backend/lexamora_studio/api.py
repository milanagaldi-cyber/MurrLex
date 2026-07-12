import json

from django.http import JsonResponse
from django.shortcuts import get_object_or_404
from django.views.decorators.http import require_http_methods

from .models import Project
from .permissions import accessible_workspaces, has_capability
from .services import create_workspace


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
    return JsonResponse(project_json(item))
