from django.db import transaction

from .models import Workspace, WorkspaceMembership


@transaction.atomic
def create_workspace(*, user, name, slug, description=""):
    workspace = Workspace.objects.create(
        name=name,
        slug=slug,
        description=description,
        owner=user,
        created_by=user,
        updated_by=user,
    )
    WorkspaceMembership.objects.create(
        workspace=workspace,
        user=user,
        role=WorkspaceMembership.Role.OWNER,
        status=WorkspaceMembership.Status.ACTIVE,
        can_use_ai=True,
        can_export=True,
        can_manage_members=True,
    )
    return workspace
