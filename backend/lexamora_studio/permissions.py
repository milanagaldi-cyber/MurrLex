from django.db.models import Q

from .models import AdditionalGeneration, AiSuggestion, Asset, Character, DialogueLine, Episode, ExportJob, Project, ProjectAccessExclusion, ProjectMembership, Prompt, PromptBlock, Revision, Scene, Workspace, WorkspaceMembership


ROLE_CAPABILITIES = {
    WorkspaceMembership.Role.OWNER: {"view", "edit", "translate", "manage_members", "manage_project", "use_ai", "export"},
    WorkspaceMembership.Role.ADMIN: {"view", "edit", "translate", "manage_members", "manage_project", "use_ai", "export"},
    WorkspaceMembership.Role.EDITOR: {"view", "edit", "translate"},
    WorkspaceMembership.Role.TRANSLATOR: {"view", "translate"},
    WorkspaceMembership.Role.VIEWER: {"view"},
}

PROJECT_ROLE_CAPABILITIES = {
    ProjectMembership.Role.VIEWER: {"view"},
    ProjectMembership.Role.EDITOR: {"view", "edit", "translate", "use_ai"},
    ProjectMembership.Role.CONTROLLER: {"view", "edit", "translate", "manage_project", "use_ai", "export"},
}


def accessible_workspaces(user):
    if not getattr(user, "is_authenticated", False):
        return Workspace.objects.none()
    if user.is_superuser:
        return Workspace.objects.all()
    return Workspace.objects.filter(
        memberships__user=user,
        memberships__status=WorkspaceMembership.Status.ACTIVE,
    ).distinct()


def accessible_projects(user):
    if not getattr(user, "is_authenticated", False):
        return Project.objects.none()
    if user.is_superuser:
        return Project.objects.filter(workspace__deleted_at__isnull=True)
    inherited = Q(
            workspace__memberships__user=user,
            workspace__memberships__status=WorkspaceMembership.Status.ACTIVE,
        ) & ~Q(access_exclusions__user=user)
    return Project.objects.filter(
        Q(workspace__owner=user) | inherited | Q(memberships__user=user, memberships__is_active=True),
        workspace__deleted_at__isnull=True,
    ).distinct()


def accessible_assets(user, *, include_deleted=False):
    manager = Asset.all_objects if include_deleted else Asset.objects
    projects = accessible_projects(user)
    workspaces = accessible_workspaces(user)
    return manager.filter(
        Q(projects__in=projects)
        | Q(referenced_by_scenes__episode__project__in=projects)
        | Q(referenced_by_characters__project__in=projects)
        | Q(referenced_by_prompts__scene__episode__project__in=projects)
        | Q(
            workspace__in=workspaces,
            projects__isnull=True,
        )
    ).distinct()


def accessible_suggestions(user):
    return AiSuggestion.objects.filter(prompt__scene__episode__project__in=accessible_projects(user))


def accessible_exports(user):
    return ExportJob.objects.filter(project__in=accessible_projects(user))


def accessible_revisions(user):
    projects = accessible_projects(user)
    return Revision.objects.filter(
        Q(entity_type="lexamora_studio.project", entity_id__in=projects.values("id"))
        | Q(entity_type="lexamora_studio.character", entity_id__in=Character.objects.filter(project__in=projects).values("id"))
        | Q(entity_type="lexamora_studio.episode", entity_id__in=Episode.objects.filter(project__in=projects).values("id"))
        | Q(entity_type="lexamora_studio.scene", entity_id__in=Scene.objects.filter(episode__project__in=projects).values("id"))
        | Q(entity_type="lexamora_studio.dialogueline", entity_id__in=DialogueLine.objects.filter(scene__episode__project__in=projects).values("id"))
        | Q(entity_type="lexamora_studio.prompt", entity_id__in=Prompt.objects.filter(scene__episode__project__in=projects).values("id"))
        | Q(entity_type="lexamora_studio.promptblock", entity_id__in=PromptBlock.objects.filter(prompt__scene__episode__project__in=projects).values("id"))
        | Q(entity_type="lexamora_studio.additionalgeneration", entity_id__in=AdditionalGeneration.objects.filter(scene__episode__project__in=projects).values("id"))
    )


def membership_for(user, workspace):
    if not getattr(user, "is_authenticated", False):
        return None
    return WorkspaceMembership.objects.filter(
        workspace=workspace,
        user=user,
        status=WorkspaceMembership.Status.ACTIVE,
    ).first()


def is_workspace_owner_or_admin(user, workspace):
    if getattr(user, "is_superuser", False):
        return True
    if workspace.owner_id == getattr(user, "id", None):
        return True
    return WorkspaceMembership.objects.filter(
        workspace=workspace,
        user=user,
        status=WorkspaceMembership.Status.ACTIVE,
        role__in=[WorkspaceMembership.Role.OWNER, WorkspaceMembership.Role.ADMIN],
    ).exists()


def has_capability(user, workspace, capability):
    if getattr(user, "is_superuser", False):
        return True
    membership = membership_for(user, workspace)
    if membership is None:
        return False
    if capability in ROLE_CAPABILITIES.get(membership.role, set()):
        return True
    return {
        "manage_members": membership.can_manage_members,
        "use_ai": membership.can_use_ai,
        "export": membership.can_export,
    }.get(capability, False)


def has_project_capability(user, project, capability):
    if getattr(user, "is_superuser", False):
        return True
    explicitly_revoked = ProjectAccessExclusion.objects.filter(project=project, user=user).exists()
    if not explicitly_revoked and has_capability(user, project.workspace, capability):
        return True
    membership = ProjectMembership.objects.filter(project=project, user=user, is_active=True).first()
    return membership is not None and capability in PROJECT_ROLE_CAPABILITIES.get(membership.role, set())


def project_for_object(item):
    if isinstance(item, Project):
        return item
    for path in (
        ("project",),
        ("episode", "project"),
        ("scene", "episode", "project"),
        ("prompt", "scene", "episode", "project"),
        ("generation", "scene", "episode", "project"),
    ):
        value = item
        try:
            for name in path:
                value = getattr(value, name)
        except (AttributeError, type(item).DoesNotExist):
            continue
        if isinstance(value, Project):
            return value
    return None


def has_object_capability(user, item, capability):
    project = project_for_object(item)
    if project is not None:
        return has_project_capability(user, project, capability)
    workspace = item if isinstance(item, Workspace) else getattr(item, "workspace", None)
    return workspace is not None and has_capability(user, workspace, capability)
