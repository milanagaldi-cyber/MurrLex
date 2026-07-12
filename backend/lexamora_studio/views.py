from django.contrib.auth.decorators import login_required
from django.shortcuts import get_object_or_404, redirect, render
from django.utils.text import slugify

from .models import Project, Scene, Workspace
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