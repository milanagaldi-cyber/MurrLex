from django.db import transaction

from .models import Prompt, Workspace, WorkspaceMembership
from .revisions import record_revision


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

@transaction.atomic
def update_dialogue_line(*, line, user, text, speaker=None, delivery=None):
    line.text = text
    if speaker is not None:
        line.speaker = speaker
    if delivery is not None:
        line.delivery = delivery
    line.updated_by = user
    line.full_clean()
    line.save()
    record_revision(instance=line, user=user, operation="UPDATE")
    Prompt.objects.filter(blocks__source_dialogue=line).update(needs_review=True)
    return line
