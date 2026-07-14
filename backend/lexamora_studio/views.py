import json
from datetime import timedelta

from django.conf import settings
from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.core import signing
from django.core.exceptions import ValidationError
from django.core.mail import send_mail
from django.db import transaction
from django.db.models import Prefetch, Q
from django.http import FileResponse, Http404, HttpResponse, HttpResponseForbidden, HttpResponseRedirect, JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.urls import reverse
from django.utils import timezone
from django.utils.http import url_has_allowed_host_and_scheme
from django.utils.text import slugify

from lessons.ai_gateway import ProviderError, TEXT_MODELS, run_text
from lessons.provider_credentials import user_has_ai_access

from .ai import StudioAiError, accept_suggestion, improve_prompt, preview_prompt_translation, reject_suggestion, translate_prompt, translate_prompt_dialogue, undo_suggestion
from .ai_catalog import PROMPT_LANGUAGES, active_text_models, default_prompt_template, default_text_model_id, selected_text_model
from .exports import ALL_SECTIONS, ExportError, generate_export
from .docx_imports import accept_docx_import, parse_docx
from .docx_exports import generate_docx_export
from .docx_roundtrip import compare_docx_export
from .forms import AdditionalGenerationForm, AiModelProfileForm, AssetEditForm, CharacterForm, DialogueLineForm, DocxImportUploadForm, EpisodeForm, GenerationOutputUploadForm, ImageUploadForm, MultipleImageUploadForm, ProjectForm, ProjectMembershipForm, PromptBlockForm, PromptForm, SceneForm, StudioTextModelForm, WorkspaceForm, WorkspaceMembershipForm
from .models import AdditionalGeneration, AiModelProfile, AiSuggestion, Asset, Character, DialogueLine, DocxImport, EmailDeliveryLog, Episode, ExportJob, GenerationOutput, Project, ProjectAccessExclusion, ProjectMembership, Prompt, PromptBlock, Scene, StudioTextModel, SubtitleTrack, TranslationUnit, Workspace, WorkspaceMembership
from .notifications import notify_access_granted
from .permissions import accessible_assets, accessible_projects, accessible_suggestions, accessible_workspaces, has_capability, has_object_capability, has_project_capability, is_workspace_owner_or_admin
from .revisions import audit, record_revision
from .services import bulk_replace_subtitle_lines, create_workspace, propagate_project_original_language, reorder_subtitle_lines, save_translation
from .storage import create_asset, crop_asset, purge_asset, restore_asset, trash_asset


ARCHIVE_PURGE_DELAY_SECONDS = 30


def _archive_purge_token(*, kind, item_id, user):
    return signing.dumps({"kind": kind, "id": str(item_id), "user": str(user.id), "issued": timezone.now().timestamp()}, salt="studio-archive-purge")


def _archive_purge_allowed(*, token, kind, item_id, user):
    try:
        payload = signing.loads(token, salt="studio-archive-purge", max_age=3600)
        age = timezone.now().timestamp() - float(payload["issued"])
    except (signing.BadSignature, KeyError, TypeError, ValueError):
        return False
    return payload.get("kind") == kind and payload.get("id") == str(item_id) and payload.get("user") == str(user.id) and age >= ARCHIVE_PURGE_DELAY_SECONDS


@login_required
def dashboard(request):
    sort = request.GET.get("sort", "updated")
    orderings = {"name": "name", "created": "-created_at", "updated": "-updated_at", "projects": "name"}
    workspaces = list(accessible_workspaces(request.user).select_related("owner", "created_by", "updated_by", "avatar_asset").prefetch_related("projects").order_by(orderings.get(sort, "-updated_at"), "name"))
    for workspace in workspaces:
        workspace.can_administer = is_workspace_owner_or_admin(request.user, workspace)
    shared_projects = accessible_projects(request.user).exclude(workspace__in=workspaces).select_related("workspace")
    archived_query = Workspace.all_objects.filter(deleted_at__isnull=False, purged_at__isnull=True)
    if not request.user.is_superuser:
        archived_query = archived_query.filter(Q(owner=request.user) | Q(memberships__user=request.user, memberships__status=WorkspaceMembership.Status.ACTIVE, memberships__role__in=[WorkspaceMembership.Role.OWNER, WorkspaceMembership.Role.ADMIN]))
    archived_workspaces = list(archived_query.select_related("owner").distinct().order_by("-deleted_at"))
    for workspace in archived_workspaces:
        workspace.purge_token = _archive_purge_token(kind="workspace", item_id=workspace.id, user=request.user)
    return render(request, "studio/dashboard.html", {"workspaces": workspaces, "shared_projects": shared_projects, "archived_workspaces": archived_workspaces, "sort": sort, "purge_delay": ARCHIVE_PURGE_DELAY_SECONDS})


def _set_single_default(model, instance):
    if instance.is_default:
        model.objects.exclude(pk=instance.pk).update(is_default=False)


@login_required
def studio_settings(request):
    if not request.user.is_superuser:
        return HttpResponseForbidden("Studio settings require a server administrator.")
    model_form = StudioTextModelForm(prefix="model")
    prompt_addition = default_prompt_template()
    if request.method == "POST":
        action = request.POST.get("action")
        if action in {"create_model", "update_model"}:
            instance = None
            if action == "update_model":
                instance = get_object_or_404(StudioTextModel, id=request.POST.get("id"))
            model_form = StudioTextModelForm(request.POST, instance=instance, prefix="model")
            if model_form.is_valid():
                item = model_form.save(commit=False)
                item.provider = StudioTextModel.Provider.OPENAI
                item.updated_by = request.user
                item.save()
                _set_single_default(StudioTextModel, item)
                messages.success(request, "OpenAI text model saved.")
                return redirect("studio:settings")
        elif action == "update_prompt_addition":
            prompt_addition.content = request.POST.get("prompt_addition", "").strip()
            prompt_addition.scope = prompt_addition.Scope.ALL
            prompt_addition.is_active = True
            prompt_addition.is_default = True
            prompt_addition.updated_by = request.user
            prompt_addition.save(update_fields=["content", "scope", "is_active", "is_default", "updated_by", "updated_at"])
            _set_single_default(prompt_addition.__class__, prompt_addition)
            messages.success(request, "Default prompt addition saved.")
            return redirect("studio:settings")
    return render(request, "studio/settings.html", {
        "model_form": model_form,
        "text_models": StudioTextModel.objects.all(),
        "prompt_addition": prompt_addition,
        "murrlex_default_model": default_text_model_id(),
    })


@login_required
def text_model_quick_create(request):
    if request.method != "POST" or not request.user.is_superuser:
        return JsonResponse({"error": "Server administrator access is required."}, status=403)
    model_id = request.POST.get("model_id", "").strip()
    name = request.POST.get("name", "").strip()
    if model_id not in TEXT_MODELS or not name:
        return JsonResponse({"error": "Choose a supported model and enter its name."}, status=400)
    model, _ = StudioTextModel.objects.update_or_create(
        model_id=model_id,
        defaults={
            "name": name[:120],
            "provider": StudioTextModel.Provider.OPENAI,
            "is_active": True,
            "updated_by": request.user,
        },
    )
    return JsonResponse({"id": str(model.id), "modelId": model.model_id, "name": model.name})


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
    workspace = get_object_or_404(accessible_workspaces(request.user).select_related("avatar_asset"), id=workspace_id)
    sort = request.GET.get("sort", "updated")
    orderings = {"title": "title", "created": "-created_at", "updated": "-updated_at", "type": "project_type"}
    sort_key = f"studio_workspace_sort_{workspace.id}"
    if request.GET.get("sort") in orderings:
        request.session[sort_key] = request.GET["sort"]
    elif not request.GET.get("sort"):
        sort = request.session.get(sort_key, sort)
    projects = list(accessible_projects(request.user).filter(workspace=workspace).select_related("created_by", "updated_by", "cover_asset").prefetch_related("media_assets", "assets", "memberships__user").order_by(orderings.get(sort, "-updated_at"), "title"))
    for project in projects:
        project.display_cover_asset = project.cover_asset or min(
            (asset for asset in project.media_assets.all() if asset.thumbnail),
            key=lambda asset: asset.created_at,
            default=None,
        )
        project.can_edit = has_project_capability(request.user, project, "edit")
        project.can_manage = has_project_capability(request.user, project, "manage_project")
        project.can_administer = is_workspace_owner_or_admin(request.user, workspace)
    recycle_count = Asset.all_objects.filter(workspace=workspace, deleted_at__isnull=False, purged_at__isnull=True).count()
    archive_count = Project.all_objects.filter(workspace=workspace, deleted_at__isnull=False, purged_at__isnull=True).count()
    workspace_assets = _decorate_gallery_assets(
        request.user,
        list(_accessible_workspace_images(request.user, workspace)),
        deduplicate=True,
    )
    return render(request, "studio/workspace_detail.html", {
        "workspace": workspace,
        "projects": projects,
        "sort": sort,
        "can_edit": has_capability(request.user, workspace, "edit"),
        "can_manage": has_capability(request.user, workspace, "manage_members"),
        "can_administer": is_workspace_owner_or_admin(request.user, workspace),
        "imports": workspace.docx_imports.select_related("project")[:10],
        "recycle_count": recycle_count,
        "archive_count": archive_count,
        "gallery_assets": workspace_assets,
        "gallery_projects": projects,
        "project_assets": [],
        "workspace_assets": workspace_assets,
    })


@login_required
def workspace_edit(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user).select_related("avatar_asset"), id=workspace_id)
    if not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    if request.method == "POST" and request.POST.get("action") == "test_email":
        recipient = request.POST.get("recipient", "").strip().lower()
        allowed = {workspace.owner.email.lower() if workspace.owner.email else ""}
        allowed.update(value.lower() for value in workspace.memberships.filter(status=WorkspaceMembership.Status.ACTIVE).values_list("user__email", flat=True) if value)
        if recipient not in allowed:
            return HttpResponseForbidden("Choose a current workspace member.")
        backend = settings.EMAIL_BACKEND
        diagnostic = f"backend={backend}\nhost={settings.EMAIL_HOST}\nport={settings.EMAIL_PORT}\ntls={settings.EMAIL_USE_TLS}\nssl={settings.EMAIL_USE_SSL}\nrecipient={recipient}\n"
        try:
            delivered = send_mail(
                f"Lexamora Studio email test: {workspace.name}",
                f"This is a test message from workspace {workspace.name}.\nSent by {request.user.get_full_name() or request.user.username}.",
                settings.DEFAULT_FROM_EMAIL, [recipient], fail_silently=False,
            )
            success = delivered == 1
            diagnostic += f"result={'delivered' if success else 'backend returned zero deliveries'}"
        except Exception as exc:
            success = False
            diagnostic += f"result=error\nerror_type={exc.__class__.__name__}\nerror={exc}"
        log = EmailDeliveryLog.objects.create(workspace=workspace, recipient=recipient, success=success, detail=diagnostic, created_by=request.user)
        messages.success(request, "Test email sent." if success else "Test email failed. Download the diagnostic log.")
        return HttpResponseRedirect(reverse("studio:workspace_edit", kwargs={"workspace_id": workspace.id}) + f"#email-test-{log.id}")
    form = WorkspaceForm(request.POST or None, request.FILES or None, instance=workspace)
    if request.method == "POST" and form.is_valid():
        item = form.save(commit=False)
        item.updated_by = request.user
        avatar = form.cleaned_data.get("avatar")
        if avatar:
            item.avatar_asset = create_asset(user=request.user, workspace=workspace, uploaded=avatar, kind=Asset.Kind.OTHER)
        item.save()
        record_revision(instance=item, user=request.user, operation="UPDATE")
        messages.success(request, "Workspace settings saved.")
        return redirect("studio:workspace_detail", workspace_id=item.id)
    return render(request, "studio/workspace_settings.html", {
        "workspace": workspace, "form": form, "models": AiModelProfile.objects.all(),
        "can_administer": is_workspace_owner_or_admin(request.user, workspace),
        "email_recipients": [workspace.owner, *[item.user for item in workspace.memberships.filter(status=WorkspaceMembership.Status.ACTIVE).select_related("user") if item.user_id != workspace.owner_id]],
        "email_logs": workspace.email_delivery_logs.select_related("created_by")[:10],
    })


@login_required
def workspace_email_log_download(request, workspace_id, log_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    if not has_capability(request.user, workspace, "manage_members"):
        return HttpResponseForbidden("Workspace administrator permission is required.")
    log = get_object_or_404(EmailDeliveryLog, workspace=workspace, id=log_id)
    content = f"Lexamora Studio email diagnostic\ncreated={log.created_at.isoformat()}\ncreated_by={log.created_by_id}\nsuccess={log.success}\n{log.detail}\n"
    response = HttpResponse(content, content_type="text/plain; charset=utf-8")
    response["Content-Disposition"] = f'attachment; filename="lexamora-email-{log.id}.txt"'
    return response


@login_required
def workspace_access(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user).select_related("owner"), id=workspace_id)
    if not has_capability(request.user, workspace, "manage_members"):
        return HttpResponseForbidden("Workspace member management permission is required.")
    form = WorkspaceMembershipForm(request.POST or None, workspace=workspace)
    if request.method == "POST":
        action = request.POST.get("action", "save")
        if action == "remove":
            membership = get_object_or_404(WorkspaceMembership, workspace=workspace, id=request.POST.get("membership_id"))
            if membership.user_id == workspace.owner_id or membership.role == WorkspaceMembership.Role.OWNER:
                return HttpResponseForbidden("The workspace owner cannot be removed.")
            ProjectMembership.objects.filter(project__workspace=workspace, user=membership.user).delete()
            ProjectAccessExclusion.objects.filter(project__workspace=workspace, user=membership.user).delete()
            membership.delete()
            messages.success(request, "Workspace access removed.")
            return redirect("studio:workspace_access", workspace_id=workspace.id)
        if form.is_valid():
            existed = WorkspaceMembership.objects.filter(workspace=workspace, user=form.user, status=WorkspaceMembership.Status.ACTIVE).exists()
            WorkspaceMembership.objects.update_or_create(
                workspace=workspace,
                user=form.user,
                defaults={"role": form.cleaned_data["role"], "status": WorkspaceMembership.Status.ACTIVE, "can_use_ai": form.cleaned_data["can_use_ai"]},
            )
            ProjectAccessExclusion.objects.filter(project__workspace=workspace, user=form.user).delete()
            if not existed:
                notify_access_granted(user=form.user, entity_name=workspace.name, entity_kind="workspace", url=request.build_absolute_uri(reverse("studio:workspace_detail", kwargs={"workspace_id": workspace.id})), granted_by=request.user)
            messages.success(request, "Workspace access saved and inherited by its projects.")
            return redirect("studio:workspace_access", workspace_id=workspace.id)
    return render(request, "studio/workspace_access.html", {"workspace": workspace, "form": form, "owner_membership": workspace.memberships.filter(user=workspace.owner).select_related("user").first(), "memberships": workspace.memberships.exclude(user=workspace.owner).select_related("user")})


