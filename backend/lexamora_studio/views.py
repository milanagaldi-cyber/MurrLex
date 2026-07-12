from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.shortcuts import get_object_or_404, redirect, render
from django.utils.text import slugify

from lessons.ai_gateway import ProviderError
from lessons.provider_credentials import user_has_ai_access

from .ai import StudioAiError, accept_suggestion, improve_prompt, reject_suggestion
from .models import AiSuggestion, Project, Prompt, Scene, Workspace
from .permissions import accessible_workspaces, has_capability
from .services import create_workspace


@login_required
def dashboard(request):
    return render(request, "studio/dashboard.html", {"workspaces": accessible_workspaces(request.user).prefetch_related("projects")})


@login_required
def workspace_create(request):
    if request.method == "POST":
        name = request.POST.get("name", "").strip()
        if name:
            base_slug = slugify(name)[:160] or "workspace"
            slug = base_slug
            suffix = 2
            while Workspace.all_objects.filter(slug=slug).exists():
                slug = f"{base_slug}-{suffix}"
                suffix += 1
            workspace = create_workspace(user=request.user, name=name, slug=slug)
            return redirect("studio:workspace_detail", workspace_id=workspace.id)
    return render(request, "studio/workspace_form.html")


@login_required
def workspace_detail(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    return render(request, "studio/workspace_detail.html", {"workspace": workspace, "can_edit": has_capability(request.user, workspace, "edit")})


@login_required
def project_detail(request, project_id):
    project = get_object_or_404(
        Project.objects.select_related("workspace").filter(workspace__in=accessible_workspaces(request.user)),
        id=project_id,
    )
    return render(request, "studio/project_detail.html", {"project": project, "can_edit": has_capability(request.user, project.workspace, "edit")})


@login_required
def scene_detail(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").prefetch_related(
            "dialogue_lines", "prompts__ai_model", "prompts__blocks"
        ).filter(episode__project__workspace__in=accessible_workspaces(request.user)),
        id=scene_id,
    )
    return render(request, "studio/scene_detail.html", {
        "scene": scene,
        "can_edit": has_capability(request.user, scene.episode.project.workspace, "edit"),
    })

@login_required
def prompt_detail(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model")
        .prefetch_related("blocks", "ai_suggestions")
        .filter(scene__episode__project__workspace__in=accessible_workspaces(request.user)),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    return render(request, "studio/prompt_detail.html", {
        "prompt": prompt,
        "can_edit": has_capability(request.user, workspace, "edit"),
        "can_use_ai": has_capability(request.user, workspace, "use_ai") and user_has_ai_access(request.user),
    })


@login_required
def prompt_improve(request, prompt_id):
    if request.method != "POST":
        return redirect("studio:prompt_detail", prompt_id=prompt_id)
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model")
        .prefetch_related("blocks")
        .filter(scene__episode__project__workspace__in=accessible_workspaces(request.user)),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    if not has_capability(request.user, workspace, "use_ai") or not user_has_ai_access(request.user):
        messages.error(request, "AI access is not enabled for this account and workspace.")
        return redirect("studio:prompt_detail", prompt_id=prompt.id)
    try:
        improve_prompt(
            prompt=prompt, user=request.user, mode=request.POST.get("mode", "non_dialogue"),
            selected_block_ids=request.POST.getlist("selected_blocks"),
            text_model=request.POST.get("text_model", "gpt-5.4-mini"),
        )
    except (StudioAiError, ProviderError) as exc:
        messages.error(request, str(exc))
    else:
        messages.success(request, "AI suggestion is ready for review.")
    return redirect("studio:prompt_detail", prompt_id=prompt.id)


@login_required
def suggestion_decide(request, suggestion_id, decision):
    suggestion = get_object_or_404(
        AiSuggestion.objects.select_related("workspace", "prompt").filter(
            workspace__in=accessible_workspaces(request.user)
        ),
        id=suggestion_id,
    )
    if request.method != "POST" or not has_capability(request.user, suggestion.workspace, "edit"):
        messages.error(request, "Edit permission is required.")
        return redirect("studio:prompt_detail", prompt_id=suggestion.prompt_id)
    try:
        if decision == "accept":
            accept_suggestion(suggestion=suggestion, user=request.user)
        else:
            reject_suggestion(suggestion=suggestion, user=request.user)
    except StudioAiError as exc:
        messages.error(request, str(exc))
    else:
        messages.success(request, f"Suggestion {decision}ed.")
    return redirect("studio:prompt_detail", prompt_id=suggestion.prompt_id)