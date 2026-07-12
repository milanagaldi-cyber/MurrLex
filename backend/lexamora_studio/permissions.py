from .models import Workspace, WorkspaceMembership


ROLE_CAPABILITIES = {
    WorkspaceMembership.Role.OWNER: {"view", "edit", "translate", "manage_members", "use_ai", "export"},
    WorkspaceMembership.Role.ADMIN: {"view", "edit", "translate", "manage_members", "use_ai", "export"},
    WorkspaceMembership.Role.EDITOR: {"view", "edit", "translate"},
    WorkspaceMembership.Role.TRANSLATOR: {"view", "translate"},
    WorkspaceMembership.Role.VIEWER: {"view"},
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


def membership_for(user, workspace):
    if not getattr(user, "is_authenticated", False):
        return None
    return WorkspaceMembership.objects.filter(
        workspace=workspace,
        user=user,
        status=WorkspaceMembership.Status.ACTIVE,
    ).first()


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