@login_required
def workspace_avatar_upload(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    if not has_capability(request.user, workspace, "manage_members"):
        return HttpResponseForbidden("Workspace administrator permission is required.")
    form = ImageUploadForm(request.POST or None, request.FILES or None)
    if request.method == "POST" and form.is_valid():
        asset = create_asset(user=request.user, workspace=workspace, uploaded=form.cleaned_data["file"], kind=Asset.Kind.OTHER)
        workspace.avatar_asset = asset
        workspace.updated_by = request.user
        workspace.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
        messages.success(request, "Workspace image updated.")
        return redirect("studio:workspace_detail", workspace_id=workspace.id)
    return render(request, "studio/entity_form.html", {"form": form, "title": "Workspace image", "multipart": True, "return_to": reverse("studio:workspace_detail", kwargs={"workspace_id": workspace.id})})


@login_required
def workspace_media_models(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    if not has_capability(request.user, workspace, "manage_project"):
        return HttpResponseForbidden("Workspace administrator permission is required.")
    instance = None
    if request.POST.get("model_pk"):
        instance = get_object_or_404(AiModelProfile, id=request.POST.get("model_pk"))
    form = AiModelProfileForm(request.POST or None, instance=instance)
    if request.method == "POST" and form.is_valid():
        form.save()
        messages.success(request, "Generation model saved.")
        return redirect("studio:workspace_media_models", workspace_id=workspace.id)
    return render(request, "studio/workspace_media_models.html", {"workspace": workspace, "models": AiModelProfile.objects.all(), "form": form})


def _unique_workspace_identity(name):
    base_name = f"{name} copy"[:170]
    candidate, counter = base_name, 2
    while Workspace.all_objects.filter(name=candidate).exists():
        candidate = f"{base_name} {counter}"[:180]
        counter += 1
    base_slug = slugify(candidate)[:170] or "workspace-copy"
    slug, counter = base_slug, 2
    while Workspace.all_objects.filter(slug=slug).exists():
        slug = f"{base_slug}-{counter}"[:180]
        counter += 1
    return candidate, slug


@login_required
@transaction.atomic
def workspace_copy(request, workspace_id):
    source = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    if request.method != "POST" or not is_workspace_owner_or_admin(request.user, source):
        return HttpResponseForbidden("Workspace owner or administrator permission is required.")
    name, slug = _unique_workspace_identity(source.name)
    copied = create_workspace(user=request.user, name=name, slug=slug)
    copied.description = source.description
    copied.documentation_language = source.documentation_language
    copied.dialogue_language = source.dialogue_language
    copied.prompt_language = source.prompt_language
    copied.image_prompt_template = source.image_prompt_template
    copied.video_prompt_template = source.video_prompt_template
    copied.audio_prompt_template = source.audio_prompt_template
    copied.text_prompt_template = source.text_prompt_template
    copied.save(update_fields=["description", "documentation_language", "dialogue_language", "prompt_language", "image_prompt_template", "video_prompt_template", "audio_prompt_template", "text_prompt_template", "updated_at"])
    messages.success(request, "Workspace copied. Projects can be copied into it individually.")
    return redirect("studio:workspace_detail", workspace_id=copied.id)


@login_required
def workspace_trash(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    if request.method != "POST" or not is_workspace_owner_or_admin(request.user, workspace):
        return HttpResponseForbidden("Workspace owner or administrator permission is required.")
    workspace.deleted_at = timezone.now()
    workspace.deleted_by = request.user
    workspace.updated_by = request.user
    workspace.save(update_fields=["deleted_at", "deleted_by", "updated_by", "updated_at"])
    messages.success(request, "Workspace archived. It remains available in Archive until explicitly deleted.")
    return redirect("studio:dashboard")


@login_required
def workspace_restore(request, workspace_id):
    workspace = get_object_or_404(Workspace.all_objects, id=workspace_id, deleted_at__isnull=False)
    if request.method != "POST" or not is_workspace_owner_or_admin(request.user, workspace):
        return HttpResponseForbidden("Workspace owner or administrator permission is required.")
    workspace.deleted_at = None
    workspace.deleted_by = None
    workspace.updated_by = request.user
    workspace.save(update_fields=["deleted_at", "deleted_by", "updated_by", "updated_at"])
    messages.success(request, "Workspace restored.")
    return redirect("studio:workspace_detail", workspace_id=workspace.id)


@login_required
def workspace_purge(request, workspace_id):
    workspace = get_object_or_404(Workspace.all_objects, id=workspace_id, deleted_at__isnull=False, purged_at__isnull=True)
    if request.method != "POST" or not is_workspace_owner_or_admin(request.user, workspace):
        return HttpResponseForbidden("Workspace owner or administrator permission is required.")
    if not _archive_purge_allowed(token=request.POST.get("purge_token", ""), kind="workspace", item_id=workspace.id, user=request.user):
        return HttpResponseForbidden("Wait 30 seconds on the archive confirmation before deleting permanently.")
    for asset in Asset.all_objects.filter(workspace=workspace, purged_at__isnull=True):
        purge_asset(asset=asset, user=request.user)
    Project.all_objects.filter(workspace=workspace, purged_at__isnull=True).update(purged_at=timezone.now(), purged_by=request.user, updated_by=request.user)
    workspace.purged_at = timezone.now()
    workspace.purged_by = request.user
    workspace.updated_by = request.user
    workspace.save(update_fields=["purged_at", "purged_by", "updated_by", "updated_at"])
    messages.success(request, "Workspace permanently removed from the archive.")
    return redirect("studio:dashboard")


def _create_entity(request, *, form_class, parent, parent_field, workspace, title, success_url, position_manager=None):
    if not has_object_capability(request.user, parent, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = form_class(request.POST or None)
    if request.method != "POST" and isinstance(parent, Project) and form_class is EpisodeForm:
        form.fields["language"].initial = parent.dialogue_language or parent.original_language or "EN"
    if request.method == "POST" and form.is_valid():
        item = form.save(commit=False)
        setattr(item, parent_field, parent)
        item.created_by = request.user
        item.updated_by = request.user
        if position_manager is not None:
            item.position = position_manager.count()
        if isinstance(item, Scene):
            item.number = item.position + 1
        try:
            item.full_clean()
        except ValidationError as exc:
            form.add_error(None, exc)
        else:
            item.save()
            record_revision(instance=item, user=request.user, operation="CREATE")
            messages.success(request, f"{title} created.")
            return redirect(*success_url(item))
    return render(request, "studio/entity_form.html", {"form": form, "title": title})


@login_required
def project_create(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    if not has_object_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    initial = {
        "original_language": workspace.dialogue_language, "documentation_language": workspace.documentation_language,
        "dialogue_language": workspace.dialogue_language, "prompt_language": workspace.prompt_language,
    }
    form = ProjectForm(request.POST or None, initial=initial)
    if request.method == "POST" and form.is_valid():
        item = form.save(commit=False); item.workspace = workspace; item.created_by = request.user; item.updated_by = request.user
        item.save(); record_revision(instance=item, user=request.user, operation="CREATE")
        messages.success(request, "New project created.")
        return redirect("studio:project_detail", project_id=item.id)
    return render(request, "studio/entity_form.html", {"form": form, "title": "New project"})


@login_required
def character_create(request, project_id):
    project = get_object_or_404(accessible_projects(request.user), id=project_id)
    return _create_entity(request, form_class=CharacterForm, parent=project, parent_field="project", workspace=project.workspace, title="New character", success_url=lambda item: ("studio:project_detail", item.project_id), position_manager=project.characters)


@login_required
def episode_create(request, project_id):
    project = get_object_or_404(accessible_projects(request.user), id=project_id)
    return _create_entity(request, form_class=EpisodeForm, parent=project, parent_field="project", workspace=project.workspace, title="New episode", success_url=lambda item: ("studio:project_detail", item.project_id), position_manager=project.episodes)


@login_required
def scene_create(request, episode_id):
    episode = get_object_or_404(Episode.objects.filter(project__in=accessible_projects(request.user)), id=episode_id)
    return _create_entity(request, form_class=SceneForm, parent=episode, parent_field="episode", workspace=episode.project.workspace, title="New scene", success_url=lambda item: ("studio:scene_detail", item.id), position_manager=episode.scenes)


@login_required
def dialogue_create(request, scene_id):
    scene = get_object_or_404(Scene.objects.filter(episode__project__in=accessible_projects(request.user)), id=scene_id)
    return _create_entity(request, form_class=DialogueLineForm, parent=scene, parent_field="scene", workspace=scene.episode.project.workspace, title="New dialogue line", success_url=lambda item: ("studio:scene_detail", item.scene_id), position_manager=scene.dialogue_lines)


@login_required
def prompt_create(request, scene_id):
    scene = get_object_or_404(Scene.objects.filter(episode__project__in=accessible_projects(request.user)), id=scene_id)
    return _create_entity(request, form_class=PromptForm, parent=scene, parent_field="scene", workspace=scene.episode.project.workspace, title="New prompt", success_url=lambda item: ("studio:prompt_detail", item.id), position_manager=scene.prompts)


@login_required
def prompt_block_create(request, prompt_id):
    prompt = get_object_or_404(Prompt.objects.filter(scene__episode__project__in=accessible_projects(request.user)), id=prompt_id)
    return _create_entity(request, form_class=PromptBlockForm, parent=prompt, parent_field="prompt", workspace=prompt.scene.episode.project.workspace, title="New prompt block", success_url=lambda item: ("studio:prompt_detail", item.prompt_id), position_manager=prompt.blocks)


def _edit_entity(request, *, item, form_class, workspace, title, success_url):
    if not has_object_capability(request.user, item, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = form_class(request.POST or None, instance=item)
    if request.method == "POST" and form.is_valid():
        item = form.save(commit=False)
        item.updated_by = request.user
        try:
            item.full_clean()
        except ValidationError as exc:
            form.add_error(None, exc)
        else:
            item.save()
            record_revision(instance=item, user=request.user, operation="UPDATE")
            messages.success(request, f"{title} saved.")
            return redirect(*success_url(item))
    return render(request, "studio/entity_form.html", {"form": form, "title": title, "submit_label": "Save changes"})


def _image_upload(request, *, target, workspace, title, success_url, kind, project=None, scene=None, character=None):
    if not has_object_capability(request.user, target, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = MultipleImageUploadForm(request.POST or None, request.FILES or None, limit=10)
    if request.method == "POST" and form.is_valid():
        uploaded_count = 0
        first_asset = None
        for uploaded in form.cleaned_data["file"]:
            try:
                asset = create_asset(user=request.user, workspace=workspace, uploaded=uploaded, kind=kind, project=project, scene=scene, character=character, prevent_duplicate=True)
            except ValidationError as exc:
                messages.warning(request, "; ".join(exc.messages))
            else:
                uploaded_count += 1
                first_asset = first_asset or asset
        image_role = request.POST.get("image_role", "")
        if first_asset and image_role == "workspace_avatar" and isinstance(target, Workspace):
            target.avatar_asset = first_asset
            target.updated_by = request.user
            target.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
        elif first_asset and image_role == "character_avatar" and isinstance(target, Character):
            target.avatar_asset = first_asset
            target.updated_by = request.user
            target.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
        elif first_asset and image_role == "project_cover" and isinstance(target, Project):
            target.cover_asset = first_asset
            target.updated_by = request.user
            target.save(update_fields=["cover_asset", "updated_by", "updated_at"])
        if uploaded_count:
            messages.success(request, f"Uploaded {uploaded_count} image{'s' if uploaded_count != 1 else ''}.")
        if uploaded_count or form.cleaned_data["file"]:
            requested = request.POST.get("next", "")
            if requested and url_has_allowed_host_and_scheme(requested, {request.get_host()}, require_https=request.is_secure()):
                return HttpResponseRedirect(requested)
            return redirect(*success_url(target))
    return render(request, "studio/entity_form.html", {"form": form, "title": title, "submit_label": "Upload image", "multipart": True, "return_to": request.GET.get("next", "")})


def _accessible_workspace_images(user, workspace):
    projects = accessible_projects(user).filter(workspace=workspace)
    return Asset.objects.filter(
        workspace=workspace,
        content_type__startswith="image/",
    ).filter(
        Q(projects__in=projects)
        | Q(projects__isnull=True),
    ).select_related("project").prefetch_related(
        "projects",
        Prefetch("referenced_by_scenes", queryset=Scene.objects.select_related("episode")),
        Prefetch("referenced_by_characters", queryset=Character.objects.select_related("project")),
        Prefetch("referenced_by_prompts", queryset=Prompt.objects.select_related("scene__episode")),
        "project_cover_for",
        Prefetch("character_avatar_for", queryset=Character.objects.select_related("project")),
    ).distinct().order_by("-created_at")


def _picker_assets(user, project):
    workspace_assets = _decorate_gallery_assets(
        user,
        list(_accessible_workspace_images(user, project.workspace)),
        deduplicate=True,
    )
    project_id = str(project.id)
    project_assets = [
        asset for asset in workspace_assets
        if project_id in asset.gallery_project_ids.split(",")
    ]
    return project_assets, workspace_assets


def _project_header_context(user, project):
    return {
        "can_edit": has_project_capability(user, project, "edit"),
        "can_manage_project": has_project_capability(user, project, "manage_project"),
        "can_administer": is_workspace_owner_or_admin(user, project.workspace),
    }


@login_required
def project_edit(request, project_id):
    item = get_object_or_404(accessible_projects(request.user), id=project_id)
    if not has_object_capability(request.user, item, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    old_language = (item.original_language or "EN").strip().upper()
    form = ProjectForm(request.POST or None, instance=item)
    if request.method == "POST" and form.is_valid():
        new_language = form.cleaned_data["original_language"]
        with transaction.atomic():
            item = form.save(commit=False)
            item.updated_by = request.user
            item.full_clean()
            item.save()
            record_revision(instance=item, user=request.user, operation="UPDATE")
            if new_language != old_language:
                counts = propagate_project_original_language(project=item, language=new_language, user=request.user)
                audit(
                    workspace=item.workspace,
                    actor=request.user,
                    action="PROJECT_ORIGINAL_LANGUAGE_PROPAGATED",
                    instance=item,
                    metadata={"from": old_language, "to": new_language, **counts},
                )
        if new_language != old_language:
            messages.success(
                request,
                f"Original language changed {old_language} -> {new_language}. "
                f"Updated {counts['originalPrompts']} original prompts, {counts['translations']} translations, "
                f"and {counts['dialogueLines']} dialogue lines.",
            )
        else:
            messages.success(request, "Project saved.")
        return redirect("studio:project_detail", project_id=item.id)
    return render(request, "studio/entity_form.html", {
        "form": form,
        "title": "Edit project",
        "submit_label": "Save changes",
        "language_propagation": True,
    })


@login_required
def character_edit(request, character_id):
    item = get_object_or_404(Character.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)), id=character_id)
    return _edit_entity(request, item=item, form_class=CharacterForm, workspace=item.project.workspace, title="Edit character", success_url=lambda value: ("studio:project_detail", value.project_id))


@login_required
def character_detail(request, character_id):
    character = get_object_or_404(
        Character.objects.select_related("project__workspace", "avatar_asset").prefetch_related("assets", "reference_assets").filter(
            project__in=accessible_projects(request.user)
        ), id=character_id,
    )
    can_edit = has_object_capability(request.user, character, "edit")
    form = CharacterForm(request.POST or None, instance=character)
    if request.method == "POST":
        if not can_edit:
            return HttpResponseForbidden("Edit permission is required.")
        if form.is_valid():
            character = form.save(commit=False)
            character.updated_by = request.user
            character.save()
            record_revision(instance=character, user=request.user, operation="INLINE_UPDATE")
            messages.success(request, "Character saved.")
            return HttpResponseRedirect(reverse("studio:character_detail", kwargs={"character_id": character.id}) + "#character-editor")
    project_assets, workspace_assets = _picker_assets(request.user, character.project)
    return render(request, "studio/character_detail.html", {
        "character": character,
        "character_form": form,
        "can_edit": can_edit,
        "project_assets": project_assets,
        "workspace_assets": workspace_assets,
    })


@login_required
def asset_attach(request, scope, owner_id):
    if scope == "workspace_avatar":
        owner = get_object_or_404(accessible_workspaces(request.user), id=owner_id)
        project = None
        workspace = owner
    elif scope == "project_cover":
        owner = get_object_or_404(
            accessible_projects(request.user).select_related("workspace"), id=owner_id
        )
        project = owner
        workspace = owner.workspace
    elif scope == "scene":
        owner = get_object_or_404(
            Scene.objects.select_related("episode__project__workspace").filter(
                episode__project__in=accessible_projects(request.user)
            ), id=owner_id,
        )
        project = owner.episode.project
        workspace = project.workspace
    elif scope in {"character", "character_avatar"}:
        owner = get_object_or_404(
            Character.objects.select_related("project__workspace").filter(
                project__in=accessible_projects(request.user)
            ), id=owner_id,
        )
        project = owner.project
        workspace = project.workspace
    else:
        return HttpResponseForbidden("Unsupported image attachment scope.")
    permitted = (
        has_capability(request.user, owner, "manage_members")
        if scope == "workspace_avatar"
        else has_object_capability(request.user, owner, "edit")
    )
    if request.method != "POST" or not permitted:
        return HttpResponseForbidden("Edit permission is required.")
    asset = get_object_or_404(
        Asset.objects.filter(
            workspace=workspace,
            content_type__startswith="image/",
        ).filter(
            Q(projects__in=accessible_projects(request.user))
            | Q(projects__isnull=True),
        ).distinct(), id=request.POST.get("asset_id"),
    )
    if project is not None:
        asset.projects.add(project)
    if scope == "workspace_avatar":
        owner.avatar_asset = asset
        owner.updated_by = request.user
        owner.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
    elif scope == "project_cover":
        owner.cover_asset = asset
        owner.updated_by = request.user
        owner.save(update_fields=["cover_asset", "updated_by", "updated_at"])
    elif scope == "scene":
        owner.reference_assets.add(asset)
    else:
        owner.reference_assets.add(asset)
    if scope in {"scene", "character", "character_avatar"}:
        asset.updated_by = request.user
        asset.save(update_fields=["updated_by", "updated_at"])
    if scope == "character_avatar":
        owner.avatar_asset = asset
        owner.updated_by = request.user
        owner.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
    audit(workspace=workspace, actor=request.user, action="ASSET_ATTACHED", instance=asset, metadata={"scope": scope, "ownerId": str(owner.id)})
    messages.success(request, "Image selection saved.")
    requested = request.POST.get("next", "")
    if requested and url_has_allowed_host_and_scheme(requested, {request.get_host()}, require_https=request.is_secure()):
        return HttpResponseRedirect(requested)
    if scope == "workspace_avatar":
        return redirect("studio:workspace_detail", workspace_id=owner.id)
    if scope == "project_cover":
        return redirect("studio:workspace_detail", workspace_id=workspace.id)
    if scope == "scene":
        return redirect("studio:scene_detail", scene_id=owner.id)
    return redirect("studio:character_detail", character_id=owner.id)


@login_required
def character_set_avatar(request, character_id):
    character = get_object_or_404(Character.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)), id=character_id)
    if request.method != "POST" or not has_object_capability(request.user, character, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    asset = get_object_or_404(
        Asset.objects.filter(
            Q(character=character) | Q(referenced_by_characters=character),
            content_type__startswith="image/",
        ).distinct(),
        id=request.POST.get("asset_id"),
    )
    asset.projects.add(character.project)
    character.avatar_asset = asset; character.updated_by = request.user
    character.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
    messages.success(request, "Character avatar saved.")
    requested = request.POST.get("next", "")
    if requested and url_has_allowed_host_and_scheme(requested, {request.get_host()}, require_https=request.is_secure()):
        return HttpResponseRedirect(requested)
    return HttpResponseRedirect(reverse("studio:character_detail", kwargs={"character_id": character.id}) + "#references")


@login_required
def episode_edit(request, episode_id):
    item = get_object_or_404(Episode.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)), id=episode_id)
    return _edit_entity(request, item=item, form_class=EpisodeForm, workspace=item.project.workspace, title="Edit episode", success_url=lambda value: ("studio:project_detail", value.project_id))


@login_required
def scene_edit(request, scene_id):
    item = get_object_or_404(Scene.objects.select_related("episode__project__workspace").filter(episode__project__in=accessible_projects(request.user)), id=scene_id)
    return _edit_entity(request, item=item, form_class=SceneForm, workspace=item.episode.project.workspace, title="Edit scene", success_url=lambda value: ("studio:scene_detail", value.id))


@login_required
def dialogue_edit(request, line_id):
    item = get_object_or_404(DialogueLine.objects.select_related("scene__episode__project__workspace").filter(scene__episode__project__in=accessible_projects(request.user)), id=line_id)
    workspace = item.scene.episode.project.workspace
    if not has_object_capability(request.user, item, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = DialogueLineForm(request.POST or None, instance=item)
    if request.method == "POST" and form.is_valid():
        update_dialogue_line(
            line=item, user=request.user, text=form.cleaned_data["text"],
            speaker=form.cleaned_data["speaker"], delivery=form.cleaned_data["delivery"],
            language=form.cleaned_data["language"], status=form.cleaned_data["status"],
        )
        messages.success(request, "Dialogue line saved.")
        return redirect("studio:scene_detail", scene_id=item.scene_id)
    return render(request, "studio/entity_form.html", {"form": form, "title": "Edit dialogue line", "submit_label": "Save changes"})


@login_required
def prompt_edit(request, prompt_id):
    item = get_object_or_404(Prompt.objects.select_related("scene__episode__project__workspace").filter(scene__episode__project__in=accessible_projects(request.user)), id=prompt_id)
    return _edit_entity(request, item=item, form_class=PromptForm, workspace=item.scene.episode.project.workspace, title="Edit prompt", success_url=lambda value: ("studio:prompt_detail", value.id))


@login_required
def prompt_block_edit(request, block_id):
    item = get_object_or_404(PromptBlock.objects.select_related("prompt__scene__episode__project__workspace").filter(prompt__scene__episode__project__in=accessible_projects(request.user)), id=block_id)
    return _edit_entity(request, item=item, form_class=PromptBlockForm, workspace=item.prompt.scene.episode.project.workspace, title="Edit prompt block", success_url=lambda value: ("studio:prompt_detail", value.prompt_id))


@login_required
def project_image_upload(request, project_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace").prefetch_related("assets", "characters__assets", "episodes__scenes"), id=project_id)
    return _image_upload(request, target=project, workspace=project.workspace, title="Upload project image", success_url=lambda value: ("studio:project_detail", value.id), kind=Asset.Kind.OTHER, project=project)


@login_required
def workspace_image_upload(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    return _image_upload(
        request,
        target=workspace,
        workspace=workspace,
        title="Upload workspace images",
        success_url=lambda value: ("studio:workspace_detail", value.id),
        kind=Asset.Kind.OTHER,
    )


@login_required
def character_image_upload(request, character_id):
    character = get_object_or_404(Character.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)), id=character_id)
    return _image_upload(request, target=character, workspace=character.project.workspace, title="Upload character reference", success_url=lambda value: ("studio:character_detail", value.id), kind=Asset.Kind.CHARACTER_REFERENCE, project=character.project, character=character)


@login_required
def scene_image_upload(request, scene_id):
    scene = get_object_or_404(Scene.objects.select_related("episode__project__workspace").filter(episode__project__in=accessible_projects(request.user)), id=scene_id)
    return _image_upload(request, target=scene, workspace=scene.episode.project.workspace, title="Upload scene image", success_url=lambda value: ("studio:scene_detail", value.id), kind=Asset.Kind.SCENE_IMAGE, project=scene.episode.project, scene=scene)
@login_required
def docx_import_create(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    if not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = DocxImportUploadForm(request.POST or None, request.FILES or None)
    if request.method == "POST" and form.is_valid():
        uploaded = form.cleaned_data["file"]
        try:
            parsed_data, warnings = parse_docx(uploaded)
            source_asset = create_asset(user=request.user, workspace=workspace, uploaded=uploaded, kind=Asset.Kind.SOURCE_DOCUMENT)
        except ValidationError as exc:
            form.add_error("file", exc)
        else:
            draft = DocxImport.objects.create(workspace=workspace, source_asset=source_asset, parsed_data=parsed_data, warnings=warnings, requested_by=request.user)
            messages.success(request, "DOCX parsed. Review the preview before accepting it.")
            return redirect("studio:docx_import_detail", import_id=draft.id)
    return render(request, "studio/entity_form.html", {"form": form, "title": "Import DOCX", "submit_label": "Parse document", "multipart": True})


@login_required
def docx_import_detail(request, import_id):
    draft = get_object_or_404(DocxImport.objects.select_related("workspace", "source_asset", "project").filter(workspace__in=accessible_workspaces(request.user)), id=import_id)
    can_edit = has_capability(request.user, draft.workspace, "edit")
    if request.method == "POST":
        if not can_edit:
            return HttpResponseForbidden("Edit permission is required.")
        if draft.status != DocxImport.Status.PREVIEW:
            messages.error(request, "An accepted import preview cannot be changed.")
            return redirect("studio:docx_import_detail", import_id=draft.id)
        data = draft.parsed_data
        data["title"] = request.POST.get("title", data.get("title", "")).strip()[:240]
        for character_index, character in enumerate(data.get("characters", [])):
            character["include"] = request.POST.get(f"character_{character_index}_include") == "on"
            character["name"] = request.POST.get(f"character_{character_index}_name", character.get("name", "")).strip()[:180]
            character["description"] = request.POST.get(f"character_{character_index}_description", character.get("description", "")).strip()
        for episode_index, episode in enumerate(data.get("episodes", [])):
            episode["include"] = request.POST.get(f"episode_{episode_index}_include") == "on"
            episode["title"] = request.POST.get(f"episode_{episode_index}_title", episode.get("title", "")).strip()[:240]
            episode["summary"] = request.POST.get(f"episode_{episode_index}_summary", episode.get("summary", "")).strip()
            for scene_index, scene in enumerate(episode.get("scenes", [])):
                prefix = f"episode_{episode_index}_scene_{scene_index}"
                scene["include"] = request.POST.get(f"{prefix}_include") == "on"
                for prompt_index, prompt in enumerate(scene.get("prompts", [])):
                    prompt["include"] = request.POST.get(f"{prefix}_prompt_{prompt_index}_include") == "on"
                    prompt["model"] = request.POST.get(f"{prefix}_prompt_{prompt_index}_model", prompt.get("model", "")).strip()[:120]
                    prompt["content"] = request.POST.get(f"{prefix}_prompt_{prompt_index}_content", prompt.get("content", "")).strip()
                for field, limit in (("title", 240), ("hook", None), ("description", None), ("location", None), ("actions", None), ("performance_notes", None), ("dialogue", None)):
                    value = request.POST.get(f"{prefix}_{field}", scene.get(field, "")).strip()
                    scene[field] = value[:limit] if limit else value
        draft.parsed_data = data
        draft.save(update_fields=["parsed_data"])
        audit(workspace=draft.workspace, actor=request.user, action="DOCX_IMPORT_PREVIEW_EDITED", instance=draft.source_asset, metadata={"importId": str(draft.id)})
        messages.success(request, "Import preview saved.")
        return redirect("studio:docx_import_detail", import_id=draft.id)
    return render(request, "studio/docx_import_detail.html", {"draft": draft, "can_edit": can_edit})


@login_required
def docx_import_accept(request, import_id):
    draft = get_object_or_404(DocxImport.objects.select_related("workspace").filter(workspace__in=accessible_workspaces(request.user)), id=import_id)
    if request.method != "POST":
        return redirect("studio:docx_import_detail", import_id=draft.id)
    if not has_capability(request.user, draft.workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    project = accept_docx_import(draft=draft, user=request.user)
    messages.success(request, "DOCX accepted. Review and edit the imported project.")
    return redirect("studio:project_detail", project_id=project.id)
@login_required
def project_master(request, project_id):
    project = get_object_or_404(
        Project.objects.select_related("workspace").prefetch_related(
            "assets", "characters__assets", "episodes__subtitle_tracks__lines",
            "episodes__scenes__assets", "episodes__scenes__dialogue_lines",
            "episodes__scenes__prompts__ai_model", "episodes__scenes__prompts__blocks",
            "episodes__scenes__prompts__assets",
            "episodes__scenes__additional_generations__outputs__asset",
        ).filter(id__in=accessible_projects(request.user)), id=project_id,
    )
    return render(request, "studio/project_master.html", {
        "project": project,
        **_project_header_context(request.user, project),
    })


@login_required
def entity_move(request, entity_type, entity_id, direction):
    if request.method != "POST" or direction not in {"up", "down"}:
        return HttpResponseForbidden("POST with a valid direction is required.")
    if entity_type == "character":
        item = get_object_or_404(Character.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)), id=entity_id)
        siblings, project, workspace = Character.objects.filter(project=item.project), item.project, item.project.workspace
    elif entity_type == "episode":
        item = get_object_or_404(Episode.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)), id=entity_id)
        siblings, project, workspace = Episode.objects.filter(project=item.project), item.project, item.project.workspace
    elif entity_type == "scene":
        item = get_object_or_404(Scene.objects.select_related("episode__project__workspace").filter(episode__project__in=accessible_projects(request.user)), id=entity_id)
        siblings, project, workspace = Scene.objects.filter(episode=item.episode), item.episode.project, item.episode.project.workspace
    elif entity_type == "dialogue":
        item = get_object_or_404(DialogueLine.objects.select_related("scene__episode__project__workspace").filter(scene__episode__project__in=accessible_projects(request.user)), id=entity_id)
        siblings, project, workspace = DialogueLine.objects.filter(scene=item.scene), item.scene.episode.project, item.scene.episode.project.workspace
    elif entity_type == "prompt":
        item = get_object_or_404(Prompt.objects.select_related("scene__episode__project__workspace").filter(scene__episode__project__in=accessible_projects(request.user)), id=entity_id)
        siblings, project, workspace = Prompt.objects.filter(scene=item.scene), item.scene.episode.project, item.scene.episode.project.workspace
    else:
        return HttpResponseForbidden("Unsupported entity type.")
    if not has_object_capability(request.user, item, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    ordered = list(siblings.order_by("position", "id"))
    index = next((value for value, sibling in enumerate(ordered) if sibling.id == item.id), None)
    other = None if index is None else index - 1 if direction == "up" else index + 1
    if other is not None and 0 <= other < len(ordered):
        ordered[index], ordered[other] = ordered[other], ordered[index]
        with transaction.atomic():
            temporary = max((sibling.position for sibling in ordered), default=0) + len(ordered) + 100
            for position, sibling in enumerate(ordered):
                sibling.position = temporary + position
                sibling.updated_by = request.user
                sibling.save(update_fields=["position", "updated_by", "updated_at"])
            for position, sibling in enumerate(ordered):
                sibling.position = position
                sibling.save(update_fields=["position", "updated_at"])
            record_revision(instance=item, user=request.user, operation="REORDER")
            audit(workspace=workspace, actor=request.user, action="ENTITY_REORDERED", instance=item, metadata={"direction": direction})
    return HttpResponseRedirect(reverse("studio:project_master", kwargs={"project_id": project.id}) + f"#item-{item.id}")

@login_required
def project_detail(request, project_id):
    project = get_object_or_404(
        accessible_projects(request.user).select_related("workspace", "created_by", "updated_by").prefetch_related("media_assets", "assets", "characters__avatar_asset", "characters__reference_assets", "episodes__scenes", "memberships__user"),
        id=project_id,
    )
    inherited = list(project.workspace.memberships.filter(status=WorkspaceMembership.Status.ACTIVE).exclude(user=project.workspace.owner).select_related("user"))
    excluded_ids = set(project.access_exclusions.values_list("user_id", flat=True))
    direct_ids = set(project.memberships.filter(is_active=True).values_list("user_id", flat=True))
    inherited = [membership for membership in inherited if membership.user_id not in excluded_ids and membership.user_id not in direct_ids]
    project_assets, workspace_assets = _picker_assets(request.user, project)
    recent_project_ids = {asset.id for asset in project_assets[:10]}
    gallery_assets = workspace_assets
    for asset in gallery_assets:
        asset.is_recent_project = asset.id in recent_project_ids
        asset.is_current_project = str(project.id) in asset.gallery_project_ids.split(",")
    return render(request, "studio/project_detail.html", {
        "project": project,
        "inherited_memberships": inherited,
        **_project_header_context(request.user, project),
        "mention_characters": list(project.characters.values_list("name", flat=True)),
        "project_assets": project_assets,
        "workspace_assets": workspace_assets,
        "gallery_assets": gallery_assets,
        "gallery_projects": [project],
    })


@login_required
def project_access(request, project_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    if not has_project_capability(request.user, project, "manage_project"):
        return HttpResponseForbidden("Full control permission is required.")
    form = ProjectMembershipForm(request.POST or None, project=project)
    if request.method == "POST":
        action = request.POST.get("action", "save")
        if action == "remove":
            membership = get_object_or_404(ProjectMembership, project=project, id=request.POST.get("membership_id"))
            if membership.user_id == project.workspace.owner_id:
                return HttpResponseForbidden("The workspace owner cannot be removed from a project.")
            membership.delete()
            messages.success(request, "Project access removed.")
            requested = request.POST.get("next", "")
            if requested and url_has_allowed_host_and_scheme(requested, {request.get_host()}, require_https=request.is_secure()):
                return HttpResponseRedirect(requested)
            return redirect("studio:project_access", project_id=project.id)
        if action == "exclude":
            user_id = request.POST.get("user_id")
            membership = get_object_or_404(WorkspaceMembership, workspace=project.workspace, user_id=user_id, status=WorkspaceMembership.Status.ACTIVE)
            if membership.user_id == project.workspace.owner_id or membership.role == WorkspaceMembership.Role.OWNER:
                return HttpResponseForbidden("The workspace owner cannot be removed from a project.")
            ProjectMembership.objects.filter(project=project, user=membership.user).delete()
            ProjectAccessExclusion.objects.update_or_create(project=project, user=membership.user, defaults={"revoked_by": request.user})
            messages.success(request, "Inherited access revoked for this project.")
            return redirect("studio:project_access", project_id=project.id)
        if form.is_valid():
            existed = ProjectMembership.objects.filter(project=project, user=form.user, is_active=True).exists()
            ProjectAccessExclusion.objects.filter(project=project, user=form.user).delete()
            ProjectMembership.objects.update_or_create(
                project=project,
                user=form.user,
                defaults={"role": form.cleaned_data["role"], "is_active": True, "invited_by": request.user},
            )
            if not existed:
                notify_access_granted(user=form.user, entity_name=project.title, entity_kind="project", url=request.build_absolute_uri(reverse("studio:project_detail", kwargs={"project_id": project.id})), granted_by=request.user)
            messages.success(request, "Project access and server AI access enabled.")
            return redirect("studio:project_access", project_id=project.id)
    inherited = project.workspace.memberships.filter(status=WorkspaceMembership.Status.ACTIVE).exclude(user=project.workspace.owner).exclude(user_id__in=project.memberships.values("user_id")).exclude(user_id__in=project.access_exclusions.values("user_id")).select_related("user")
    return render(request, "studio/project_access.html", {
        "project": project,
        "form": form,
        "memberships": project.memberships.select_related("user"),
        "inherited_memberships": inherited,
    })


@login_required
def scene_detail(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").prefetch_related(
            "dialogue_lines", "prompts__ai_model", "prompts__template", "prompts__blocks__source_dialogue",
            "prompts__assets", "prompts__reference_assets", "prompts__ai_suggestions", "assets", "reference_assets"
        ).filter(episode__project__in=accessible_projects(request.user)),
        id=scene_id,
    )
    workspace = scene.episode.project.workspace
    can_edit = has_object_capability(request.user, scene, "edit")
    form = SceneForm(request.POST or None, instance=scene)
    if request.method == "POST":
        if not can_edit:
            return HttpResponseForbidden("Edit permission is required.")
        if form.is_valid():
            scene = form.save(commit=False)
            scene.updated_by = request.user
            scene.save()
            record_revision(instance=scene, user=request.user, operation="INLINE_UPDATE")
            audit(workspace=workspace, actor=request.user, action="SCENE_INLINE_UPDATED", instance=scene)
            messages.success(request, f"Scene {scene.number} saved.")
            focus = request.POST.get("navigation_focus", "top")
            return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": scene.id}) + f"?focus={focus}#scene-navigation-{focus}")
    episode_scenes = list(scene.episode.scenes.order_by("position", "number", "id"))
    scene_index = next(index for index, item in enumerate(episode_scenes) if item.id == scene.id)
    editor_prompts = [prompt for prompt in scene.prompts.all() if not prompt.source_prompt_id]
    _attach_prompt_ai_state(editor_prompts)
    project_assets, workspace_assets = _picker_assets(request.user, scene.episode.project)
    return render(request, "studio/scene_detail.html", {
        "scene": scene,
        "scene_form": form,
        "episode_scenes": episode_scenes,
        "previous_scene": episode_scenes[scene_index - 1] if scene_index else None,
        "next_scene": episode_scenes[scene_index + 1] if scene_index + 1 < len(episode_scenes) else None,
        "editor_prompts": editor_prompts,
        "can_edit": can_edit,
        "can_use_ai": has_object_capability(request.user, scene, "use_ai") and user_has_ai_access(request.user),
        "scene_images": scene.reference_assets.filter(prompt__isnull=True),
        "project_assets": project_assets,
        "workspace_assets": workspace_assets,
        "mention_characters": list(scene.episode.project.characters.values_list("name", flat=True)),
        **_prompt_editor_context(request),
    })

@login_required
def prompt_detail(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model", "template")
        .prefetch_related("scene__dialogue_lines", "blocks__source_dialogue", "assets", "ai_suggestions")
        .filter(scene__episode__project__in=accessible_projects(request.user)),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    _attach_prompt_ai_state([prompt])
    return render(request, "studio/prompt_detail.html", {
        "prompt": prompt,
        "can_edit": has_object_capability(request.user, prompt, "edit"),
        "can_use_ai": has_object_capability(request.user, prompt, "use_ai") and user_has_ai_access(request.user),
        **_prompt_editor_context(request),
    })


@login_required
def prompt_improve(request, prompt_id):
    if request.method != "POST":
        return redirect("studio:prompt_detail", prompt_id=prompt_id)
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model", "template")
        .prefetch_related("blocks")
        .filter(scene__episode__project__in=accessible_projects(request.user)),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    if not has_object_capability(request.user, prompt, "use_ai") or not user_has_ai_access(request.user):
        messages.error(request, "AI access is not enabled for this account and workspace.")
        return _prompt_action_redirect(request, prompt)
    try:
        model_id = selected_text_model(request.POST.get("text_model"))
        improve_prompt(
            prompt=prompt, user=request.user, mode="improve_translate_en",
            selected_block_ids=request.POST.getlist("selected_blocks"),
            text_model=model_id,
        )
    except (StudioAiError, ProviderError, ValidationError) as exc:
        messages.error(request, str(exc))
    else:
        messages.success(request, "AI suggestion is ready for review.")
    return _prompt_action_redirect(request, prompt)


@login_required
def prompt_translate_dialogue(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model", "template")
        .prefetch_related("blocks__source_dialogue")
        .filter(scene__episode__project__in=accessible_projects(request.user)),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    if request.method != "POST" or not has_object_capability(request.user, prompt, "use_ai") or not user_has_ai_access(request.user):
        messages.error(request, "AI access is not enabled for this account and workspace.")
        return _prompt_action_redirect(request, prompt)
    try:
        model_id = selected_text_model(request.POST.get("text_model"))
        translated_prompt, count, _ = translate_prompt_dialogue(
            prompt=prompt,
            user=request.user,
            target_language=request.POST.get("target_language"),
            text_model=model_id,
        )
    except (StudioAiError, ProviderError, ValidationError) as exc:
        messages.error(request, str(exc))
    else:
        messages.success(request, f"Created {translated_prompt.language} prompt with {count} translated dialogue block(s).")
        return _prompt_action_redirect(request, translated_prompt)
    return _prompt_action_redirect(request, prompt)


@login_required
def prompt_ai_action(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model", "template")
        .prefetch_related("blocks__source_dialogue")
        .filter(scene__episode__project__in=accessible_projects(request.user)),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    if request.method != "POST" or not has_object_capability(request.user, prompt, "use_ai") or not user_has_ai_access(request.user):
        messages.error(request, "AI access is not enabled for this account and workspace.")
        return _prompt_action_redirect(request, prompt)
    action = request.POST.get("action")
    try:
        model_id = selected_text_model(request.POST.get("text_model"))
        if action == "improve":
            improve_prompt(
                prompt=prompt, user=request.user, mode="improve_translate_en",
                selected_block_ids=[], text_model=model_id,
            )
            messages.success(request, "Improved version is ready. Apply it or cancel without changing the prompt.")
            return _prompt_action_redirect(request, prompt)
        scope = {
            "translate_all": Prompt.TranslationScope.FULL,
            "translate_dialogue": Prompt.TranslationScope.DIALOGUE,
        }.get(action)
        if scope is None:
            raise ValidationError("Choose an AI action.")
        translated_prompt, count, _ = translate_prompt(
            prompt=prompt, user=request.user, target_language=request.POST.get("target_language"),
            text_model=model_id, scope=scope,
        )
    except (StudioAiError, ProviderError, ValidationError) as exc:
        messages.error(request, str(exc))
        return _prompt_action_redirect(request, prompt)
    messages.success(request, f"Created a new {translated_prompt.language} prompt. Translated blocks: {count}.")
    return _prompt_action_redirect(request, translated_prompt)


@login_required
def prompt_editor_data(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model")
        .prefetch_related("blocks")
        .filter(scene__episode__project__in=accessible_projects(request.user)),
        id=prompt_id,
    )
    return JsonResponse(_prompt_payload(prompt))


@login_required
def prompt_ai_preview(request, prompt_id):
    prompt = Prompt.objects.select_related("scene__episode__project__workspace", "ai_model").filter(id=prompt_id).first()
    if prompt is None:
        return JsonResponse({"error": "Prompt no longer exists. Refresh the scene."}, status=404)
    if not has_project_capability(request.user, prompt.scene.episode.project, "view"):
        return JsonResponse({"error": "You no longer have access to this project."}, status=403)
    if request.method != "POST" or not has_object_capability(request.user, prompt, "use_ai") or not user_has_ai_access(request.user):
        return JsonResponse({"error": "AI access is not enabled for this account."}, status=403)
    try:
        data = json.loads(request.body or b"{}")
        model_id = selected_text_model(data.get("textModel"))
        scope = str(data.get("scope", Prompt.TranslationScope.FULL)).upper()
        content, used_model = preview_prompt_translation(
            prompt=prompt,
            user=request.user,
            content=data.get("content", ""),
            target_language=data.get("targetLanguage"),
            text_model=model_id,
            scope=scope,
            selection_start=data.get("selectionStart"),
            selection_end=data.get("selectionEnd"),
            improve=data.get("action") == "improve_translation",
        )
    except (json.JSONDecodeError, StudioAiError, ProviderError, ValidationError) as exc:
        return JsonResponse({"error": str(exc)}, status=400)
    except Exception:
        return JsonResponse({"error": "The translation service returned an unexpected error. Please retry."}, status=500)
    return JsonResponse({
        "content": content,
        "language": str(data.get("targetLanguage", "")).upper(),
        "scope": scope,
        "model": used_model,
    })


@login_required
def prompt_apply_translation(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("source_prompt", "scene__episode__project__workspace", "ai_model", "template")
        .filter(scene__episode__project__in=accessible_projects(request.user)),
        id=prompt_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, prompt, "edit"):
        return JsonResponse({"error": "Edit permission is required."}, status=403)
    try:
        data = json.loads(request.body or b"{}")
    except json.JSONDecodeError:
        return JsonResponse({"error": "Invalid request."}, status=400)
    target = str(data.get("targetLanguage", "")).strip().upper()
    scope = str(data.get("scope", Prompt.TranslationScope.FULL)).upper()
    content = str(data.get("content", "")).strip()
    if target not in Prompt.Language.values or scope not in Prompt.TranslationScope.values or not content:
        return JsonResponse({"error": "Language, scope, and translated text are required."}, status=400)
    root = prompt.source_prompt or prompt
    with transaction.atomic():
        if target == root.language:
            version = root
            version.translation_scope = Prompt.TranslationScope.ORIGINAL
        else:
            version = root.translations.filter(language=target).order_by("created_at").first()
            if version is None:
                version = Prompt.objects.create(
                    scene=root.scene,
                    ai_model=root.ai_model,
                    template=root.template,
                    source_prompt=root,
                    language=target,
                    translation_scope=scope,
                    prompt_type=root.prompt_type,
                    title=f"{root.title or root.get_prompt_type_display()} [{target}]",
                    status=Prompt.Status.DRAFT,
                    position=root.scene.prompts.count(),
                    content=content,
                    created_by=request.user,
                    updated_by=request.user,
                )
            else:
                version.translation_scope = scope
        version.translation_scope = Prompt.TranslationScope.ORIGINAL if version == root else scope
        version.updated_by = request.user
        version.save(update_fields=["translation_scope", "updated_by", "updated_at"])
        _save_unified_prompt_content(version, content, request.user, operation="AI_TRANSLATION_APPLY")
        audit(
            workspace=root.scene.episode.project.workspace,
            actor=request.user,
            action="PROMPT_TRANSLATION_APPLIED",
            instance=version,
            metadata={"sourcePromptId": str(root.id), "targetLanguage": target, "scope": scope},
        )
    return JsonResponse(_prompt_payload(version))


def _prompt_action_redirect(request, prompt):
    return_to = request.POST.get("return_to")
    if return_to == "chain":
        return HttpResponseRedirect(
            reverse("studio:project_scene_chain", kwargs={"project_id": prompt.scene.episode.project_id})
            + f"#prompt-{prompt.id}"
        )
    if return_to == "scene":
        return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": prompt.scene_id}) + f"#prompt-{prompt.id}")
    return redirect("studio:prompt_detail", prompt_id=prompt.id)


@login_required
def suggestion_decide(request, suggestion_id, decision):
    suggestion = get_object_or_404(
        accessible_suggestions(request.user).select_related("workspace", "prompt"),
        id=suggestion_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, suggestion, "edit"):
        messages.error(request, "Edit permission is required.")
        return _prompt_action_redirect(request, suggestion.prompt)
    try:
        if decision == "accept":
            accept_suggestion(suggestion=suggestion, user=request.user)
        elif decision == "undo":
            undo_suggestion(suggestion=suggestion, user=request.user)
        else:
            reject_suggestion(suggestion=suggestion, user=request.user)
    except StudioAiError as exc:
        messages.error(request, str(exc))
    else:
        messages.success(request, {"accept": "Improvement applied.", "undo": "Previous prompt restored."}.get(decision, "Improvement cancelled."))
    return _prompt_action_redirect(request, suggestion.prompt)

@login_required
def translation_workspace(request, project_id):
    project = get_object_or_404(accessible_projects(request.user), id=project_id)
    workspace = project.workspace
    filter_key = f"studio_translation_filters_{project.id}"
    saved_filters = request.session.get(filter_key, {})
    target_language = (request.POST.get("target_language") or request.GET.get("target_language") or saved_filters.get("target_language") or "en").strip().lower()[:16]
    scene_id = request.GET.get("scene", saved_filters.get("scene", "")).strip()
    prompt_language = request.GET.get("prompt_language", saved_filters.get("prompt_language", "")).strip().upper()
    sort_direction = request.GET.get("sort", saved_filters.get("sort", "asc"))
    if request.method == "GET":
        request.session[filter_key] = {
            "target_language": target_language,
            "scene": scene_id,
            "prompt_language": prompt_language,
            "sort": sort_direction,
        }
    if request.method == "POST" and request.POST.get("line_id"):
        if not has_project_capability(request.user, project, "translate"):
            messages.error(request, "Translation permission is required.")
        else:
            line = get_object_or_404(DialogueLine.objects.filter(scene__episode__project=project), id=request.POST["line_id"])
            status = request.POST.get("status", TranslationUnit.Status.DRAFT)
            if status not in {TranslationUnit.Status.DRAFT, TranslationUnit.Status.IN_REVIEW, TranslationUnit.Status.APPROVED}:
                status = TranslationUnit.Status.DRAFT
            save_translation(
                dialogue_line=line, user=request.user, target_language=target_language,
                translated_text=request.POST.get("translation", "").strip(), status=status,
            )
            messages.success(request, "Translation saved.")
        return redirect(f"{request.path}?target_language={target_language}")
    lines = DialogueLine.objects.filter(scene__episode__project=project).select_related("scene__episode").prefetch_related("translations")
    if scene_id:
        lines = lines.filter(scene_id=scene_id)
    order_prefix = "-" if sort_direction == "desc" else ""
    lines = lines.order_by(f"{order_prefix}scene__episode__position", f"{order_prefix}scene__position", f"{order_prefix}position")
    rows = []
    for line in lines:
        unit = next((item for item in line.translations.all() if item.target_language == target_language), None)
        rows.append({"line": line, "unit": unit})
    original_prompts = Prompt.objects.filter(scene__episode__project=project, source_prompt__isnull=True).select_related("scene__episode", "ai_model").prefetch_related("translations")
    if scene_id:
        original_prompts = original_prompts.filter(scene_id=scene_id)
    original_prompts = original_prompts.order_by(f"{order_prefix}scene__episode__position", f"{order_prefix}scene__position", f"{order_prefix}position")
    prompt_rows = []
    available_prompt_languages = set()
    for prompt in original_prompts:
        versions = list(prompt.translations.select_related("ai_model").order_by("language", "created_at"))
        available_prompt_languages.update(version.language for version in versions if version.language)
        if prompt_language:
            versions = [version for version in versions if version.language == prompt_language]
            if not versions:
                continue
        prompt_rows.append({"prompt": prompt, "versions": versions})
    return render(request, "studio/translations.html", {
        "project": project, "rows": rows, "target_language": target_language,
        "prompt_rows": prompt_rows, "scenes": Scene.objects.filter(episode__project=project).select_related("episode").order_by("episode__position", "position"),
        "scene_id": scene_id, "prompt_language": prompt_language, "available_prompt_languages": sorted(available_prompt_languages), "sort_direction": sort_direction,
        "can_translate": has_project_capability(request.user, project, "translate"),
        "can_use_ai": has_project_capability(request.user, project, "use_ai") and user_has_ai_access(request.user),
        "prompt_languages": PROMPT_LANGUAGES,
        "text_models": active_text_models(),
        "default_text_model": default_text_model_id(),
        "translation_statuses": [TranslationUnit.Status.DRAFT, TranslationUnit.Status.IN_REVIEW, TranslationUnit.Status.APPROVED],
        **_project_header_context(request.user, project),
    })


@login_required
def episode_subtitles(request, episode_id):
    episode = get_object_or_404(
        Episode.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)),
        id=episode_id,
    )
    if request.method == "POST" and has_project_capability(request.user, episode.project, "translate"):
        language = request.POST.get("language", "").strip().lower()[:16]
        kind = request.POST.get("kind", SubtitleTrack.Kind.WORKING)
        if language and kind in SubtitleTrack.Kind.values:
            track, _ = SubtitleTrack.objects.get_or_create(
                episode=episode, language=language, kind=kind,
                defaults={"created_by": request.user, "updated_by": request.user},
            )
            return redirect("studio:subtitle_track", track_id=track.id)
    return render(request, "studio/episode_subtitles.html", {
        "episode": episode,
        "can_translate": has_project_capability(request.user, episode.project, "translate"),
    })


@login_required
def subtitle_track(request, track_id):
    track = get_object_or_404(
        SubtitleTrack.objects.select_related("episode__project__workspace").filter(
            episode__project__in=accessible_projects(request.user)
        ),
        id=track_id,
    )
    can_translate = has_project_capability(request.user, track.episode.project, "translate")
    if request.method == "POST" and can_translate:
        action = request.POST.get("action", "bulk")
        if action == "bulk":
            bulk_replace_subtitle_lines(track=track, user=request.user, texts=request.POST.get("lines", "").splitlines())
        elif action in {"up", "down"}:
            lines = list(track.lines.all())
            selected = next((index for index, line in enumerate(lines) if str(line.id) == request.POST.get("line_id")), None)
            if selected is not None:
                other = selected - 1 if action == "up" else selected + 1
                if 0 <= other < len(lines):
                    lines[selected], lines[other] = lines[other], lines[selected]
                    reorder_subtitle_lines(track=track, user=request.user, ordered_ids=[line.id for line in lines])
        return redirect("studio:subtitle_track", track_id=track.id)
    return render(request, "studio/subtitle_track.html", {"track": track, "can_translate": can_translate})

@login_required
def project_exports(request, project_id):
    project = get_object_or_404(
        Project.objects.prefetch_related(
            "characters", "episodes__scenes__dialogue_lines", "episodes__scenes__prompts__blocks",
            "episodes__scenes__prompts__assets",
            "episodes__scenes__additional_generations__outputs__asset", "episodes__subtitle_tracks__lines",
        ).filter(id__in=accessible_projects(request.user)),
        id=project_id,
    )
    can_export = has_project_capability(request.user, project, "export")
    if request.method == "POST" and can_export:
        episode = None
        if request.POST.get("episode_id"):
            episode = get_object_or_404(Episode.objects.filter(project=project), id=request.POST["episode_id"])
        export_format = request.POST.get("format", "pdf")
        try:
            if export_format == "docx":
                job = generate_docx_export(project=project, user=request.user)
            else:
                generate_export(
                    project=project, episode=episode,
                    sections=request.POST.getlist("sections"), user=request.user,
                )
        except ExportError as exc:
            messages.error(request, str(exc))
        except Exception:
            messages.error(request, f"{export_format.upper()} generation failed.")
        else:
            messages.success(request, f"{export_format.upper()} export is ready.")
            if export_format == "docx":
                return redirect("studio:docx_roundtrip", job_id=job.id)
        return redirect("studio:project_exports", project_id=project.id)
    return render(request, "studio/exports.html", {
        "project": project, "jobs": project.export_jobs.select_related("episode", "output_asset")[:50],
        "sections": ALL_SECTIONS, "can_export": can_export,
    })


@login_required
def scene_media(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").prefetch_related(
            "assets", "additional_generations__source_asset", "additional_generations__outputs__asset"
        ).filter(episode__project__in=accessible_projects(request.user)),
        id=scene_id,
    )
    workspace = scene.episode.project.workspace
    return render(request, "studio/scene_media.html", {
        "scene": scene,
        "can_edit": has_object_capability(request.user, scene, "edit"),
    })


@login_required
def generation_create(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__in=accessible_projects(request.user)
        ),
        id=scene_id,
    )
    workspace = scene.episode.project.workspace
    if not has_object_capability(request.user, scene, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = AdditionalGenerationForm(request.POST or None, project=scene.episode.project)
    if request.method == "POST" and form.is_valid():
        with transaction.atomic():
            episode = scene.episode
            sequence = Scene.objects.filter(episode=episode, title__startswith="Догенерация ").count() + 1
            last_number = max(episode.scenes.values_list("number", flat=True), default=0)
            generated_scene = Scene.objects.create(
                episode=episode, number=last_number + 1, title=f"Догенерация {sequence}",
                hook=f"Additional generation based on scene {scene.number}", description=form.cleaned_data["reason"],
                position=episode.scenes.count(), status=Scene.Status.DRAFT,
                created_by=request.user, updated_by=request.user,
            )
            item = form.save(commit=False)
            item.scene = generated_scene
            item.position = 0
            item.created_by = request.user
            item.updated_by = request.user
            item.save()
        record_revision(instance=item, user=request.user, operation="CREATE")
        audit(workspace=workspace, actor=request.user, action="GENERATION_CREATED", instance=item)
        messages.success(request, f"{generated_scene.title} appended to the end of the episode.")
        return redirect("studio:scene_media", scene_id=generated_scene.id)
    return render(request, "studio/entity_form.html", {
        "form": form, "title": "New additional generation", "submit_label": "Create generation",
    })


@login_required
def generation_edit(request, generation_id):
    item = get_object_or_404(
        AdditionalGeneration.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__in=accessible_projects(request.user)
        ),
        id=generation_id,
    )
    workspace = item.scene.episode.project.workspace
    if not has_object_capability(request.user, item, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = AdditionalGenerationForm(
        request.POST or None, instance=item, project=item.scene.episode.project
    )
    if request.method == "POST" and form.is_valid():
        item = form.save(commit=False)
        item.updated_by = request.user
        item.save()
        record_revision(instance=item, user=request.user, operation="UPDATE")
        audit(workspace=workspace, actor=request.user, action="GENERATION_UPDATED", instance=item)
        messages.success(request, "Additional generation saved.")
        return redirect("studio:scene_media", scene_id=item.scene_id)
    return render(request, "studio/entity_form.html", {
        "form": form, "title": "Edit additional generation", "submit_label": "Save changes",
    })


@login_required
def generation_output_upload(request, generation_id):
    generation = get_object_or_404(
        AdditionalGeneration.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__in=accessible_projects(request.user)
        ),
        id=generation_id,
    )
    scene = generation.scene
    workspace = scene.episode.project.workspace
    if not has_object_capability(request.user, generation, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = GenerationOutputUploadForm(request.POST or None, request.FILES or None)
    if request.method == "POST" and form.is_valid():
        try:
            asset = create_asset(
                user=request.user,
                workspace=workspace,
                uploaded=form.cleaned_data["file"],
                kind=Asset.Kind.GENERATION_OUTPUT,
                project=scene.episode.project,
                scene=scene,
            )
        except ValidationError as exc:
            form.add_error("file", exc)
        else:
            output = GenerationOutput.objects.create(
                generation=generation,
                asset=asset,
                model_metadata={"model": form.cleaned_data["model_name"]} if form.cleaned_data["model_name"] else {},
                position=generation.outputs.count(),
                created_by=request.user,
                updated_by=request.user,
            )
            generation.status = AdditionalGeneration.Status.IN_REVIEW
            generation.updated_by = request.user
            generation.save(update_fields=["status", "updated_by", "updated_at"])
            audit(
                workspace=workspace,
                actor=request.user,
                action="GENERATION_OUTPUT_UPLOADED",
                instance=generation,
                metadata={"outputId": str(output.id), "assetId": str(asset.id)},
            )
            messages.success(request, "Generation result uploaded.")
            return redirect("studio:scene_media", scene_id=scene.id)
    return render(request, "studio/entity_form.html", {
        "form": form,
        "title": "Upload generation result",
        "submit_label": "Upload result",
        "multipart": True,
    })


@login_required
def generation_output_final_web(request, output_id):
    if request.method != "POST":
        return HttpResponseForbidden("POST is required.")
    output = get_object_or_404(
        GenerationOutput.objects.select_related(
            "generation__scene__episode__project__workspace", "asset"
        ).filter(generation__scene__episode__project__in=accessible_projects(request.user)),
        id=output_id,
    )
    generation = output.generation
    workspace = generation.scene.episode.project.workspace
    if not has_object_capability(request.user, output, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    with transaction.atomic():
        generation.outputs.update(is_final=False)
        output.is_final = True
        output.updated_by = request.user
        output.save(update_fields=["is_final", "updated_by", "updated_at"])
        generation.status = AdditionalGeneration.Status.FINAL
        generation.updated_by = request.user
        generation.save(update_fields=["status", "updated_by", "updated_at"])
        record_revision(instance=generation, user=request.user, operation="FINAL_OUTPUT")
        audit(
            workspace=workspace,
            actor=request.user,
            action="GENERATION_OUTPUT_SELECTED",
            instance=generation,
            metadata={"outputId": str(output.id), "assetId": str(output.asset_id)},
        )
    messages.success(request, "Final generation result selected.")
    return HttpResponseRedirect(
        reverse("studio:scene_media", kwargs={"scene_id": generation.scene_id})
        + f"#generation-{generation.id}"
    )

@login_required
def asset_edit(request, asset_id):
    asset = get_object_or_404(
        accessible_assets(request.user).select_related("workspace", "project", "scene"),
        id=asset_id,
    )
    if not has_object_capability(request.user, asset, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = AssetEditForm(request.POST or None, instance=asset)
    if request.method == "POST" and form.is_valid():
        asset = form.save(commit=False)
        asset.updated_by = request.user
        asset.save(update_fields=["original_filename", "kind", "updated_by", "updated_at"])
        audit(
            workspace=asset.workspace,
            actor=request.user,
            action="ASSET_METADATA_UPDATED",
            instance=asset,
            metadata={"filename": asset.original_filename, "kind": asset.kind},
        )
        messages.success(request, "Image details saved.")
        if asset.scene_id:
            return redirect("studio:scene_media", scene_id=asset.scene_id)
        if asset.project_id:
            return redirect("studio:project_detail", project_id=asset.project_id)
        return redirect("studio:dashboard")
    return render(request, "studio/entity_form.html", {
        "form": form, "title": "Edit image details", "submit_label": "Save changes",
    })


def _asset_action_redirect(request, asset):
    requested = request.POST.get("next", "")
    if requested and url_has_allowed_host_and_scheme(
        requested,
        allowed_hosts={request.get_host()},
        require_https=request.is_secure(),
    ):
        return HttpResponseRedirect(requested)
    if asset.project_id:
        return redirect("studio:project_detail", project_id=asset.project_id)
    return redirect("studio:dashboard")


def _asset_for_edit(request, asset_id, include_deleted=False):
    return get_object_or_404(
        accessible_assets(request.user, include_deleted=include_deleted)
        .select_related("workspace", "project", "scene", "character", "prompt")
        .filter(content_type__startswith="image/"),
        id=asset_id,
    )


def _asset_usage_project_ids(asset):
    project_ids = {project.id for project in asset.projects.all()}
    project_ids.update(scene.episode.project_id for scene in asset.referenced_by_scenes.all())
    project_ids.update(character.project_id for character in asset.referenced_by_characters.all())
    project_ids.update(prompt.scene.episode.project_id for prompt in asset.referenced_by_prompts.all())
    project_ids.update(project.id for project in asset.project_cover_for.all())
    project_ids.update(character.project_id for character in asset.character_avatar_for.all())
    return {value for value in project_ids if value}


def _asset_usage_count(asset):
    return (
        len(asset.projects.all())
        + len(asset.referenced_by_scenes.all())
        + len(asset.referenced_by_characters.all())
        + len(asset.referenced_by_prompts.all())
        + len(asset.project_cover_for.all())
        + len(asset.character_avatar_for.all())
    )


def _decorate_gallery_assets(user, assets, *, deduplicate=False):
    accessible_ids = set(accessible_projects(user).values_list("id", flat=True))
    for asset in assets:
        usage_project_ids = _asset_usage_project_ids(asset)
        asset._gallery_project_id_set = usage_project_ids
        asset.gallery_project_ids = ",".join(str(value) for value in sorted(usage_project_ids, key=str))
        asset.usage_count = _asset_usage_count(asset)
        asset.requires_usage_confirmation = asset.usage_count > 1
        asset.can_trash_from_workspace = usage_project_ids.issubset(accessible_ids)
    if not deduplicate:
        return assets

    groups = {}
    for asset in assets:
        key = asset.checksum_sha256 or str(asset.id)
        group = groups.setdefault(key, [])
        group.append(asset)

    result = []
    for group in groups.values():
        original = min(group, key=lambda item: (item.created_at, str(item.id)))
        project_ids = set().union(*(item._gallery_project_id_set for item in group))
        original.gallery_project_ids = ",".join(str(value) for value in sorted(project_ids, key=str))
        original.usage_count = sum(item.usage_count for item in group)
        original.requires_usage_confirmation = original.usage_count > 1
        original.can_trash_from_workspace = all(item.can_trash_from_workspace for item in group)
        result.append(original)
    return result


@login_required
def asset_trash(request, asset_id):
    asset = _asset_for_edit(request, asset_id)
    if request.method != "POST" or request.POST.get("workspace_delete") != "1" or not has_capability(request.user, asset.workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    usage_project_ids = _asset_usage_project_ids(asset)
    accessible_ids = set(accessible_projects(request.user).filter(id__in=usage_project_ids).values_list("id", flat=True))
    if accessible_ids != usage_project_ids:
        return HttpResponseForbidden("This image is used by a project you cannot access and cannot be deleted.")
    if _asset_usage_count(asset) > 1 and request.POST.get("confirm_usage") != "1":
        messages.warning(request, "This image is used multiple times or by several projects. Confirm deletion to continue.")
        return _asset_action_redirect(request, asset)
    try:
        trash_asset(asset=asset, user=request.user)
    except ValidationError as exc:
        messages.error(request, "; ".join(exc.messages))
    else:
        Project.all_objects.filter(cover_asset=asset).update(cover_asset=None)
        messages.success(request, "Image moved to trash.")
    return _asset_action_redirect(request, asset)


@login_required
def asset_crop(request, asset_id):
    asset = _asset_for_edit(request, asset_id)
    if request.method != "POST" or not has_object_capability(request.user, asset, "edit"):
        return JsonResponse({"error": "Edit permission is required."}, status=403)
    try:
        data = json.loads(request.body or b"{}")
        cropped = crop_asset(
            asset=asset,
            user=request.user,
            x=data.get("x"),
            y=data.get("y"),
            width=data.get("width"),
            height=data.get("height"),
        )
    except (json.JSONDecodeError, ValidationError) as exc:
        message = "; ".join(exc.messages) if isinstance(exc, ValidationError) else "Invalid crop request."
        return JsonResponse({"error": message}, status=400)
    return JsonResponse({
        "id": str(cropped.id),
        "viewUrl": reverse("studio_api:asset_view", kwargs={"asset_id": cropped.id}),
        "thumbnailUrl": reverse("studio_api:asset_thumbnail", kwargs={"asset_id": cropped.id}),
    }, status=201)


@login_required
def asset_detach(request, asset_id, scope, owner_id):
    asset = _asset_for_edit(request, asset_id)
    if request.method != "POST":
        return HttpResponseForbidden("Edit permission is required.")
    detached = False
    if scope == "scene":
        scene = get_object_or_404(Scene.objects.filter(episode__project__in=accessible_projects(request.user)), id=owner_id)
        if not has_object_capability(request.user, scene, "edit"):
            return HttpResponseForbidden("Edit permission is required.")
        if scene.reference_assets.filter(id=asset.id).exists():
            scene.reference_assets.remove(asset)
            detached = True
        if asset.scene_id == scene.id:
            asset.scene = None
            asset.kind = Asset.Kind.OTHER
            detached = True
    elif scope == "prompt":
        prompt = get_object_or_404(Prompt.objects.filter(scene__episode__project__in=accessible_projects(request.user)), id=owner_id)
        if not has_object_capability(request.user, prompt, "edit"):
            return HttpResponseForbidden("Edit permission is required.")
        if asset.prompt_id == prompt.id:
            asset.prompt = None
            detached = True
        if prompt.reference_assets.filter(id=asset.id).exists():
            prompt.reference_assets.remove(asset)
            detached = True
    elif scope == "character":
        character = get_object_or_404(Character.objects.filter(project__in=accessible_projects(request.user)), id=owner_id)
        if not has_object_capability(request.user, character, "edit"):
            return HttpResponseForbidden("Edit permission is required.")
        if character.reference_assets.filter(id=asset.id).exists():
            character.reference_assets.remove(asset)
            detached = True
        if asset.character_id == character.id:
            asset.character = None
            asset.kind = Asset.Kind.OTHER
            detached = True
    elif scope == "project":
        project = get_object_or_404(accessible_projects(request.user), id=owner_id)
        if not has_project_capability(request.user, project, "edit"):
            return HttpResponseForbidden("Edit permission is required.")
        if asset.projects.filter(id=project.id).exists() or asset.project_id == project.id:
            for scene in Scene.objects.filter(episode__project=project, reference_assets=asset):
                scene.reference_assets.remove(asset)
            for character in Character.objects.filter(project=project, reference_assets=asset):
                character.reference_assets.remove(asset)
            for prompt in Prompt.objects.filter(scene__episode__project=project, reference_assets=asset):
                prompt.reference_assets.remove(asset)
            if project.cover_asset_id == asset.id:
                project.cover_asset = None
                project.updated_by = request.user
                project.save(update_fields=["cover_asset", "updated_by", "updated_at"])
            asset.projects.remove(project)
            if asset.project_id == project.id:
                asset.project = None
            if asset.scene_id and asset.scene.episode.project_id == project.id:
                asset.scene = None
            if asset.character_id and asset.character.project_id == project.id:
                asset.character = None
            if asset.prompt_id and asset.prompt.scene.episode.project_id == project.id:
                asset.prompt = None
            asset.kind = Asset.Kind.OTHER
            detached = True
    if not detached:
        return HttpResponseForbidden("This image is not attached here.")
    asset.updated_by = request.user
    asset.save(update_fields=["project", "scene", "prompt", "character", "kind", "updated_by", "updated_at"])
    audit(workspace=asset.workspace, actor=request.user, action="ASSET_DETACHED", instance=asset, metadata={"scope": scope, "ownerId": str(owner_id)})
    messages.success(request, "Image detached. It remains available in the Workspace library.")
    return _asset_action_redirect(request, asset)


@login_required
def asset_restore(request, asset_id):
    asset = _asset_for_edit(request, asset_id, include_deleted=True)
    if request.method != "POST" or not has_object_capability(request.user, asset, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    try:
        restore_asset(asset=asset, user=request.user)
    except ValidationError as exc:
        messages.error(request, "; ".join(exc.messages))
    else:
        messages.success(request, "Image restored.")
    return _asset_action_redirect(request, asset)


@login_required
def asset_purge(request, asset_id):
    asset = _asset_for_edit(request, asset_id, include_deleted=True)
    if request.method != "POST" or not has_object_capability(request.user, asset, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    try:
        purge_asset(asset=asset, user=request.user)
    except ValidationError as exc:
        messages.error(request, "; ".join(exc.messages))
    else:
        messages.success(request, "Image file permanently deleted.")
    return _asset_action_redirect(request, asset)


@login_required
def project_image_trash(request, project_id):
    project = get_object_or_404(
        accessible_projects(request.user).select_related("workspace"),
        id=project_id,
    )
    messages.info(request, "Image deletion is managed only from the Workspace Recycle Bin.")
    return redirect("studio:workspace_recycle_bin", workspace_id=project.workspace_id)


@login_required
def workspace_recycle_bin(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    assets = Asset.all_objects.filter(
        workspace=workspace, content_type__startswith="image/", deleted_at__isnull=False, purged_at__isnull=True,
    ).select_related("project", "scene", "character").order_by("-deleted_at")
    return render(request, "studio/workspace_recycle_bin.html", {
        "workspace": workspace, "assets": assets,
        "can_edit": has_capability(request.user, workspace, "edit"),
    })


@login_required
def dialogue_translation_preview(request, line_id):
    line = DialogueLine.objects.select_related("scene__episode__project__workspace").filter(id=line_id).first()
    if line is None:
        return JsonResponse({"error": "Dialogue line no longer exists."}, status=404)
    project = line.scene.episode.project
    if request.method != "POST" or not has_project_capability(request.user, project, "use_ai") or not user_has_ai_access(request.user):
        return JsonResponse({"error": "AI access is not enabled for this account."}, status=403)
    try:
        data = json.loads(request.body or b"{}")
        target = str(data.get("targetLanguage", "")).strip().upper()
        if target not in Prompt.Language.values:
            raise ValidationError("Choose a supported target language.")
        model = selected_text_model(data.get("textModel"))
        instruction = json.dumps({
            "task": f"Translate this dialogue into {target}.",
            "rules": ["Return JSON only with shape {\"content\": \"...\"}.", "Preserve meaning, names, tone, punctuation and speaker intent.", "Do not add explanations."],
            "source_language": (line.language or project.original_language or "").upper(),
            "content": line.text,
        }, ensure_ascii=False)
        raw, used_model = run_text(model, instruction)
        clean = raw.strip()
        if clean.startswith("```"):
            clean = clean.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
        parsed = json.loads(clean)
        content = str(parsed.get("content", "")).strip()
        if not content:
            raise ValidationError("AI returned an empty translation.")
    except (json.JSONDecodeError, ProviderError, ValidationError) as exc:
        return JsonResponse({"error": str(exc)}, status=400)
    except Exception:
        return JsonResponse({"error": "The translation service returned an unexpected error."}, status=500)
    return JsonResponse({"content": content, "language": target, "model": used_model})


def _unique_project_title(workspace, title):
    base = f"{title} copy"[:220]
    candidate, counter = base, 2
    while Project.all_objects.filter(workspace=workspace, title=candidate).exists():
        candidate = f"{base} {counter}"[:240]
        counter += 1
    return candidate


@login_required
@transaction.atomic
def project_copy(request, project_id):
    source = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    if request.method != "POST" or not is_workspace_owner_or_admin(request.user, source.workspace):
        return HttpResponseForbidden("Workspace owner or administrator permission is required.")
    copied = Project.objects.create(
        workspace=source.workspace, project_type=source.project_type, title=_unique_project_title(source.workspace, source.title),
        concept=source.concept, original_language=source.original_language, translation_languages=source.translation_languages,
        prompt_template=source.prompt_template, documentation_language=source.documentation_language,
        dialogue_language=source.dialogue_language, prompt_language=source.prompt_language,
        rights_holder=source.rights_holder, publication_info=source.publication_info, status=Project.Status.DRAFT,
        created_by=request.user, updated_by=request.user,
    )
    character_map = {}
    for character in source.characters.all():
        character_map[character.id] = Character.objects.create(project=copied, name=character.name, description=character.description, visual_description=character.visual_description, position=character.position, created_by=request.user, updated_by=request.user)
    scene_map, prompt_map = {}, {}
    for episode in source.episodes.prefetch_related("scenes__dialogue_lines", "scenes__prompts__blocks").all():
        new_episode = Episode.objects.create(project=copied, number=episode.number, title=episode.title, summary=episode.summary, position=episode.position, language=episode.language, created_by=request.user, updated_by=request.user)
        for scene in episode.scenes.all():
            new_scene = Scene.objects.create(episode=new_episode, number=scene.number, title=scene.title, hook=scene.hook, description=scene.description, location=scene.location, actions=scene.actions, performance_notes=scene.performance_notes, status=scene.status, position=scene.position, created_by=request.user, updated_by=request.user)
            scene_map[scene.id] = new_scene
            line_map = {}
            for line in scene.dialogue_lines.all():
                new_line = DialogueLine.objects.create(scene=new_scene, speaker=line.speaker, text=line.text, language=line.language, delivery=line.delivery, position=line.position, status=line.status, created_by=request.user, updated_by=request.user)
                line_map[line.id] = new_line
                for translated in line.translations.all():
                    TranslationUnit.objects.create(dialogue_line=new_line, source_text=translated.source_text, translated_text=translated.translated_text, target_language=translated.target_language, status=translated.status, created_by=request.user, updated_by=request.user)
            ordered_prompts = list(scene.prompts.select_related("source_prompt").prefetch_related("blocks").all())
            for prompt in sorted(ordered_prompts, key=lambda item: bool(item.source_prompt_id)):
                new_prompt = Prompt.objects.create(scene=new_scene, ai_model=prompt.ai_model, template=prompt.template, source_prompt=prompt_map.get(prompt.source_prompt_id), original_language=prompt.original_language, language=prompt.language, translation_scope=prompt.translation_scope, content=prompt.content, prompt_type=prompt.prompt_type, title=prompt.title, status=prompt.status, position=prompt.position, created_by=request.user, updated_by=request.user)
                prompt_map[prompt.id] = new_prompt
                for block in prompt.blocks.all():
                    PromptBlock.objects.create(prompt=new_prompt, block_type=block.block_type, content=block.content, source_dialogue=line_map.get(block.source_dialogue_id), translated_content=block.translated_content, translation_language=block.translation_language, translation_model=block.translation_model, position=block.position, created_by=request.user, updated_by=request.user)
    source_assets = list(
        Asset.objects.filter(projects=source, purged_at__isnull=True).distinct()
    )
    asset_map = {asset.id: asset for asset in source_assets}
    if source_assets:
        copied.media_assets.add(*source_assets)
    for old_scene_id, new_scene in scene_map.items():
        old_scene = Scene.all_objects.get(id=old_scene_id)
        references = list(old_scene.reference_assets.all())
        if references:
            new_scene.reference_assets.add(*references)
            copied.media_assets.add(*references)
    for old_prompt_id, new_prompt in prompt_map.items():
        references = list(Prompt.all_objects.get(id=old_prompt_id).reference_assets.all())
        if references:
            new_prompt.reference_assets.add(*references)
            copied.media_assets.add(*references)
    for old_character_id, new_character in character_map.items():
        old_character = Character.all_objects.get(id=old_character_id)
        references = list(old_character.reference_assets.all())
        if references:
            new_character.reference_assets.add(*references)
            copied.media_assets.add(*references)
        old_avatar_id = old_character.avatar_asset_id
        if old_avatar_id:
            new_character.avatar_asset_id = old_avatar_id
            new_character.save(update_fields=["avatar_asset", "updated_at"])
    if source.cover_asset_id:
        copied.cover_asset_id = source.cover_asset_id
        copied.media_assets.add(source.cover_asset_id)
        copied.save(update_fields=["cover_asset", "updated_at"])
    if request.POST.get("copy_access") == "1":
        ProjectMembership.objects.bulk_create([
            ProjectMembership(project=copied, user=item.user, role=item.role, is_active=item.is_active, invited_by=request.user)
            for item in source.memberships.filter(is_active=True).select_related("user")
        ])
    audit(workspace=source.workspace, actor=request.user, action="PROJECT_COPIED", instance=copied, metadata={"sourceProjectId": str(source.id)})
    messages.success(request, "Project copied with shared links to its Workspace images.")
    return redirect("studio:project_detail", project_id=copied.id)


@login_required
def project_set_cover(request, project_id):
    project = get_object_or_404(
        accessible_projects(request.user).select_related("workspace"),
        id=project_id,
    )
    if request.method != "POST" or not has_project_capability(request.user, project, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    asset = get_object_or_404(
        Asset.objects.filter(Q(projects=project) | Q(project=project), content_type__startswith="image/").distinct(),
        id=request.POST.get("asset_id"),
    )
    project.cover_asset = asset
    asset.projects.add(project)
    project.updated_by = request.user
    project.save(update_fields=["cover_asset", "updated_by", "updated_at"])
    audit(
        workspace=project.workspace,
        actor=request.user,
        action="PROJECT_COVER_UPDATED",
        instance=project,
        metadata={"assetId": str(asset.id)},
    )
    messages.success(request, "Project cover updated.")
    return HttpResponseRedirect(
        reverse("studio:workspace_detail", kwargs={"workspace_id": project.workspace_id})
        + f"#project-{project.id}"
    )


@login_required
def project_trash(request, project_id):
    project = get_object_or_404(accessible_projects(request.user), id=project_id)
    if request.method != "POST" or not is_workspace_owner_or_admin(request.user, project.workspace):
        return HttpResponseForbidden("Workspace owner or administrator permission is required.")
    project.deleted_at = timezone.now(); project.deleted_by = request.user; project.updated_by = request.user
    project.save(update_fields=["deleted_at", "deleted_by", "updated_by", "updated_at"])
    messages.success(request, "Project archived. It remains available in Archive until explicitly deleted.")
    return redirect("studio:workspace_detail", workspace_id=project.workspace_id)


@login_required
def project_trash_view(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    projects = list(Project.all_objects.filter(workspace=workspace, deleted_at__isnull=False, purged_at__isnull=True).order_by("-deleted_at"))
    can_administer = is_workspace_owner_or_admin(request.user, workspace)
    if can_administer:
        for project in projects:
            project.purge_token = _archive_purge_token(kind="project", item_id=project.id, user=request.user)
    return render(request, "studio/project_trash.html", {"workspace": workspace, "projects": projects, "can_edit": can_administer, "purge_delay": ARCHIVE_PURGE_DELAY_SECONDS})


@login_required
def project_restore(request, project_id):
    project = get_object_or_404(Project.all_objects.select_related("workspace"), id=project_id, deleted_at__isnull=False, purged_at__isnull=True)
    if request.method != "POST" or not is_workspace_owner_or_admin(request.user, project.workspace):
        return HttpResponseForbidden("Workspace owner or administrator permission is required.")
    project.deleted_at = None; project.deleted_by = None; project.updated_by = request.user
    project.save(update_fields=["deleted_at", "deleted_by", "updated_by", "updated_at"])
    messages.success(request, "Project restored.")
    requested = request.POST.get("next", "")
    if requested and url_has_allowed_host_and_scheme(requested, {request.get_host()}, require_https=request.is_secure()):
        return HttpResponseRedirect(requested)
    return redirect("studio:project_detail", project_id=project.id)


@login_required
def project_purge(request, project_id):
    project = get_object_or_404(Project.all_objects.select_related("workspace"), id=project_id, deleted_at__isnull=False, purged_at__isnull=True)
    if request.method != "POST" or not is_workspace_owner_or_admin(request.user, project.workspace):
        return HttpResponseForbidden("Workspace owner or administrator permission is required.")
    if not _archive_purge_allowed(token=request.POST.get("purge_token", ""), kind="project", item_id=project.id, user=request.user):
        return HttpResponseForbidden("Wait 30 seconds on the archive confirmation before deleting permanently.")
    for asset in Asset.all_objects.filter(projects=project).distinct():
        asset.projects.remove(project)
    project.purged_at = timezone.now(); project.purged_by = request.user; project.updated_by = request.user
    project.save(update_fields=["purged_at", "purged_by", "updated_by", "updated_at"])
    messages.success(request, "Project permanently removed from the archive.")
    requested = request.POST.get("next", "")
    if requested and url_has_allowed_host_and_scheme(requested, {request.get_host()}, require_https=request.is_secure()):
        return HttpResponseRedirect(requested)
    return redirect("studio:project_trash_view", workspace_id=project.workspace_id)


@login_required
def project_trash_clear(request, workspace_id):
    return HttpResponseForbidden("Bulk permanent deletion is disabled. Delete archived projects individually after the confirmation delay.")


@login_required
def project_image_trash_clear(request, project_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    return HttpResponseForbidden("Images can only be deleted from the Workspace Recycle Bin.")


@login_required
def trashed_asset_file(request, asset_id, thumbnail=False):
    asset = get_object_or_404(
        accessible_assets(request.user, include_deleted=True).select_related("workspace").filter(
            content_type__startswith="image/",
            deleted_at__isnull=False,
            purged_at__isnull=True,
        ),
        id=asset_id,
    )
    field = asset.thumbnail if thumbnail and asset.thumbnail.name else asset.file
    if not field.name:
        raise Http404("Image file not found.")
    audit(workspace=asset.workspace, actor=request.user, action="ASSET_TRASH_VIEW", instance=asset)
    return FileResponse(field.open("rb"), content_type="image/jpeg" if thumbnail else asset.content_type)

@login_required
def docx_roundtrip(request, job_id):
    job = get_object_or_404(
        ExportJob.objects.select_related("project__workspace", "output_asset").filter(
            project__in=accessible_projects(request.user),
            output_asset__isnull=False,
        ),
        id=job_id,
    )
    if "DOCX" not in job.sections:
        raise Http404("DOCX export not found.")
    report = compare_docx_export(job)
    audit(
        workspace=job.workspace,
        actor=request.user,
        action="DOCX_ROUNDTRIP_REVIEWED",
        instance=job.output_asset,
        metadata={
            "exportId": str(job.id),
            "available": report["available"],
            "changedCategories": report.get("changed_categories"),
        },
    )
    return render(request, "studio/docx_roundtrip.html", {
        "job": job,
        "project": job.project,
        "report": report,
        "can_export": has_object_capability(request.user, job, "export"),
    })

def _prompt_editor_context(request):
    return {
        "ai_models": AiModelProfile.objects.filter(is_active=True),
        "text_models": active_text_models(),
        "default_text_model": default_text_model_id(),
        "prompt_languages": PROMPT_LANGUAGES,
        "prompt_addition": default_prompt_template(),
        "prompt_types": Prompt.Type.choices,
        "prompt_statuses": Prompt.Status.choices,
        "block_types": PromptBlock.Type.choices,
        "can_manage_text_models": request.user.is_superuser,
        "available_text_models": sorted((value, value) for value in TEXT_MODELS),
    }


def _prompt_payload(prompt):
    versions = [
        {
            "id": str(item.id),
            "language": item.language,
            "label": f"{'Translation' if item.source_prompt_id else 'Original'} · {item.language}",
            "dataUrl": reverse("studio:prompt_editor_data", kwargs={"prompt_id": item.id}),
        }
        for item in prompt.language_versions()
    ]
    return {
        "id": str(prompt.id),
        "language": prompt.language,
        "originalLanguage": prompt.original_language,
        "isTranslation": bool(prompt.source_prompt_id),
        "content": prompt.editor_content,
        "title": prompt.title,
        "aiModel": str(prompt.ai_model_id),
        "promptType": prompt.prompt_type,
        "status": prompt.status,
        "versions": versions,
        "saveUrl": reverse("studio:prompt_quick_save", kwargs={"prompt_id": prompt.id}),
        "previewUrl": reverse("studio:prompt_ai_preview", kwargs={"prompt_id": prompt.id}),
        "applyUrl": reverse("studio:prompt_apply_translation", kwargs={"prompt_id": prompt.id}),
    }


def _save_unified_prompt_content(prompt, content, user, operation="INLINE_UPDATE"):
    clean = (content or "").strip()
    prompt.content = clean
    prompt.updated_by = user
    prompt.save(update_fields=["content", "updated_by", "updated_at"])
    blocks = list(prompt.blocks.order_by("position", "id"))
    if blocks:
        primary = blocks[0]
        primary.content = clean
        primary.block_type = PromptBlock.Type.NARRATIVE
        primary.source_dialogue = None
        primary.updated_by = user
        primary.save(update_fields=["content", "block_type", "source_dialogue", "updated_by", "updated_at"])
        record_revision(instance=primary, user=user, operation=operation)
        now = timezone.now()
        for extra in blocks[1:]:
            extra.deleted_at = now
            extra.deleted_by = user
            extra.updated_by = user
            extra.save(update_fields=["deleted_at", "deleted_by", "updated_by", "updated_at"])
    else:
        primary = PromptBlock.objects.create(
            prompt=prompt,
            block_type=PromptBlock.Type.NARRATIVE,
            content=clean,
            position=0,
            created_by=user,
            updated_by=user,
        )
        record_revision(instance=primary, user=user, operation="CREATE")
    record_revision(instance=prompt, user=user, operation=operation)


def _attach_prompt_ai_state(prompts):
    for prompt in prompts:
        suggestions = list(prompt.ai_suggestions.all())
        prompt.pending_suggestions = [item for item in suggestions if item.status == AiSuggestion.Status.PENDING]
        prompt.undo_suggestion = next(
            (item for item in suggestions if item.status == AiSuggestion.Status.ACCEPTED and item.original_blocks),
            None,
        )
        prompt.display_assets = list(prompt.reference_assets.all())


@login_required
def scene_prompt_quick_create(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__in=accessible_projects(request.user)
        ),
        id=scene_id,
    )
    workspace = scene.episode.project.workspace
    if request.method != "POST" or not has_object_capability(request.user, scene, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    ai_model = get_object_or_404(AiModelProfile.objects.filter(is_active=True), id=request.POST.get("ai_model"))
    prompt_type = request.POST.get("prompt_type", Prompt.Type.IMAGE)
    status = request.POST.get("status", Prompt.Status.DRAFT)
    if prompt_type not in Prompt.Type.values or status not in Prompt.Status.values:
        messages.error(request, "Choose a valid prompt type and status.")
        return redirect("studio:scene_detail", scene_id=scene.id)
    content = request.POST.get("content", "").strip()
    prompt = Prompt.objects.create(
        scene=scene,
        ai_model=ai_model,
        template=default_prompt_template(prompt_type),
        prompt_type=prompt_type,
        title=request.POST.get("title", "").strip(),
        status=status,
        position=scene.prompts.count(),
        content=content,
        created_by=request.user,
        updated_by=request.user,
    )
    content = prompt.content
    if content:
        block = PromptBlock.objects.create(
            prompt=prompt,
            block_type=PromptBlock.Type.NARRATIVE,
            content=content,
            position=0,
            created_by=request.user,
            updated_by=request.user,
        )
        record_revision(instance=block, user=request.user, operation="CREATE")
    record_revision(instance=prompt, user=request.user, operation="CREATE")
    audit(workspace=workspace, actor=request.user, action="PROMPT_INLINE_CREATED", instance=prompt)
    messages.success(request, "Prompt created.")
    return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": scene.id}) + f"#prompt-{prompt.id}")


@login_required
def prompt_quick_save(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model", "template")
        .prefetch_related("blocks")
        .filter(scene__episode__project__in=accessible_projects(request.user)),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    if request.method != "POST" or not has_object_capability(request.user, prompt, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    ai_model = get_object_or_404(AiModelProfile.objects.filter(is_active=True), id=request.POST.get("ai_model"))
    prompt_type = request.POST.get("prompt_type", prompt.prompt_type)
    status = request.POST.get("status", prompt.status)
    if prompt_type not in Prompt.Type.values or status not in Prompt.Status.values:
        messages.error(request, "Choose a valid prompt type and status.")
        return redirect("studio:scene_detail", scene_id=prompt.scene_id)
    with transaction.atomic():
        old_original_language = prompt.original_language
        requested_original_language = request.POST.get("original_language", old_original_language).strip().upper()
        if not prompt.source_prompt_id and requested_original_language not in Prompt.Language.values:
            if request.headers.get("X-Requested-With") == "XMLHttpRequest":
                return JsonResponse({"error": "Choose a valid original language."}, status=400)
            messages.error(request, "Choose a valid original language.")
            return redirect("studio:scene_detail", scene_id=prompt.scene_id)
        prompt.ai_model = ai_model
        prompt.template = default_prompt_template(prompt_type)
        prompt.prompt_type = prompt_type
        prompt.title = request.POST.get("title", "").strip()
        prompt.status = status
        if not prompt.source_prompt_id:
            prompt.original_language = requested_original_language
            prompt.language = requested_original_language
        content = request.POST.get("content", prompt.editor_content)
        prompt.updated_by = request.user
        prompt.full_clean()
        prompt.save(update_fields=["ai_model", "template", "prompt_type", "title", "status", "original_language", "language", "updated_by", "updated_at"])
        if not prompt.source_prompt_id and requested_original_language != old_original_language:
            for translated in prompt.translations.all():
                translated.original_language = requested_original_language
                translated.updated_by = request.user
                translated.save(update_fields=["original_language", "updated_by", "updated_at"])
                record_revision(instance=translated, user=request.user, operation="LANGUAGE_PROPAGATION")
        _save_unified_prompt_content(prompt, content, request.user)
        audit(
            workspace=workspace,
            actor=request.user,
            action="PROMPT_INLINE_UPDATED",
            instance=prompt,
            metadata={"originalLanguageFrom": old_original_language, "originalLanguageTo": prompt.original_language},
        )
    if request.headers.get("X-Requested-With") == "XMLHttpRequest":
        return JsonResponse(_prompt_payload(prompt))
    messages.success(request, "Prompt saved.")
    if request.POST.get("return_to") == "chain":
        return HttpResponseRedirect(reverse("studio:project_scene_chain", kwargs={"project_id": prompt.scene.episode.project_id}) + f"#prompt-{prompt.id}")
    return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": prompt.scene_id}) + f"#prompt-{prompt.id}")


@login_required
def prompt_image_upload(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__in=accessible_projects(request.user)
        ),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    if request.method != "POST" or not has_object_capability(request.user, prompt, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = MultipleImageUploadForm(request.POST, request.FILES, limit=3)
    if form.is_valid():
        uploaded_count = 0
        for uploaded in form.cleaned_data["file"]:
            try:
                create_asset(
                    user=request.user, workspace=workspace, uploaded=uploaded,
                    kind=Asset.Kind.OTHER, project=prompt.scene.episode.project,
                    scene=prompt.scene, prompt=prompt, prevent_duplicate=True,
                )
            except ValidationError as exc:
                messages.warning(request, "; ".join(exc.messages))
            else:
                uploaded_count += 1
        if uploaded_count:
            messages.success(request, f"Attached {uploaded_count} prompt image{'s' if uploaded_count != 1 else ''}.")
    else:
        messages.error(request, "Choose a valid JPG, PNG or WEBP image.")
    if request.POST.get("return_to") == "chain":
        return HttpResponseRedirect(reverse("studio:project_scene_chain", kwargs={"project_id": prompt.scene.episode.project_id}) + f"#prompt-{prompt.id}")
    return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": prompt.scene_id}) + f"#prompt-{prompt.id}")


@login_required
def prompt_asset_attach(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__in=accessible_projects(request.user)
        ),
        id=prompt_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, prompt, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    project = prompt.scene.episode.project
    asset = get_object_or_404(
        Asset.objects.filter(workspace=project.workspace, content_type__startswith="image/").filter(
            Q(projects__in=accessible_projects(request.user))
            | Q(projects__isnull=True),
        ),
        id=request.POST.get("asset_id"),
    )
    asset.projects.add(project)
    prompt.reference_assets.add(asset)
    prompt.updated_by = request.user
    prompt.save(update_fields=["updated_by", "updated_at"])
    audit(workspace=prompt.scene.episode.project.workspace, actor=request.user, action="PROMPT_ASSET_ATTACHED", instance=prompt, metadata={"assetId": str(asset.id)})
    messages.success(request, "Project image attached to prompt.")
    return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": prompt.scene_id}) + f"#prompt-{prompt.id}")


@login_required
def project_scene_chain(request, project_id):
    project = get_object_or_404(
        Project.objects.select_related("workspace").prefetch_related(
            Prefetch("episodes__scenes__reference_assets", queryset=Asset.objects.filter(prompt__isnull=True), to_attr="chain_images"),
            Prefetch(
                "episodes__scenes__prompts",
                queryset=Prompt.objects.only("id", "scene_id"),
                to_attr="chain_prompts",
            ),
        ).filter(id__in=accessible_projects(request.user)),
        id=project_id,
    )
    for episode in project.episodes.all():
        for scene in episode.scenes.all():
            scene.chain_prompt_count = len(scene.chain_prompts)
    context = {
        "project": project,
        **_project_header_context(request.user, project),
    }
    return render(request, "studio/project_scene_chain.html", context)


@login_required
def scene_quick_save(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__in=accessible_projects(request.user)
        ),
        id=scene_id,
    )
    workspace = scene.episode.project.workspace
    if request.method != "POST" or not has_object_capability(request.user, scene, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = SceneForm(request.POST, instance=scene)
    if form.is_valid():
        scene = form.save(commit=False)
        scene.updated_by = request.user
        scene.save()
        record_revision(instance=scene, user=request.user, operation="CHAIN_UPDATE")
        audit(workspace=workspace, actor=request.user, action="SCENE_CHAIN_UPDATED", instance=scene)
        messages.success(request, f"Scene {scene.number} saved.")
    else:
        messages.error(request, "Scene could not be saved. Check the entered values.")
    return HttpResponseRedirect(reverse("studio:project_scene_chain", kwargs={"project_id": scene.episode.project_id}) + f"#scene-{scene.id}")


@login_required
def episode_scene_reorder(request, episode_id):
    episode = get_object_or_404(
        Episode.objects.select_related("project__workspace").filter(
            project__in=accessible_projects(request.user)
        ),
        id=episode_id,
    )
    workspace = episode.project.workspace
    if request.method != "POST" or not has_object_capability(request.user, episode, "edit"):
        return JsonResponse({"error": "Edit permission is required."}, status=403)
    try:
        data = json.loads(request.body.decode("utf-8"))
        ordered_ids = [str(value) for value in data.get("sceneIds", [])]
    except (ValueError, TypeError, UnicodeDecodeError):
        return JsonResponse({"error": "Invalid reorder payload."}, status=400)
    with transaction.atomic():
        scenes = list(Scene.objects.select_for_update().filter(episode=episode).order_by("position", "id"))
        by_id = {str(scene.id): scene for scene in scenes}
        if len(ordered_ids) != len(scenes) or set(ordered_ids) != set(by_id):
            return JsonResponse({"error": "Every scene must be included exactly once."}, status=400)
        for offset, scene_id in enumerate(ordered_ids):
            scene = by_id[scene_id]
            scene.position = 100000 + offset
            scene.number = 100000 + offset
            scene.save(update_fields=["position", "number", "updated_at"])
        for position, scene_id in enumerate(ordered_ids):
            scene = by_id[scene_id]
            scene.position = position
            scene.number = position + 1
            scene.updated_by = request.user
            scene.save(update_fields=["position", "number", "updated_by", "updated_at"])
        audit(
            workspace=workspace,
            actor=request.user,
            action="SCENES_DRAG_REORDERED",
            instance=episode,
            metadata={"sceneIds": ordered_ids},
        )
    return JsonResponse({"ok": True, "sceneIds": ordered_ids})
