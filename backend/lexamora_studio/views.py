import json
from datetime import timedelta

from django.conf import settings
from django.contrib import messages
from django.contrib.auth import get_user_model
from django.contrib.auth.decorators import login_required
from django.core import signing
from django.core.exceptions import ValidationError
from django.core.files.base import ContentFile
from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.paginator import Paginator
from django.core.mail import send_mail
from django.db import transaction
from django.db.models import Prefetch, Q, Sum
from django.forms import inlineformset_factory
from django.http import FileResponse, Http404, HttpResponse, HttpResponseForbidden, HttpResponseRedirect, JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.urls import reverse
from django.utils import timezone
from django.utils.http import url_has_allowed_host_and_scheme
from django.utils.text import slugify

from lessons.ai_gateway import ProviderError, TEXT_MODELS, generate_image as _gateway_generate_image, generate_image_with_usage as _gateway_generate_image_with_usage, run_multimodal_text_with_usage as _gateway_run_multimodal_text_with_usage, run_text as _gateway_run_text, run_text_with_usage as _gateway_run_text_with_usage
from lessons.provider_credentials import user_has_ai_access

from .ai import StudioAiError, accept_suggestion, improve_prompt, preview_prompt_translation, reject_suggestion, translate_prompt, translate_prompt_dialogue, undo_suggestion
from .ai_catalog import PROMPT_LANGUAGES, PROMPT_LANGUAGE_NAMES, active_text_models, default_prompt_template, default_text_model_id, project_text_model_id, selected_text_model
from .comics import build_episode_comic_pdf
from .exports import ALL_SECTIONS, ExportError, generate_export
from .external_images import download_external_image
from .docx_imports import accept_docx_import, parse_docx
from .docx_exports import generate_docx_export
from .docx_roundtrip import compare_docx_export
from .forms import AdditionalGenerationForm, AiModelProfileForm, AssetEditForm, CharacterCreateForm, CharacterForm, DialogueLineForm, DocxImportUploadForm, EpisodeForm, GenerationOutputUploadForm, ImageUploadForm, MultipleImageUploadForm, ProjectBulkMembershipForm, ProjectForm, ProjectMembershipForm, ProjectSettingsForm, ProjectUserSelectionForm, PromptBlockForm, PromptForm, RecommendedTrackForm, SceneForm, StudioTextModelForm, WorkspaceForm, WorkspaceMembershipForm, WorkspaceUserSelectionForm
from .models import AdditionalGeneration, AiModelProfile, AiSuggestion, AiUsageLog, Asset, Character, DialogueLine, DocxImport, EmailDeliveryLog, Episode, EpisodeComic, EpisodeCover, ExportJob, GenerationOutput, ImageGenerationJob, Project, ProjectAccessExclusion, ProjectAssistantContext, ProjectMembership, Prompt, PromptBlock, RecommendedTrack, Scene, StudioTextModel, SubtitleTrack, TranslationUnit, Workspace, WorkspaceMembership
from .notifications import notify_access_granted
from .permissions import accessible_assets, accessible_projects, accessible_suggestions, accessible_workspaces, has_capability, has_object_capability, has_project_capability, is_workspace_owner_or_admin
from .revisions import audit, record_revision
from .services import bulk_replace_subtitle_lines, create_workspace, propagate_project_original_language, reorder_subtitle_lines, save_translation, update_dialogue_line
from .storage import create_asset, crop_asset, purge_asset, restore_asset, trash_asset
from .usage import TokenQuotaExceeded, require_token_quota, token_summary


ARCHIVE_PURGE_DELAY_SECONDS = 30
run_text = _gateway_run_text
generate_image = _gateway_generate_image


def _is_server_super_admin(user):
    return bool(
        getattr(user, "is_superuser", False)
        or user.groups.filter(name="Superadmin").exists()
    )


def _run_logged_text(*, workspace, user, action, model, text, prompt=None):
    require_token_quota(user)
    usage = AiUsageLog.objects.create(
        workspace=workspace, user=user, prompt=prompt, action=action,
        model=model, status="STARTED", input_chars=len(text),
    )
    try:
        if run_text is not _gateway_run_text:
            output, selected_model = run_text(model, text)
            provider_usage = {}
        else:
            output, selected_model, provider_usage = _gateway_run_text_with_usage(model, text)
    except Exception:
        usage.status = "ERROR"
        usage.error_code = "provider_error"
        usage.save(update_fields=["status", "error_code"])
        raise
    usage.model = selected_model
    usage.status = "SUCCESS"
    usage.output_chars = len(output)
    usage.input_tokens = provider_usage.get("input_tokens", 0)
    usage.output_tokens = provider_usage.get("output_tokens", 0)
    usage.total_tokens = provider_usage.get("total_tokens", 0)
    usage.save(update_fields=[
        "model", "status", "output_chars", "input_tokens", "output_tokens", "total_tokens",
    ])
    return output, selected_model


def _run_logged_multimodal_text(*, workspace, user, action, model, text, reference_images, prompt=None):
    require_token_quota(user)
    usage = AiUsageLog.objects.create(
        workspace=workspace, user=user, prompt=prompt, action=action,
        model=model, status="STARTED", input_chars=len(text),
    )
    try:
        output, selected_model, provider_usage = _gateway_run_multimodal_text_with_usage(
            model, text, reference_images,
        )
    except Exception:
        usage.status = "ERROR"
        usage.error_code = "provider_error"
        usage.save(update_fields=["status", "error_code"])
        raise
    usage.model = selected_model
    usage.status = "SUCCESS"
    usage.output_chars = len(output)
    usage.input_tokens = provider_usage.get("input_tokens", 0)
    usage.output_tokens = provider_usage.get("output_tokens", 0)
    usage.total_tokens = provider_usage.get("total_tokens", 0)
    usage.save(update_fields=[
        "model", "status", "output_chars", "input_tokens", "output_tokens", "total_tokens",
    ])
    return output, selected_model


def _generate_provider_image(prompt, **kwargs):
    if generate_image is not _gateway_generate_image:
        image_bytes, selected_model = generate_image(prompt, **kwargs)
        return image_bytes, selected_model, {}
    return _gateway_generate_image_with_usage(prompt, **kwargs)


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
        project.can_copy = has_project_capability(request.user, project, "manage_project")
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
        "imports": workspace.docx_imports.filter(
            archived_at__isnull=True, source_asset__deleted_at__isnull=True,
        ).select_related("project", "source_asset", "requested_by"),
        "archived_imports": workspace.docx_imports.filter(
            archived_at__isnull=False, source_asset__deleted_at__isnull=True,
        ).select_related("project", "source_asset", "archived_by"),
        "recycle_count": recycle_count,
        "archive_count": archive_count,
        "gallery_assets": workspace_assets,
        "gallery_projects": projects,
        "project_assets": [],
        "workspace_assets": workspace_assets,
        "copy_target_workspaces": _project_copy_targets(request.user),
    })


@login_required
def workspace_edit(request, workspace_id):
    workspace = get_object_or_404(
        accessible_workspaces(request.user).select_related("avatar_asset", "default_image_model"),
        id=workspace_id,
    )
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
        item.save()
        record_revision(instance=item, user=request.user, operation="UPDATE")
        messages.success(request, "Workspace settings saved.")
        return redirect("studio:workspace_edit", workspace_id=item.id)
    ai_usage = workspace.ai_usage.select_related("user", "prompt").order_by("-created_at")
    user_filter = request.GET.get("ai_user", "").strip()
    model_filter = request.GET.get("ai_model", "").strip()
    action_filter = request.GET.get("ai_action", "").strip()
    status_filter = request.GET.get("ai_status", "").strip()
    if user_filter:
        ai_usage = ai_usage.filter(user_id=user_filter)
    if model_filter:
        ai_usage = ai_usage.filter(model=model_filter)
    if action_filter:
        ai_usage = ai_usage.filter(action=action_filter)
    if status_filter:
        ai_usage = ai_usage.filter(status=status_filter)
    ai_totals = ai_usage.aggregate(
        input_tokens=Sum("input_tokens"), output_tokens=Sum("output_tokens"), total_tokens=Sum("total_tokens"),
    )
    try:
        per_page = int(request.GET.get("per_page", 50))
    except (TypeError, ValueError):
        per_page = 50
    if per_page not in {20, 50, 100, 500}:
        per_page = 50
    page = Paginator(ai_usage, per_page).get_page(request.GET.get("page"))
    workspace_assets = _decorate_gallery_assets(request.user, list(_accessible_workspace_images(request.user, workspace)), deduplicate=True)
    return render(request, "studio/workspace_settings.html", {
        "workspace": workspace, "form": form, "models": AiModelProfile.objects.all(),
        "can_administer": is_workspace_owner_or_admin(request.user, workspace),
        "email_recipients": [workspace.owner, *[item.user for item in workspace.memberships.filter(status=WorkspaceMembership.Status.ACTIVE).select_related("user") if item.user_id != workspace.owner_id]],
        "email_logs": workspace.email_delivery_logs.select_related("created_by")[:10],
        "ai_usage": page,
        "ai_page": page,
        "ai_per_page": per_page,
        "ai_users": workspace.ai_usage.select_related("user").order_by("user__email").values_list("user_id", "user__email", "user__username").distinct(),
        "ai_models": workspace.ai_usage.order_by("model").values_list("model", flat=True).distinct(),
        "ai_actions": workspace.ai_usage.order_by("action").values_list("action", flat=True).distinct(),
        "ai_filters": {"user": user_filter, "model": model_filter, "action": action_filter, "status": status_filter},
        "project_assets": [], "workspace_assets": workspace_assets,
        "ai_totals": {key: value or 0 for key, value in ai_totals.items()},
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
    action = request.POST.get("action", "save") if request.method == "POST" else "save"
    form = WorkspaceMembershipForm(request.POST if action == "save" else None, workspace=workspace)
    selection_form = WorkspaceUserSelectionForm(
        request.POST if action == "select_users" else None,
        workspace=workspace,
    ) if _is_server_super_admin(request.user) else None
    if request.method == "POST":
        if action in {"remove", "exclude"} and request.user.id != workspace.owner_id:
            return HttpResponseForbidden("Only the workspace owner can remove workspace access.")
        if action == "remove":
            membership = get_object_or_404(WorkspaceMembership, workspace=workspace, id=request.POST.get("membership_id"))
            if membership.user_id == workspace.owner_id or membership.role == WorkspaceMembership.Role.OWNER:
                return HttpResponseForbidden("The workspace owner cannot be removed.")
            ProjectMembership.objects.filter(project__workspace=workspace, user=membership.user).delete()
            ProjectAccessExclusion.objects.filter(project__workspace=workspace, user=membership.user).delete()
            membership.delete()
            messages.success(request, "Workspace access removed.")
            return redirect("studio:workspace_access", workspace_id=workspace.id)
        if action == "select_users" and selection_form is not None and selection_form.is_valid():
            added = 0
            updated = 0
            role = selection_form.cleaned_data["role"]
            can_use_ai = selection_form.cleaned_data["can_use_ai"]
            for user in selection_form.cleaned_data["users"]:
                existed = WorkspaceMembership.objects.filter(
                    workspace=workspace, user=user, status=WorkspaceMembership.Status.ACTIVE,
                ).exists()
                WorkspaceMembership.objects.update_or_create(
                    workspace=workspace,
                    user=user,
                    defaults={"role": role, "status": WorkspaceMembership.Status.ACTIVE, "can_use_ai": can_use_ai},
                )
                ProjectAccessExclusion.objects.filter(project__workspace=workspace, user=user).delete()
                if existed:
                    updated += 1
                else:
                    added += 1
                    notify_access_granted(
                        user=user, entity_name=workspace.name, entity_kind="workspace",
                        url=request.build_absolute_uri(reverse("studio:workspace_detail", kwargs={"workspace_id": workspace.id})),
                        granted_by=request.user,
                    )
            messages.success(request, f"Selected workspace access saved: {added} added, {updated} updated")
            return redirect("studio:workspace_access", workspace_id=workspace.id)
        if action == "save" and form.is_valid():
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
    return render(request, "studio/workspace_access.html", {
        "workspace": workspace,
        "form": form,
        "selection_form": selection_form,
        "owner_membership": workspace.memberships.filter(user=workspace.owner).select_related("user").first(),
        "memberships": workspace.memberships.exclude(user=workspace.owner).select_related("user"),
        "can_remove_access": request.user.id == workspace.owner_id,
        "can_transfer_owner": _is_server_super_admin(request.user),
    })


@login_required
@transaction.atomic
def workspace_transfer_owner(request, workspace_id):
    if request.method != "POST" or not _is_server_super_admin(request.user):
        return HttpResponseForbidden("Server super administrator access is required.")
    workspace = get_object_or_404(Workspace.objects.select_for_update().select_related("owner"), id=workspace_id)
    email = request.POST.get("email", "").strip().lower()
    matches = list(get_user_model().objects.filter(email__iexact=email)[:2])
    if len(matches) != 1:
        messages.error(request, "Enter the email of one registered user.")
        return redirect("studio:workspace_access", workspace_id=workspace.id)
    new_owner = matches[0]
    if new_owner.id == workspace.owner_id:
        messages.warning(request, "This user already owns the workspace.")
        return redirect("studio:workspace_access", workspace_id=workspace.id)
    previous_owner = workspace.owner
    WorkspaceMembership.objects.update_or_create(
        workspace=workspace,
        user=previous_owner,
        defaults={
            "role": WorkspaceMembership.Role.ADMIN,
            "status": WorkspaceMembership.Status.ACTIVE,
            "can_use_ai": True,
            "can_export": True,
            "can_manage_members": True,
        },
    )
    WorkspaceMembership.objects.update_or_create(
        workspace=workspace,
        user=new_owner,
        defaults={
            "role": WorkspaceMembership.Role.OWNER,
            "status": WorkspaceMembership.Status.ACTIVE,
            "can_use_ai": True,
            "can_export": True,
            "can_manage_members": True,
        },
    )
    ProjectAccessExclusion.objects.filter(project__workspace=workspace, user=new_owner).delete()
    workspace.owner = new_owner
    workspace.updated_by = request.user
    workspace.save(update_fields=["owner", "updated_by", "updated_at"])
    audit(
        workspace=workspace,
        actor=request.user,
        action="WORKSPACE_OWNER_TRANSFERRED",
        instance=workspace,
        metadata={"previousOwnerId": previous_owner.id, "newOwnerId": new_owner.id},
    )
    messages.success(request, "Workspace owner changed. The previous owner remains a workspace administrator.")
    return redirect("studio:workspace_access", workspace_id=workspace.id)


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
def workspace_remove_avatar(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    if request.method != "POST" or not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    workspace.avatar_asset = None
    workspace.updated_by = request.user
    workspace.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
    audit(workspace=workspace, actor=request.user, action="WORKSPACE_AVATAR_REMOVED", instance=workspace)
    if request.headers.get("X-Requested-With") == "XMLHttpRequest":
        return JsonResponse({"ok": True})
    messages.success(request, "Workspace avatar removed")
    return redirect("studio:workspace_edit", workspace_id=workspace.id)


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
    form = ProjectForm(request.POST or None)
    if request.method == "POST" and form.is_valid():
        item = form.save(commit=False); item.workspace = workspace; item.created_by = request.user; item.updated_by = request.user
        item.original_language = workspace.dialogue_language
        item.documentation_language = workspace.documentation_language
        item.dialogue_language = workspace.dialogue_language
        item.prompt_language = workspace.prompt_language
        item.save(); record_revision(instance=item, user=request.user, operation="CREATE")
        messages.success(request, "New project created.")
        return redirect("studio:project_detail", project_id=item.id)
    return render(request, "studio/entity_form.html", {"form": form, "title": "New project"})


@login_required
def character_create(request, project_id):
    project = get_object_or_404(accessible_projects(request.user), id=project_id)
    if not has_project_capability(request.user, project, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = CharacterCreateForm(request.POST or None, request.FILES or None)
    if request.method == "POST" and form.is_valid():
        with transaction.atomic():
            character = form.save(commit=False)
            character.project = project
            character.position = project.characters.count()
            character.created_by = request.user
            character.updated_by = request.user
            character.save()
            uploaded = form.cleaned_data.get("avatar_file")
            if uploaded:
                asset = create_asset(
                    user=request.user, workspace=project.workspace, project=project,
                    character=character, uploaded=uploaded, kind=Asset.Kind.CHARACTER_REFERENCE,
                )
                character.reference_assets.add(asset)
                character.avatar_asset = asset
                character.save(update_fields=["avatar_asset", "updated_at"])
            record_revision(instance=character, user=request.user, operation="CREATE")
        messages.success(request, "New character created.")
        return redirect("studio:character_detail", character_id=character.id)
    return render(request, "studio/character_form.html", {
        "form": form, "project": project, "title": "New character", "multipart": True,
        "translate_url": reverse("studio:localized_translate", kwargs={"project_id": project.id}),
    })


@login_required
def episode_create(request, project_id):
    project = get_object_or_404(accessible_projects(request.user), id=project_id)
    return _create_entity(request, form_class=EpisodeForm, parent=project, parent_field="project", workspace=project.workspace, title="New episode", success_url=lambda item: ("studio:project_detail", item.project_id), position_manager=project.episodes)


@login_required
def scene_create(request, episode_id):
    episode = get_object_or_404(Episode.objects.filter(project__in=accessible_projects(request.user)), id=episode_id)
    return _create_entity(request, form_class=SceneForm, parent=episode, parent_field="episode", workspace=episode.project.workspace, title="New scene", success_url=lambda item: ("studio:scene_detail", item.id), position_manager=episode.scenes)


def _renumber_episode_scenes(*, episode, user):
    scenes = list(Scene.objects.select_for_update().filter(episode=episode).order_by("position", "number", "id"))
    highest_number = max(
        Scene.all_objects.filter(episode=episode).values_list("number", flat=True),
        default=0,
    )
    temporary = highest_number + len(scenes) + 100
    for offset, scene in enumerate(scenes):
        scene.position = temporary + offset
        scene.number = temporary + offset
        scene.save(update_fields=["position", "number", "updated_at"])
    for position, scene in enumerate(scenes):
        scene.position = position
        scene.number = position + 1
        scene.updated_by = user
        scene.save(update_fields=["position", "number", "updated_by", "updated_at"])
    return scenes


@login_required
@transaction.atomic
def scene_copy(request, scene_id):
    source = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").prefetch_related(
            "dialogue_lines__translations", "prompts__blocks", "prompts__reference_assets",
            "reference_assets", "additional_generations__outputs",
        ).filter(episode__project__in=accessible_projects(request.user)),
        id=scene_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, source, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    episode = source.episode
    position = episode.scenes.count()
    copied = Scene.objects.create(
        episode=episode, number=position + 1, title=f"{source.title} copy"[:240],
        title_prompt=source.title_prompt, title_dialogue=source.title_dialogue,
        hook=source.hook, description=source.description, location=source.location,
        hook_prompt=source.hook_prompt, hook_dialogue=source.hook_dialogue,
        description_prompt=source.description_prompt, description_dialogue=source.description_dialogue,
        location_prompt=source.location_prompt, location_dialogue=source.location_dialogue,
        actions=source.actions, performance_notes=source.performance_notes,
        actions_prompt=source.actions_prompt, actions_dialogue=source.actions_dialogue,
        performance_notes_prompt=source.performance_notes_prompt,
        performance_notes_dialogue=source.performance_notes_dialogue,
        scene_type=source.scene_type, status=Scene.Status.DRAFT, position=position,
        status_comment=source.status_comment,
        created_by=request.user, updated_by=request.user,
    )
    references = list(source.reference_assets.all())
    if references:
        copied.reference_assets.add(*references)
        episode.project.media_assets.add(*references)

    line_map = {}
    for line in source.dialogue_lines.all():
        new_line = DialogueLine.objects.create(
            scene=copied, character=line.character, speaker=line.speaker, text=line.text, language=line.language,
            delivery=line.delivery, position=line.position, status=line.status,
            speaker_documentation=line.speaker_documentation, speaker_prompt=line.speaker_prompt,
            text_documentation=line.text_documentation, text_prompt=line.text_prompt,
            delivery_documentation=line.delivery_documentation, delivery_prompt=line.delivery_prompt,
            status_comment=line.status_comment,
            created_by=request.user, updated_by=request.user,
        )
        line_map[line.id] = new_line
        for translated in line.translations.all():
            TranslationUnit.objects.create(
                dialogue_line=new_line, source_text=translated.source_text,
                translated_text=translated.translated_text, target_language=translated.target_language,
                source_revision=translated.source_revision, status=translated.status,
                created_by=request.user, updated_by=request.user,
            )

    prompt_map = {}
    ordered_prompts = sorted(source.prompts.select_related("source_prompt").all(), key=lambda item: bool(item.source_prompt_id))
    for prompt in ordered_prompts:
        new_prompt = Prompt.objects.create(
            scene=copied, ai_model=prompt.ai_model, template=prompt.template,
            source_prompt=prompt_map.get(prompt.source_prompt_id), original_language=prompt.original_language,
            language=prompt.language, translation_scope=prompt.translation_scope, content=prompt.content,
            prompt_type=prompt.prompt_type, title=prompt.title, status=prompt.status,
            status_comment=prompt.status_comment,
            position=prompt.position, needs_review=prompt.needs_review,
            created_by=request.user, updated_by=request.user,
        )
        prompt_map[prompt.id] = new_prompt
        for block in prompt.blocks.all():
            PromptBlock.objects.create(
                prompt=new_prompt, block_type=block.block_type, content=block.content,
                source_dialogue=line_map.get(block.source_dialogue_id),
                translated_content=block.translated_content,
                translation_language=block.translation_language,
                translation_model=block.translation_model, position=block.position,
                created_by=request.user, updated_by=request.user,
            )
        prompt_references = list(prompt.reference_assets.all())
        if prompt_references:
            new_prompt.reference_assets.add(*prompt_references)
            episode.project.media_assets.add(*prompt_references)

    for generation in source.additional_generations.all():
        new_generation = AdditionalGeneration.objects.create(
            scene=copied, reason=generation.reason, source_asset=generation.source_asset,
            prompt=generation.prompt, position=generation.position, status=generation.status,
            status_comment=generation.status_comment,
            created_by=request.user, updated_by=request.user,
        )
        for output in generation.outputs.all():
            GenerationOutput.objects.create(
                generation=new_generation, asset=output.asset, model_metadata=output.model_metadata,
                position=output.position, is_final=output.is_final,
                created_by=request.user, updated_by=request.user,
            )
            episode.project.media_assets.add(output.asset)
    record_revision(instance=copied, user=request.user, operation="COPY")
    audit(
        workspace=episode.project.workspace, actor=request.user, action="SCENE_COPIED",
        instance=copied, metadata={"sourceSceneId": str(source.id)},
    )
    messages.success(request, f"Scene {copied.number} copied to the end of the episode.")
    return redirect("studio:scene_detail", scene_id=copied.id)


@login_required
@transaction.atomic
def scene_delete(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__in=accessible_projects(request.user)
        ), id=scene_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, scene, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    episode = scene.episode
    old_position = scene.position
    scene.deleted_at = timezone.now()
    scene.deleted_by = request.user
    scene.updated_by = request.user
    scene.number = max(Scene.all_objects.filter(episode=episode).values_list("number", flat=True), default=0) + 1000
    scene.position = scene.number
    scene.save(update_fields=["deleted_at", "deleted_by", "updated_by", "number", "position", "updated_at"])
    remaining = _renumber_episode_scenes(episode=episode, user=request.user)
    audit(
        workspace=episode.project.workspace, actor=request.user, action="SCENE_DELETED",
        instance=scene, metadata={"remainingSceneIds": [str(item.id) for item in remaining]},
    )
    messages.success(request, "Scene deleted and remaining scene numbers updated.")
    if remaining:
        return redirect("studio:scene_detail", scene_id=remaining[min(old_position, len(remaining) - 1)].id)
    return redirect("studio:project_detail", project_id=episode.project_id)


def _dialogue_return_to(request, scene, line=None):
    requested = request.POST.get("return_to") or request.GET.get("return_to") or ""
    if requested and url_has_allowed_host_and_scheme(requested, {request.get_host()}, require_https=request.is_secure()):
        return requested
    anchor = f"dialogue-{line.id}" if line else "dialogue"
    return f"{reverse('studio:scene_detail', kwargs={'scene_id': scene.id})}#{anchor}"


@login_required
def dialogue_create(request, scene_id):
    scene = get_object_or_404(Scene.objects.filter(episode__project__in=accessible_projects(request.user)), id=scene_id)
    if not has_object_capability(request.user, scene, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    project = scene.episode.project
    initial = {"language": project.dialogue_language}
    return_to = _dialogue_return_to(request, scene)
    form = DialogueLineForm(request.POST or None, initial=initial, project=project)
    if request.method == "POST" and form.is_valid():
        line = form.save(commit=False)
        line.scene = scene
        line.position = scene.dialogue_lines.count()
        line.language = project.dialogue_language
        line.created_by = request.user
        line.updated_by = request.user
        line.save()
        record_revision(instance=line, user=request.user, operation="CREATE")
        messages.success(request, "Dialogue line created.")
        return HttpResponseRedirect(return_to)
    return render(request, "studio/dialogue_form.html", {
        "form": form, "project": project, "scene": scene, "title": "New dialogue line",
        "translate_url": reverse("studio:localized_translate", kwargs={"project_id": project.id}),
        "return_to": return_to,
    })


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


def _image_upload(request, *, target, workspace, title, success_url, kind, project=None, scene=None, character=None, episode=None):
    if not has_object_capability(request.user, target, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = MultipleImageUploadForm(request.POST or None, request.FILES or None, limit=10)
    if request.method == "POST" and form.is_valid():
        image_role = request.POST.get("image_role", "")
        avatar_roles = {"workspace_avatar", "character_avatar", "episode_avatar", "project_cover"}
        defer_avatar_assignment = (
            image_role in avatar_roles
            and request.headers.get("X-Requested-With") == "XMLHttpRequest"
        )
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
                if episode is not None:
                    episode.cover_assets.add(asset)
                    EpisodeCover.objects.get_or_create(
                        episode=episode,
                        asset=asset,
                        defaults={
                            "language_code": episode.language or episode.project.dialogue_language or "EN",
                            "created_by": request.user,
                            "updated_by": request.user,
                        },
                    )
                    if not defer_avatar_assignment and not episode.avatar_asset_id:
                        episode.avatar_asset = asset
                        episode.updated_by = request.user
                        episode.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
        if first_asset and defer_avatar_assignment:
            return JsonResponse({
                "assetId": str(first_asset.id),
                "name": first_asset.original_filename,
                "viewUrl": reverse("studio_api:asset_view", kwargs={"asset_id": first_asset.id}),
                "thumbnailUrl": reverse("studio_api:asset_thumbnail", kwargs={"asset_id": first_asset.id}),
                "cropUrl": reverse("studio:asset_crop", kwargs={"asset_id": first_asset.id}),
                "attachScope": image_role,
                "attachOwnerId": str(target.id),
            }, status=201)
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
        elif first_asset and image_role == "episode_avatar" and isinstance(target, Episode):
            target.avatar_asset = first_asset
            target.updated_by = request.user
            target.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
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
    ).select_related("project", "created_by", "updated_by").prefetch_related(
        "projects",
        Prefetch("referenced_by_scenes", queryset=Scene.objects.select_related("episode")),
        Prefetch("referenced_by_characters", queryset=Character.objects.select_related("project")),
        Prefetch("referenced_by_prompts", queryset=Prompt.objects.select_related("scene__episode")),
        "project_cover_for",
        Prefetch("character_avatar_for", queryset=Character.objects.select_related("project")),
        Prefetch("cover_for_episodes", queryset=Episode.objects.select_related("project")),
        Prefetch("episode_avatar_for", queryset=Episode.objects.select_related("project")),
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


def _project_copy_targets(user):
    workspaces = accessible_workspaces(user).select_related("owner")
    if user.is_superuser:
        return list(workspaces.order_by("name"))
    return list(
        workspaces.filter(
            Q(owner=user)
            | Q(
                memberships__user=user,
                memberships__status=WorkspaceMembership.Status.ACTIVE,
                memberships__role__in=[
                    WorkspaceMembership.Role.OWNER,
                    WorkspaceMembership.Role.ADMIN,
                ],
            )
        ).distinct().order_by("name")
    )


def _project_header_context(user, project):
    return {
        "can_edit": has_project_capability(user, project, "edit"),
        "can_manage_project": has_project_capability(user, project, "manage_project"),
        "can_copy_project": has_project_capability(user, project, "manage_project"),
        "can_administer": is_workspace_owner_or_admin(user, project.workspace),
        "can_remove_project_access": user.id == project.workspace.owner_id,
        "copy_target_workspaces": _project_copy_targets(user),
    }


@login_required
def project_edit(request, project_id):
    item = get_object_or_404(accessible_projects(request.user), id=project_id)
    if not has_object_capability(request.user, item, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    TrackFormSet = inlineformset_factory(
        Project, RecommendedTrack, form=RecommendedTrackForm, extra=0, can_delete=True,
    )
    form = ProjectForm(request.POST or None, instance=item)
    track_formset = TrackFormSet(request.POST or None, instance=item, prefix="tracks")
    if request.method == "POST" and form.is_valid() and track_formset.is_valid():
        with transaction.atomic():
            item = form.save(commit=False)
            item.updated_by = request.user
            item.full_clean()
            item.save()
            tracks = track_formset.save(commit=False)
            for removed in track_formset.deleted_objects:
                removed.delete()
            for position, track in enumerate(tracks):
                track.project = item
                track.position = position
                if not track.created_by_id:
                    track.created_by = request.user
                track.updated_by = request.user
                track.save()
            record_revision(instance=item, user=request.user, operation="UPDATE")
        messages.success(request, "Project saved.")
        return redirect("studio:project_edit", project_id=item.id)
    return render(request, "studio/project_edit.html", {
        "project": item, "form": form, "track_formset": track_formset,
        **_project_header_context(request.user, item),
    })


@login_required
def project_settings(request, project_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    if not has_project_capability(request.user, project, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    original_language = project.original_language
    requested_original_language = (request.POST.get("original_language") or original_language).upper()
    original_language_changed = request.method == "POST" and requested_original_language != original_language.upper()
    propagation_confirmed = request.POST.get("confirm_language_propagation") == "on"
    form = ProjectSettingsForm(request.POST or None, instance=project)
    if original_language_changed and not propagation_confirmed:
        form.add_error(
            None,
            "Confirm that the new language must be propagated to original prompts, translations, and dialogue lines.",
        )
    if request.method == "POST" and form.is_valid():
        with transaction.atomic():
            project = form.save(commit=False)
            project.original_language = requested_original_language
            project.updated_by = request.user
            project.full_clean()
            project.save()
            if original_language_changed:
                counts = propagate_project_original_language(
                    project=project,
                    language=requested_original_language,
                    user=request.user,
                )
            record_revision(instance=project, user=request.user, operation="SETTINGS_UPDATE")
        messages.success(request, "Project settings saved")
        if original_language_changed:
            messages.success(
                request,
                "Updated "
                f"{counts['originalPrompts']} original prompts, "
                f"{counts['translations']} translations, and "
                f"{counts['dialogueLines']} dialogue lines",
            )
        return redirect("studio:project_settings", project_id=project.id)
    return render(request, "studio/project_settings.html", {
        "project": project, "form": form,
        **_project_header_context(request.user, project),
    })


@login_required
def character_edit(request, character_id):
    item = get_object_or_404(Character.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)), id=character_id)
    return _edit_entity(request, item=item, form_class=CharacterForm, workspace=item.project.workspace, title="Edit character", success_url=lambda value: ("studio:project_detail", value.project_id))


@login_required
@transaction.atomic
def character_copy(request, character_id):
    source = get_object_or_404(
        Character.objects.select_related("project__workspace", "avatar_asset").prefetch_related("reference_assets").filter(
            project__in=accessible_projects(request.user)
        ),
        id=character_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, source, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    base = f"{source.name} copy"[:160]
    name = base
    suffix = 2
    while Character.all_objects.filter(project=source.project, name=name).exists():
        name = f"{base} {suffix}"[:180]
        suffix += 1
    copied = Character.objects.create(
        project=source.project,
        name=name,
        name_prompt=source.name_prompt,
        name_dialogue=source.name_dialogue,
        description=source.description,
        description_prompt=source.description_prompt,
        description_dialogue=source.description_dialogue,
        visual_description=source.visual_description,
        visual_description_prompt=source.visual_description_prompt,
        visual_description_dialogue=source.visual_description_dialogue,
        position=source.project.characters.count(),
        avatar_asset=source.avatar_asset,
        created_by=request.user,
        updated_by=request.user,
    )
    references = list(source.reference_assets.all())
    if references:
        copied.reference_assets.add(*references)
        copied.project.media_assets.add(*references)
    record_revision(instance=copied, user=request.user, operation="COPY")
    audit(
        workspace=source.project.workspace,
        actor=request.user,
        action="CHARACTER_COPIED",
        instance=copied,
        metadata={"sourceCharacterId": str(source.id)},
    )
    messages.success(request, "Character copied to the end of the project character list.")
    return redirect("studio:character_detail", character_id=copied.id)


@login_required
@transaction.atomic
def character_delete(request, character_id):
    character = get_object_or_404(
        Character.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)),
        id=character_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, character, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    project = character.project
    character.deleted_at = timezone.now()
    character.deleted_by = request.user
    character.updated_by = request.user
    character.save(update_fields=["deleted_at", "deleted_by", "updated_by", "updated_at"])
    for position, item in enumerate(project.characters.order_by("position", "id")):
        if item.position != position:
            item.position = position
            item.save(update_fields=["position", "updated_at"])
    audit(workspace=project.workspace, actor=request.user, action="CHARACTER_DELETED", instance=character)
    messages.success(request, "Character deleted. Its Workspace images were preserved.")
    return redirect("studio:project_detail", project_id=project.id)


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
        "project": character.project,
        "character_form": form,
        "can_edit": can_edit,
        "project_assets": project_assets,
        "workspace_assets": workspace_assets,
        "translate_url": reverse("studio:localized_translate", kwargs={"project_id": character.project_id}),
    })


def _asset_attachment_target(user, scope, owner_id):
    if scope in {"workspace", "workspace_avatar"}:
        owner = get_object_or_404(accessible_workspaces(user), id=owner_id)
        project = None
        workspace = owner
    elif scope == "project_cover":
        owner = get_object_or_404(
            accessible_projects(user).select_related("workspace"), id=owner_id
        )
        project = owner
        workspace = owner.workspace
    elif scope == "project":
        owner = get_object_or_404(
            accessible_projects(user).select_related("workspace"), id=owner_id
        )
        project = owner
        workspace = owner.workspace
    elif scope == "scene":
        owner = get_object_or_404(
            Scene.objects.select_related("episode__project__workspace").filter(
                episode__project__in=accessible_projects(user)
            ), id=owner_id,
        )
        project = owner.episode.project
        workspace = project.workspace
    elif scope in {"character", "character_avatar"}:
        owner = get_object_or_404(
            Character.objects.select_related("project__workspace").filter(
                project__in=accessible_projects(user)
            ), id=owner_id,
        )
        project = owner.project
        workspace = project.workspace
    elif scope in {"episode", "episode_avatar"}:
        owner = get_object_or_404(
            Episode.objects.select_related("project__workspace").filter(
                project__in=accessible_projects(user)
            ), id=owner_id,
        )
        project = owner.project
        workspace = project.workspace
    else:
        return None
    return owner, project, workspace


def _apply_asset_attachment(*, user, scope, owner, project, workspace, asset):
    if project is not None:
        asset.projects.add(project)
    if scope == "workspace_avatar":
        owner.avatar_asset = asset
        owner.updated_by = user
        owner.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
    elif scope == "workspace":
        pass
    elif scope == "project_cover":
        owner.cover_asset = asset
        owner.updated_by = user
        owner.save(update_fields=["cover_asset", "updated_by", "updated_at"])
    elif scope == "project":
        pass
    elif scope in {"episode", "episode_avatar"}:
        owner.cover_assets.add(asset)
        EpisodeCover.objects.get_or_create(
            episode=owner,
            asset=asset,
            defaults={
                "language_code": owner.language or owner.project.dialogue_language or "EN",
                "created_by": user,
                "updated_by": user,
            },
        )
        if scope == "episode_avatar" or not owner.avatar_asset_id:
            owner.avatar_asset = asset
            owner.updated_by = user
            owner.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
    elif scope == "scene":
        owner.reference_assets.add(asset)
    else:
        owner.reference_assets.add(asset)
    if scope in {"scene", "character", "character_avatar"}:
        asset.updated_by = user
        asset.save(update_fields=["updated_by", "updated_at"])
    if scope == "character_avatar":
        owner.avatar_asset = asset
        owner.updated_by = user
        owner.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
    audit(
        workspace=workspace,
        actor=user,
        action="ASSET_ATTACHED",
        instance=asset,
        metadata={"scope": scope, "ownerId": str(owner.id)},
    )


@login_required
def asset_attach(request, scope, owner_id):
    target = _asset_attachment_target(request.user, scope, owner_id)
    if target is None:
        return HttpResponseForbidden("Unsupported image attachment scope.")
    owner, project, workspace = target
    permitted = (
        has_capability(request.user, owner, "manage_members")
        if scope in {"workspace", "workspace_avatar"}
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
    _apply_asset_attachment(
        user=request.user, scope=scope, owner=owner, project=project, workspace=workspace, asset=asset,
    )
    messages.success(request, "Image selection saved.")
    requested = request.POST.get("next", "")
    if requested and url_has_allowed_host_and_scheme(requested, {request.get_host()}, require_https=request.is_secure()):
        return HttpResponseRedirect(requested)
    if scope in {"workspace", "workspace_avatar"}:
        return redirect("studio:workspace_detail", workspace_id=owner.id)
    if scope == "project_cover":
        return redirect("studio:workspace_detail", workspace_id=workspace.id)
    if scope == "project":
        return redirect("studio:project_detail", project_id=owner.id)
    if scope == "scene":
        return redirect("studio:scene_detail", scene_id=owner.id)
    return redirect("studio:character_detail", character_id=owner.id)


@login_required
def asset_url_import(request, scope, owner_id):
    target = _asset_attachment_target(request.user, scope, owner_id)
    if target is None:
        return HttpResponseForbidden("Unsupported image attachment scope.")
    owner, project, workspace = target
    permitted = (
        has_capability(request.user, owner, "manage_members")
        if scope in {"workspace", "workspace_avatar"}
        else has_object_capability(request.user, owner, "edit")
    )
    if request.method != "POST" or not permitted:
        return HttpResponseForbidden("Edit permission is required.")
    try:
        uploaded = download_external_image(request.POST.get("image_url", ""))
        asset = create_asset(
            user=request.user,
            workspace=workspace,
            uploaded=uploaded,
            kind=Asset.Kind.OTHER,
            prevent_duplicate=True,
        )
        _apply_asset_attachment(
            user=request.user, scope=scope, owner=owner, project=project, workspace=workspace, asset=asset,
        )
    except ValidationError as exc:
        messages.error(request, "; ".join(exc.messages))
    else:
        messages.success(request, "External image imported and selected.")
    requested = request.POST.get("next", "")
    if requested and url_has_allowed_host_and_scheme(requested, {request.get_host()}, require_https=request.is_secure()):
        return HttpResponseRedirect(requested)
    if project is not None:
        return redirect("studio:project_detail", project_id=project.id)
    return redirect("studio:workspace_detail", workspace_id=workspace.id)


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
def character_remove_avatar(request, character_id):
    character = get_object_or_404(
        Character.objects.select_related("project__workspace").filter(
            project__in=accessible_projects(request.user)
        ),
        id=character_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, character, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    character.avatar_asset = None
    character.updated_by = request.user
    character.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
    audit(
        workspace=character.project.workspace,
        actor=request.user,
        action="CHARACTER_AVATAR_REMOVED",
        instance=character,
    )
    if request.headers.get("X-Requested-With") == "XMLHttpRequest":
        return JsonResponse({"ok": True, "characterId": str(character.id)})
    messages.success(request, "Character avatar removed")
    requested = request.POST.get("next", "")
    if requested and url_has_allowed_host_and_scheme(
        requested, {request.get_host()}, require_https=request.is_secure()
    ):
        return HttpResponseRedirect(requested)
    return redirect("studio:character_detail", character_id=character.id)


@login_required
def episode_edit(request, episode_id):
    item = get_object_or_404(
        Episode.objects.select_related("project__workspace", "avatar_asset").prefetch_related(
            Prefetch("cover_entries", queryset=EpisodeCover.objects.select_related("asset"))
        ).filter(project__in=accessible_projects(request.user)),
        id=episode_id,
    )
    if not has_object_capability(request.user, item, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = EpisodeForm(request.POST or None, instance=item)
    if request.method == "POST" and form.is_valid():
        item = form.save(commit=False)
        item.updated_by = request.user
        item.full_clean()
        item.save()
        record_revision(instance=item, user=request.user, operation="UPDATE")
        messages.success(request, "Episode saved")
        return redirect("studio:episode_edit", episode_id=item.id)
    project_assets, workspace_assets = _picker_assets(request.user, item.project)
    return render(request, "studio/episode_edit.html", {
        "episode": item,
        "project": item.project,
        "form": form,
        "can_edit": True,
        "project_assets": project_assets,
        "workspace_assets": workspace_assets,
        "cover_languages": PROMPT_LANGUAGES,
        "cover_platforms": EpisodeCover.Platform.choices,
        "text_models": active_text_models(),
        "default_text_model": project_text_model_id(item.project),
    })


@login_required
def episode_comic_generate(request, episode_id):
    episode = get_object_or_404(Episode.objects.select_related("project__workspace").filter(project__in=accessible_projects(request.user)), id=episode_id)
    if request.method != "POST" or not has_object_capability(request.user, episode, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    model = selected_text_model(request.POST.get("model") or project_text_model_id(episode.project))
    comic = EpisodeComic.objects.create(episode=episode, model=model, created_by=request.user)
    try:
        scene_text = []
        reference_images = []
        max_references = int(getattr(settings, "AI_MAX_COMIC_REFERENCE_IMAGES", 20))
        scenes = episode.scenes.prefetch_related("dialogue_lines", "assets", "reference_assets").order_by("position", "number")
        for scene in scenes:
            dialogue = "\n".join(f"{line.speaker}: {line.text}" for line in scene.dialogue_lines.all())
            scene_asset_map = {
                asset.id: asset for asset in [*scene.assets.all(), *scene.reference_assets.all()]
                if asset.deleted_at is None and asset.content_type.startswith("image/") and asset.file.name
            }
            image_labels = []
            for asset in scene_asset_map.values():
                if len(reference_images) >= max_references:
                    break
                try:
                    with asset.file.open("rb") as source:
                        image_bytes = source.read()
                except OSError:
                    continue
                reference_images.append((asset.original_filename, image_bytes, asset.content_type))
                image_labels.append(f"reference image {len(reference_images)} ({asset.original_filename})")
            image_note = f"\nVisual references: {', '.join(image_labels)}" if image_labels else ""
            scene_text.append(f"Scene {scene.number}: {scene.title}\n{scene.description}\n{dialogue}{image_note}")
        request_text = (
            "Create concise comic panel narration. Preserve scene order, dialogue facts, character identity, "
            "and visible details from the supplied reference images. Associate each numbered image with the "
            "scene that names it. Return text only.\n\n" + "\n\n".join(scene_text)
        )
        if reference_images:
            plan, used_model = _run_logged_multimodal_text(
                workspace=episode.project.workspace, user=request.user, action="GENERATE_COMIC_PLAN",
                model=model, text=request_text, reference_images=reference_images,
            )
        else:
            plan, used_model = _run_logged_text(
                workspace=episode.project.workspace, user=request.user, action="GENERATE_COMIC_PLAN",
                model=model, text=request_text,
            )
        uploaded = SimpleUploadedFile(
            f"{slugify(episode.project.title)}-episode-{episode.number}-comic.pdf",
            build_episode_comic_pdf(episode, plan), content_type="application/pdf",
        )
        asset = create_asset(user=request.user, workspace=episode.project.workspace, uploaded=uploaded, kind=Asset.Kind.EXPORT, project=episode.project)
        asset.ai_metadata = {"kind": "episode_comic", "episodeId": str(episode.id), "model": used_model}
        asset.save(update_fields=["ai_metadata", "updated_at"])
        comic.asset = asset; comic.status = EpisodeComic.Status.SUCCESS; comic.model = used_model
        comic.save(update_fields=["asset", "status", "model", "updated_at"])
        messages.success(request, "Comic generated")
    except Exception as exc:
        comic.status = EpisodeComic.Status.ERROR; comic.error_message = str(exc)[:2000]
        comic.save(update_fields=["status", "error_message", "updated_at"])
        messages.error(request, "Comic generation failed")
    return redirect("studio:episode_edit", episode_id=episode.id)


@login_required
def episode_comic_detach(request, comic_id):
    comic = get_object_or_404(EpisodeComic.objects.select_related("episode__project__workspace", "asset").filter(episode__project__in=accessible_projects(request.user)), id=comic_id)
    episode_id = comic.episode_id
    if request.method != "POST" or not has_object_capability(request.user, comic.episode, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    if request.POST.get("trash") == "1" and comic.asset:
        trash_asset(asset=comic.asset, user=request.user)
    comic.delete()
    messages.success(request, "Comic detached")
    return redirect("studio:episode_edit", episode_id=episode_id)


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
    return_to = _dialogue_return_to(request, item.scene, line=item)
    form = DialogueLineForm(request.POST or None, instance=item, project=item.scene.episode.project)
    if request.method == "POST" and form.is_valid():
        update_dialogue_line(
            line=item, user=request.user, text=form.cleaned_data["text"],
            speaker=form.cleaned_data["speaker"], delivery=form.cleaned_data["delivery"],
            language=form.cleaned_data["language"], status=form.cleaned_data["status"],
            speaker_documentation=form.cleaned_data["speaker_documentation"],
            speaker_prompt=form.cleaned_data["speaker_prompt"],
            text_documentation=form.cleaned_data["text_documentation"],
            text_prompt=form.cleaned_data["text_prompt"],
            delivery_documentation=form.cleaned_data["delivery_documentation"],
            delivery_prompt=form.cleaned_data["delivery_prompt"],
            status_comment=form.cleaned_data["status_comment"],
        )
        messages.success(request, "Dialogue line saved.")
        return HttpResponseRedirect(return_to)
    return render(request, "studio/dialogue_form.html", {
        "form": form, "title": "Edit dialogue line", "project": item.scene.episode.project,
        "scene": item.scene,
        "translate_url": reverse("studio:localized_translate", kwargs={"project_id": item.scene.episode.project_id}),
        "return_to": return_to,
    })


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
def episode_image_upload(request, episode_id):
    episode = get_object_or_404(
        Episode.objects.select_related("project__workspace").filter(
            project__in=accessible_projects(request.user)
        ), id=episode_id,
    )
    return _image_upload(
        request, target=episode, workspace=episode.project.workspace,
        title="Upload episode covers", success_url=lambda value: ("studio:project_detail", value.project_id),
        kind=Asset.Kind.OTHER, project=episode.project, episode=episode,
    )
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
def docx_import_archive(request, import_id):
    document = get_object_or_404(
        DocxImport.objects.select_related("workspace", "source_asset").filter(
            workspace__in=accessible_workspaces(request.user),
        ),
        id=import_id,
    )
    if request.method != "POST" or not has_capability(request.user, document.workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required")
    restore = request.POST.get("action") == "restore"
    document.archived_at = None if restore else timezone.now()
    document.archived_by = None if restore else request.user
    document.save(update_fields=["archived_at", "archived_by"])
    audit(
        workspace=document.workspace,
        actor=request.user,
        action="DOCUMENT_RESTORED" if restore else "DOCUMENT_ARCHIVED",
        instance=document.source_asset,
        metadata={"importId": str(document.id)},
    )
    messages.success(request, "Document restored" if restore else "Document archived")
    return HttpResponseRedirect(
        reverse("studio:workspace_detail", kwargs={"workspace_id": document.workspace_id}) + "#documents"
    )


@login_required
def docx_import_trash(request, import_id):
    document = get_object_or_404(
        DocxImport.objects.select_related("workspace", "source_asset").filter(
            workspace__in=accessible_workspaces(request.user),
        ),
        id=import_id,
    )
    if request.method != "POST" or not has_capability(request.user, document.workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required")
    trash_asset(asset=document.source_asset, user=request.user)
    audit(
        workspace=document.workspace,
        actor=request.user,
        action="DOCUMENT_TRASHED",
        instance=document.source_asset,
        metadata={"importId": str(document.id)},
    )
    messages.success(request, "Document moved to Recycle Bin")
    return HttpResponseRedirect(
        reverse("studio:workspace_detail", kwargs={"workspace_id": document.workspace_id}) + "#documents"
    )
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
        accessible_projects(request.user).select_related("workspace", "created_by", "updated_by", "cover_asset").prefetch_related("media_assets", "assets", "characters__avatar_asset", "characters__reference_assets", Prefetch("episodes__cover_entries", queryset=EpisodeCover.objects.select_related("asset")), "episodes__scenes", "memberships__user", "recommended_tracks"),
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
def project_document_archive(request, project_id, import_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    document = get_object_or_404(DocxImport.objects.select_related("source_asset"), id=import_id, project=project)
    if request.method != "POST" or not has_project_capability(request.user, project, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    restore = request.POST.get("action") == "restore"
    document.archived_at = None if restore else timezone.now()
    document.archived_by = None if restore else request.user
    document.save(update_fields=["archived_at", "archived_by"])
    audit(
        workspace=project.workspace,
        actor=request.user,
        action="PROJECT_DOCUMENT_RESTORED" if restore else "PROJECT_DOCUMENT_ARCHIVED",
        instance=document.source_asset,
        metadata={"importId": str(document.id), "projectId": str(project.id)},
    )
    messages.success(request, "Document restored from archive." if restore else "Document archived.")
    return HttpResponseRedirect(reverse("studio:project_detail", kwargs={"project_id": project.id}) + "#documents")


@login_required
def project_document_trash(request, project_id, import_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    document = get_object_or_404(DocxImport.objects.select_related("source_asset"), id=import_id, project=project)
    if request.method != "POST" or not has_project_capability(request.user, project, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    trash_asset(asset=document.source_asset, user=request.user)
    audit(
        workspace=project.workspace,
        actor=request.user,
        action="PROJECT_DOCUMENT_TRASHED",
        instance=document.source_asset,
        metadata={"importId": str(document.id), "projectId": str(project.id)},
    )
    messages.success(request, "Document moved to Recycle Bin.")
    return HttpResponseRedirect(reverse("studio:project_detail", kwargs={"project_id": project.id}) + "#documents")


@login_required
def project_access(request, project_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    if not has_project_capability(request.user, project, "manage_project"):
        return HttpResponseForbidden("Full control permission is required.")
    action = request.POST.get("action", "save") if request.method == "POST" else "save"
    form = ProjectMembershipForm(request.POST if action == "save" else None, project=project)
    bulk_form = ProjectBulkMembershipForm(request.POST if action == "bulk_add" else None, project=project)
    selection_form = ProjectUserSelectionForm(
        request.POST if action == "select_users" else None,
        project=project,
    ) if _is_server_super_admin(request.user) else None
    if request.method == "POST":
        if action in {"remove", "exclude"} and request.user.id != project.workspace.owner_id:
            return HttpResponseForbidden("Only the workspace owner can remove project access.")
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
        if action == "bulk_add" and bulk_form.is_valid():
            added = 0
            updated = 0
            role = bulk_form.cleaned_data["role"]
            for user in bulk_form.users:
                existed = ProjectMembership.objects.filter(project=project, user=user, is_active=True).exists()
                ProjectAccessExclusion.objects.filter(project=project, user=user).delete()
                ProjectMembership.objects.update_or_create(
                    project=project,
                    user=user,
                    defaults={"role": role, "is_active": True, "invited_by": request.user},
                )
                if existed:
                    updated += 1
                else:
                    added += 1
                    notify_access_granted(
                        user=user,
                        entity_name=project.title,
                        entity_kind="project",
                        url=request.build_absolute_uri(reverse("studio:project_detail", kwargs={"project_id": project.id})),
                        granted_by=request.user,
                    )
            summary = f"Bulk access saved: {added} added, {updated} updated"
            if bulk_form.missing_emails:
                summary += f", {len(bulk_form.missing_emails)} not registered"
            messages.success(request, summary)
            return redirect("studio:project_access", project_id=project.id)
        if action == "select_users" and selection_form is not None and selection_form.is_valid():
            added = 0
            updated = 0
            role = selection_form.cleaned_data["role"]
            for user in selection_form.cleaned_data["users"]:
                existed = ProjectMembership.objects.filter(project=project, user=user, is_active=True).exists()
                ProjectAccessExclusion.objects.filter(project=project, user=user).delete()
                ProjectMembership.objects.update_or_create(
                    project=project,
                    user=user,
                    defaults={"role": role, "is_active": True, "invited_by": request.user},
                )
                if existed:
                    updated += 1
                else:
                    added += 1
                    notify_access_granted(
                        user=user,
                        entity_name=project.title,
                        entity_kind="project",
                        url=request.build_absolute_uri(reverse("studio:project_detail", kwargs={"project_id": project.id})),
                        granted_by=request.user,
                    )
            messages.success(request, f"Selected access saved: {added} added, {updated} updated")
            return redirect("studio:project_access", project_id=project.id)
        if action == "save" and form.is_valid():
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
        "bulk_form": bulk_form,
        "selection_form": selection_form,
        "memberships": project.memberships.select_related("user"),
        "inherited_memberships": inherited,
        "can_remove_access": request.user.id == project.workspace.owner_id,
    })


@login_required
def scene_detail(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace", "episode__project__cover_asset").prefetch_related(
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
        "project": scene.episode.project,
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
        "translate_url": reverse("studio:localized_translate", kwargs={"project_id": scene.episode.project_id}),
        **_project_header_context(request.user, scene.episode.project),
        **_prompt_editor_context(request, scene.episode.project),
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
        **_prompt_editor_context(request, prompt.scene.episode.project),
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
        model_id = selected_text_model(request.POST.get("text_model") or project_text_model_id(prompt.scene.episode.project))
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
        model_id = selected_text_model(request.POST.get("text_model") or project_text_model_id(prompt.scene.episode.project))
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
        model_id = selected_text_model(request.POST.get("text_model") or project_text_model_id(prompt.scene.episode.project))
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
        model_id = selected_text_model(data.get("textModel") or project_text_model_id(prompt.scene.episode.project))
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
            prompt_improvement=data.get("action") == "improve_prompt",
        )
    except (json.JSONDecodeError, StudioAiError, ProviderError, ValidationError) as exc:
        return JsonResponse({"error": str(exc)}, status=400)
    except Exception:
        return JsonResponse({"error": "The translation service returned an unexpected error. Please retry."}, status=500)
    response_language = (
        prompt.language or prompt.original_language
        if data.get("action") == "improve_prompt"
        else data.get("targetLanguage", "")
    )
    return JsonResponse({
        "content": content,
        "language": str(response_language).upper(),
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
        "default_text_model": project_text_model_id(project),
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
            sequence = Scene.objects.filter(episode=episode, scene_type=Scene.Type.ADDITIONAL_GENERATION).count() + 1
            last_number = max(episode.scenes.values_list("number", flat=True), default=0)
            generated_scene = Scene.objects.create(
                episode=episode, number=last_number + 1, title=f"Additional generation {sequence}",
                hook=f"Additional generation based on scene {scene.number}", description=form.cleaned_data["reason"],
                position=episode.scenes.count(), status=Scene.Status.DRAFT,
                scene_type=Scene.Type.ADDITIONAL_GENERATION,
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


@login_required
def asset_star_toggle(request, asset_id):
    asset = _asset_for_edit(request, asset_id)
    if request.method != "POST" or not has_object_capability(request.user, asset, "edit"):
        return JsonResponse({"error": "Edit permission is required."}, status=403)
    asset.is_starred = not asset.is_starred
    asset.updated_by = request.user
    asset.save(update_fields=["is_starred", "updated_by", "updated_at"])
    audit(
        workspace=asset.workspace,
        actor=request.user,
        action="ASSET_STAR_UPDATED",
        instance=asset,
        metadata={"starred": asset.is_starred},
    )
    return JsonResponse({"assetId": str(asset.id), "starred": asset.is_starred})


def _asset_for_edit(request, asset_id, include_deleted=False):
    return get_object_or_404(
        accessible_assets(request.user, include_deleted=include_deleted)
        .select_related("workspace", "project", "scene", "character", "prompt"),
        id=asset_id,
    )


def _asset_original_exists(asset):
    if not asset.file.name:
        return False
    try:
        return asset.file.storage.exists(asset.file.name)
    except OSError:
        return False


def _detach_asset_display_links(asset, user):
    now = timezone.now()
    Workspace.all_objects.filter(avatar_asset=asset).update(
        avatar_asset=None, updated_by=user, updated_at=now,
    )
    Project.all_objects.filter(cover_asset=asset).update(
        cover_asset=None, updated_by=user, updated_at=now,
    )
    Character.all_objects.filter(avatar_asset=asset).update(
        avatar_asset=None, updated_by=user, updated_at=now,
    )
    Episode.all_objects.filter(avatar_asset=asset).update(
        avatar_asset=None, updated_by=user, updated_at=now,
    )
    EpisodeCover.all_objects.filter(asset=asset).delete()
    asset.projects.clear()
    asset.referenced_by_scenes.clear()
    asset.referenced_by_characters.clear()
    asset.referenced_by_prompts.clear()
    asset.cover_for_episodes.clear()


def _asset_usage_project_ids(asset):
    project_ids = {project.id for project in asset.projects.all()}
    project_ids.update(scene.episode.project_id for scene in asset.referenced_by_scenes.all())
    project_ids.update(character.project_id for character in asset.referenced_by_characters.all())
    project_ids.update(prompt.scene.episode.project_id for prompt in asset.referenced_by_prompts.all())
    project_ids.update(project.id for project in asset.project_cover_for.all())
    project_ids.update(character.project_id for character in asset.character_avatar_for.all())
    project_ids.update(episode.project_id for episode in asset.cover_for_episodes.all())
    project_ids.update(episode.project_id for episode in asset.episode_avatar_for.all())
    return {value for value in project_ids if value}


def _asset_usage_count(asset):
    return (
        len(asset.projects.all())
        + len(asset.referenced_by_scenes.all())
        + len(asset.referenced_by_characters.all())
        + len(asset.referenced_by_prompts.all())
        + len(asset.project_cover_for.all())
        + len(asset.character_avatar_for.all())
        + len(asset.cover_for_episodes.all())
        + len(asset.episode_avatar_for.all())
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
        messages.success(request, "File moved to Recycle Bin.")
    return _asset_action_redirect(request, asset)


@login_required
def asset_crop(request, asset_id):
    asset = _asset_for_edit(request, asset_id)
    if request.method != "POST":
        return JsonResponse({"error": "Edit permission is required."}, status=403)
    try:
        data = json.loads(request.body or b"{}")
        attach_scope = str(data.get("attachScope") or "")
        attach_owner_id = data.get("attachOwnerId")
        attachment_target = None
        attachment_permitted = False
        if attach_scope:
            if attach_scope not in {"workspace_avatar", "character_avatar", "episode_avatar", "project_cover"}:
                raise ValidationError("This crop target is not supported.")
            attachment_target = _asset_attachment_target(request.user, attach_scope, attach_owner_id)
            if attachment_target is None:
                raise ValidationError("The crop target is not available.")
            owner, project, workspace = attachment_target
            if workspace.id != asset.workspace_id:
                raise ValidationError("The image and crop target must belong to the same workspace.")
            attachment_permitted = (
                has_capability(request.user, owner, "manage_members")
                if attach_scope == "workspace_avatar"
                else has_object_capability(request.user, owner, "edit")
            )
        if not has_object_capability(request.user, asset, "edit") and not attachment_permitted:
            return JsonResponse({"error": "Edit permission is required."}, status=403)
        cropped = crop_asset(
            asset=asset,
            user=request.user,
            x=data.get("x"),
            y=data.get("y"),
            width=data.get("width"),
            height=data.get("height"),
        )
        if attachment_target is not None:
            owner, project, workspace = attachment_target
            _apply_asset_attachment(
                user=request.user,
                scope=attach_scope,
                owner=owner,
                project=project,
                workspace=workspace,
                asset=cropped,
            )
    except (json.JSONDecodeError, ValidationError) as exc:
        message = "; ".join(exc.messages) if isinstance(exc, ValidationError) else "Invalid crop request."
        return JsonResponse({"error": message}, status=400)
    return JsonResponse({
        "id": str(cropped.id),
        "viewUrl": reverse("studio_api:asset_view", kwargs={"asset_id": cropped.id}),
        "thumbnailUrl": reverse("studio_api:asset_thumbnail", kwargs={"asset_id": cropped.id}),
        "attached": bool(attachment_target),
        "attachScope": attach_scope,
        "attachOwnerId": str(attach_owner_id or ""),
    }, status=201)


@login_required
@transaction.atomic
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
        for scene in Scene.objects.filter(episode__project=project, reference_assets=asset):
            scene.reference_assets.remove(asset)
        for character in Character.objects.filter(project=project, reference_assets=asset):
            character.reference_assets.remove(asset)
        for prompt in Prompt.objects.filter(scene__episode__project=project, reference_assets=asset):
            prompt.reference_assets.remove(asset)
        for episode in Episode.objects.filter(project=project, cover_assets=asset):
            episode.cover_assets.remove(asset)
            EpisodeCover.objects.filter(episode=episode, asset=asset).delete()
            if episode.avatar_asset_id == asset.id:
                episode.avatar_asset = None
                episode.updated_by = request.user
                episode.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
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
        if asset.kind != Asset.Kind.GENERATION_OUTPUT:
            asset.kind = Asset.Kind.OTHER
        detached = True
    elif scope == "episode":
        episode = get_object_or_404(Episode.objects.filter(project__in=accessible_projects(request.user)), id=owner_id)
        if not has_object_capability(request.user, episode, "edit"):
            return HttpResponseForbidden("Edit permission is required.")
        episode.cover_assets.remove(asset)
        EpisodeCover.objects.filter(episode=episode, asset=asset).delete()
        if episode.avatar_asset_id == asset.id:
            episode.avatar_asset = None
            episode.updated_by = request.user
            episode.save(update_fields=["avatar_asset", "updated_by", "updated_at"])
        detached = True
    if not detached:
        return HttpResponseForbidden("This image is not attached here.")
    asset.updated_by = request.user
    asset.save(update_fields=["project", "scene", "prompt", "character", "kind", "updated_by", "updated_at"])
    audit(workspace=asset.workspace, actor=request.user, action="ASSET_DETACHED", instance=asset, metadata={"scope": scope, "ownerId": str(owner_id)})
    messages.success(request, "Image detached. It remains available in the Workspace library.")
    return _asset_action_redirect(request, asset)


@login_required
def episode_cover_update(request, cover_id):
    cover = get_object_or_404(
        EpisodeCover.objects.select_related("episode__project__workspace").filter(
            episode__project__in=accessible_projects(request.user)
        ),
        id=cover_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, cover.episode, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    language_code = request.POST.get("language_code", "").strip().upper()
    platform = request.POST.get("platform", "").strip().upper()
    custom_platform = request.POST.get("custom_platform", "").strip()
    if language_code not in dict(PROMPT_LANGUAGES):
        return JsonResponse({"error": "Choose a supported cover language."}, status=400)
    if platform not in EpisodeCover.Platform.values:
        return JsonResponse({"error": "Choose a supported cover platform."}, status=400)
    if platform == EpisodeCover.Platform.OTHER and not custom_platform:
        return JsonResponse({"error": "Enter the custom platform name."}, status=400)
    cover.language_code = language_code
    cover.platform = platform
    cover.custom_platform = custom_platform if platform == EpisodeCover.Platform.OTHER else ""
    cover.updated_by = request.user
    cover.save(update_fields=["language_code", "platform", "custom_platform", "updated_by", "updated_at"])
    audit(
        workspace=cover.episode.project.workspace,
        actor=request.user,
        action="EPISODE_COVER_UPDATED",
        instance=cover,
        metadata={"languageCode": language_code, "platform": cover.custom_platform or platform},
    )
    if request.headers.get("X-Requested-With") == "XMLHttpRequest":
        return JsonResponse({"ok": True, "languageCode": language_code, "platform": cover.custom_platform or cover.get_platform_display()})
    messages.success(request, "Episode cover saved.")
    return HttpResponseRedirect(reverse("studio:project_detail", kwargs={"project_id": cover.episode.project_id}) + f"#episode-{cover.episode_id}")


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
        messages.success(request, "File restored.")
    return _asset_action_redirect(request, asset)


@login_required
@transaction.atomic
def asset_purge(request, asset_id):
    asset = _asset_for_edit(request, asset_id, include_deleted=True)
    if request.method != "POST" or not has_object_capability(request.user, asset, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    if not _asset_original_exists(asset):
        _detach_asset_display_links(asset, request.user)
        purge_asset(asset=asset, user=request.user)
        messages.warning(request, "The missing file record was removed from Recycle Bin")
        return redirect("studio:workspace_recycle_bin", workspace_id=asset.workspace_id)
    try:
        _detach_asset_display_links(asset, request.user)
        purge_asset(asset=asset, user=request.user)
    except ValidationError as exc:
        messages.error(request, "; ".join(exc.messages))
    else:
        messages.success(request, "File permanently deleted")
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
    candidates = list(Asset.all_objects.filter(
        workspace=workspace, deleted_at__isnull=False, purged_at__isnull=True,
    ).select_related("project", "scene", "character").order_by("-deleted_at"))
    can_edit = has_capability(request.user, workspace, "edit")
    assets = []
    for asset in candidates:
        if _asset_original_exists(asset):
            assets.append(asset)
        elif can_edit:
            _detach_asset_display_links(asset, request.user)
            purge_asset(asset=asset, user=request.user)
    return render(request, "studio/workspace_recycle_bin.html", {
        "workspace": workspace, "assets": assets,
        "can_edit": can_edit,
    })


@login_required
@transaction.atomic
def workspace_recycle_bin_clear(request, workspace_id):
    workspace = get_object_or_404(accessible_workspaces(request.user), id=workspace_id)
    if request.method != "POST" or not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    assets = list(
        Asset.all_objects.select_for_update().filter(
            workspace=workspace,
            deleted_at__isnull=False,
            purged_at__isnull=True,
        )
    )
    for asset in assets:
        _detach_asset_display_links(asset, request.user)
        purge_asset(asset=asset, user=request.user)
    messages.success(
        request,
        f"Recycle Bin cleared: {len(assets)} file{'s' if len(assets) != 1 else ''} permanently deleted",
    )
    return redirect("studio:workspace_recycle_bin", workspace_id=workspace.id)


@login_required
def localized_translate(request, project_id):
    project = get_object_or_404(accessible_projects(request.user), id=project_id)
    if request.method != "POST" or not has_project_capability(request.user, project, "use_ai") or not user_has_ai_access(request.user):
        return JsonResponse({"error": "AI access is not enabled for this account."}, status=403)
    try:
        data = json.loads(request.body or b"{}")
        source = str(data.get("source", "")).strip()
        source_language = str(data.get("sourceLanguage", "")).strip().upper()
        action = str(data.get("action", "translate")).strip().lower()
        targets = {
            str(role): str(language).strip().upper()
            for role, language in dict(data.get("targets") or {}).items()
            if str(role) in {"documentation", "prompt", "dialogue"}
        }
        if not source:
            raise ValidationError("Enter source text first.")
        if source_language not in PROMPT_LANGUAGE_NAMES or (action != "improve" and not targets):
            raise ValidationError("Choose supported source and target languages.")
        if any(language not in PROMPT_LANGUAGE_NAMES for language in targets.values()):
            raise ValidationError("Choose supported target languages.")
        model = selected_text_model(data.get("textModel") or project_text_model_id(project))
        if action == "improve":
            instruction = json.dumps({
                "task": "Improve the supplied production phrase in the same language.",
                "language": PROMPT_LANGUAGE_NAMES[source_language],
                "content": source,
                "rules": [
                    "Correct grammar, spelling, punctuation and awkward phrasing.",
                    "Make the phrase polished and literary without changing its meaning.",
                    "Return JSON only with shape {\"improved\": \"...\"}.",
                    "Do not translate or add explanations.",
                ],
            }, ensure_ascii=False)
            raw, used_model = _run_logged_text(
                workspace=project.workspace, user=request.user, action="IMPROVE_LOCALIZED_TEXT",
                model=model, text=instruction,
            )
            clean = raw.strip()
            if clean.startswith("```"):
                clean = clean.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
            improved = str(json.loads(clean).get("improved", "")).strip()
            if not improved:
                raise ValidationError("AI returned an empty improved phrase.")
            return JsonResponse({"improved": improved, "model": used_model})
        response_shape = {role: "translated text" for role in targets}
        instruction = json.dumps({
            "task": "Translate one production text into each requested language.",
            "source_language": PROMPT_LANGUAGE_NAMES[source_language],
            "targets": {role: PROMPT_LANGUAGE_NAMES[language] for role, language in targets.items()},
            "content": source,
            "rules": [
                f"Return JSON only with shape {json.dumps(response_shape)}.",
                "Preserve names, meaning, formatting, tone and production terminology.",
                "Do not add explanations or information absent from the source.",
            ],
        }, ensure_ascii=False)
        raw, used_model = _run_logged_text(
            workspace=project.workspace, user=request.user, action="TRANSLATE_LOCALIZED_TEXT",
            model=model, text=instruction,
        )
        clean = raw.strip()
        if clean.startswith("```"):
            clean = clean.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
        parsed = json.loads(clean)
        translations = {role: str(parsed.get(role, "")).strip() for role in targets}
        if any(not value for value in translations.values()):
            raise ValidationError("AI returned an incomplete translation.")
    except (json.JSONDecodeError, ProviderError, ValidationError, TypeError, ValueError) as exc:
        message = "; ".join(exc.messages) if isinstance(exc, ValidationError) else str(exc)
        return JsonResponse({"error": message}, status=400)
    except Exception:
        return JsonResponse({"error": "The translation service returned an unexpected error."}, status=500)
    return JsonResponse({"translations": translations, "model": used_model})


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
        model = selected_text_model(data.get("textModel") or project_text_model_id(project))
        instruction = json.dumps({
            "task": f"Translate this dialogue into {target}.",
            "rules": ["Return JSON only with shape {\"content\": \"...\"}.", "Preserve meaning, names, tone, punctuation and speaker intent.", "Do not add explanations."],
            "source_language": (line.language or project.original_language or "").upper(),
            "content": line.text,
        }, ensure_ascii=False)
        raw, used_model = _run_logged_text(
            workspace=project.workspace, user=request.user, action="TRANSLATE_DIALOGUE_PREVIEW",
            model=model, text=instruction,
        )
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


def _project_image_assets(project):
    return list(
        Asset.objects.filter(
            workspace=project.workspace,
            content_type__startswith="image/",
            purged_at__isnull=True,
        ).filter(
            Q(project=project)
            | Q(scene__episode__project=project)
            | Q(character__project=project)
            | Q(prompt__scene__episode__project=project)
            | Q(projects=project)
            | Q(referenced_by_scenes__episode__project=project)
            | Q(referenced_by_characters__project=project)
            | Q(referenced_by_prompts__scene__episode__project=project)
            | Q(project_cover_for=project)
            | Q(character_avatar_for__project=project)
            | Q(cover_for_episodes__project=project)
            | Q(episode_avatar_for__project=project)
            | Q(episode_cover_entries__episode__project=project)
        ).distinct()
    )


def _copy_project_images(*, source, copied, target_workspace, user):
    source_assets = _project_image_assets(source)
    if target_workspace.id == source.workspace_id:
        copied.media_assets.add(*source_assets)
        return {asset.id: asset for asset in source_assets}

    asset_map = {}
    for source_asset in source_assets:
        target_asset = Asset.objects.filter(
            workspace=target_workspace,
            checksum_sha256=source_asset.checksum_sha256,
            content_type=source_asset.content_type,
            purged_at__isnull=True,
        ).exclude(file="").first()
        if target_asset is None:
            if not source_asset.file.name:
                raise ValidationError(f"{source_asset.original_filename} has no stored file to copy.")
            with source_asset.file.open("rb") as stored_file:
                uploaded = SimpleUploadedFile(
                    source_asset.original_filename,
                    stored_file.read(),
                    content_type=source_asset.content_type,
                )
            target_asset = create_asset(
                user=user,
                workspace=target_workspace,
                uploaded=uploaded,
                kind=source_asset.kind,
                project=copied,
            )
            target_asset.ai_metadata = dict(source_asset.ai_metadata or {})
            target_asset.save(update_fields=["ai_metadata", "updated_at"])
        else:
            target_asset.projects.add(copied)
        asset_map[source_asset.id] = target_asset
    return asset_map


def _mapped_assets(asset_map, assets):
    return [asset_map[asset.id] for asset in assets if asset.id in asset_map]


@login_required
@transaction.atomic
def project_copy(request, project_id):
    source = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    if request.method != "POST" or not has_project_capability(request.user, source, "manage_project"):
        return HttpResponseForbidden("Project management permission is required.")
    target_workspace = get_object_or_404(
        accessible_workspaces(request.user),
        id=request.POST.get("target_workspace") or source.workspace_id,
    )
    if not has_capability(request.user, target_workspace, "manage_project"):
        return HttpResponseForbidden("Target Workspace project management permission is required.")
    copied = Project.objects.create(
        workspace=target_workspace, project_type=source.project_type, title=_unique_project_title(target_workspace, source.title),
        description=source.description, concept=source.concept, original_language=source.original_language, translation_languages=source.translation_languages,
        prompt_template=source.prompt_template, documentation_language=source.documentation_language,
        dialogue_language=source.dialogue_language, prompt_language=source.prompt_language,
        default_translation_model=source.default_translation_model,
        hidden_sections=list(source.hidden_sections or []),
        rights_holder=source.rights_holder, publication_info=source.publication_info, status=Project.Status.DRAFT,
        status_comment=source.status_comment,
        created_by=request.user, updated_by=request.user,
    )
    asset_map = _copy_project_images(
        source=source,
        copied=copied,
        target_workspace=target_workspace,
        user=request.user,
    )
    for track in source.recommended_tracks.all():
        RecommendedTrack.objects.create(
            project=copied, is_primary=track.is_primary, platform=track.platform,
            artist=track.artist, title=track.title, url=track.url, position=track.position,
            created_by=request.user, updated_by=request.user,
        )
    character_map = {}
    for character in source.characters.all():
        character_map[character.id] = Character.objects.create(
            project=copied, name=character.name, name_prompt=character.name_prompt, name_dialogue=character.name_dialogue,
            description=character.description, description_prompt=character.description_prompt,
            description_dialogue=character.description_dialogue, visual_description=character.visual_description,
            visual_description_prompt=character.visual_description_prompt,
            visual_description_dialogue=character.visual_description_dialogue, position=character.position,
            created_by=request.user, updated_by=request.user,
        )
    episode_map, scene_map, prompt_map = {}, {}, {}
    for episode in source.episodes.prefetch_related("scenes__dialogue_lines", "scenes__prompts__blocks").all():
        new_episode = Episode.objects.create(project=copied, number=episode.number, title=episode.title, summary=episode.summary, position=episode.position, language=episode.language, created_by=request.user, updated_by=request.user)
        episode_map[episode.id] = new_episode
        for scene in episode.scenes.all():
            new_scene = Scene.objects.create(
                episode=new_episode, number=scene.number, title=scene.title, title_prompt=scene.title_prompt,
                title_dialogue=scene.title_dialogue, hook=scene.hook, hook_prompt=scene.hook_prompt,
                hook_dialogue=scene.hook_dialogue, description=scene.description,
                description_prompt=scene.description_prompt, description_dialogue=scene.description_dialogue,
                location=scene.location, location_prompt=scene.location_prompt, location_dialogue=scene.location_dialogue,
                actions=scene.actions, actions_prompt=scene.actions_prompt, actions_dialogue=scene.actions_dialogue,
                performance_notes=scene.performance_notes, performance_notes_prompt=scene.performance_notes_prompt,
                performance_notes_dialogue=scene.performance_notes_dialogue, scene_type=scene.scene_type,
                status=scene.status, status_comment=scene.status_comment, position=scene.position,
                created_by=request.user, updated_by=request.user,
            )
            scene_map[scene.id] = new_scene
            line_map = {}
            for line in scene.dialogue_lines.all():
                new_line = DialogueLine.objects.create(
                    scene=new_scene, speaker=line.speaker, speaker_documentation=line.speaker_documentation,
                    speaker_prompt=line.speaker_prompt, text=line.text, text_documentation=line.text_documentation,
                    text_prompt=line.text_prompt, language=line.language, delivery=line.delivery,
                    delivery_documentation=line.delivery_documentation, delivery_prompt=line.delivery_prompt,
                    position=line.position, status=line.status, status_comment=line.status_comment,
                    created_by=request.user, updated_by=request.user,
                )
                line_map[line.id] = new_line
                for translated in line.translations.all():
                    TranslationUnit.objects.create(dialogue_line=new_line, source_text=translated.source_text, translated_text=translated.translated_text, target_language=translated.target_language, status=translated.status, created_by=request.user, updated_by=request.user)
            ordered_prompts = list(scene.prompts.select_related("source_prompt").prefetch_related("blocks").all())
            for prompt in sorted(ordered_prompts, key=lambda item: bool(item.source_prompt_id)):
                new_prompt = Prompt.objects.create(scene=new_scene, ai_model=prompt.ai_model, template=prompt.template, source_prompt=prompt_map.get(prompt.source_prompt_id), original_language=prompt.original_language, language=prompt.language, translation_scope=prompt.translation_scope, content=prompt.content, prompt_type=prompt.prompt_type, title=prompt.title, status=prompt.status, status_comment=prompt.status_comment, position=prompt.position, needs_review=prompt.needs_review, created_by=request.user, updated_by=request.user)
                prompt_map[prompt.id] = new_prompt
                for block in prompt.blocks.all():
                    PromptBlock.objects.create(prompt=new_prompt, block_type=block.block_type, content=block.content, source_dialogue=line_map.get(block.source_dialogue_id), translated_content=block.translated_content, translation_language=block.translation_language, translation_model=block.translation_model, position=block.position, created_by=request.user, updated_by=request.user)
    for old_scene_id, new_scene in scene_map.items():
        old_scene = Scene.all_objects.get(id=old_scene_id)
        references = _mapped_assets(asset_map, old_scene.reference_assets.all())
        if references:
            new_scene.reference_assets.add(*references)
            copied.media_assets.add(*references)
    for old_prompt_id, new_prompt in prompt_map.items():
        references = _mapped_assets(
            asset_map,
            Prompt.all_objects.get(id=old_prompt_id).reference_assets.all(),
        )
        if references:
            new_prompt.reference_assets.add(*references)
            copied.media_assets.add(*references)
    for old_character_id, new_character in character_map.items():
        old_character = Character.all_objects.get(id=old_character_id)
        references = _mapped_assets(asset_map, old_character.reference_assets.all())
        if references:
            new_character.reference_assets.add(*references)
            copied.media_assets.add(*references)
        old_avatar_id = old_character.avatar_asset_id
        if old_avatar_id in asset_map:
            new_character.avatar_asset = asset_map[old_avatar_id]
            new_character.save(update_fields=["avatar_asset", "updated_at"])
    for old_episode_id, new_episode in episode_map.items():
        old_episode = Episode.all_objects.get(id=old_episode_id)
        covers = _mapped_assets(asset_map, old_episode.cover_assets.all())
        if covers:
            new_episode.cover_assets.add(*covers)
            copied.media_assets.add(*covers)
        if old_episode.avatar_asset_id in asset_map:
            new_episode.avatar_asset = asset_map[old_episode.avatar_asset_id]
            new_episode.save(update_fields=["avatar_asset", "updated_at"])
        EpisodeCover.objects.bulk_create([
            EpisodeCover(
                episode=new_episode,
                asset=asset_map[cover.asset_id],
                language_code=cover.language_code,
                platform=cover.platform,
                custom_platform=cover.custom_platform,
                created_by=request.user,
                updated_by=request.user,
            )
            for cover in old_episode.cover_entries.all()
            if cover.asset_id in asset_map
        ], ignore_conflicts=True)
    if source.cover_asset_id in asset_map:
        copied.cover_asset = asset_map[source.cover_asset_id]
        copied.media_assets.add(copied.cover_asset)
        copied.save(update_fields=["cover_asset", "updated_at"])
    if request.POST.get("copy_access") == "1":
        memberships = source.memberships.filter(is_active=True).select_related("user")
        if target_workspace.id != source.workspace_id:
            allowed_user_ids = set(
                target_workspace.memberships.filter(
                    status=WorkspaceMembership.Status.ACTIVE,
                ).values_list("user_id", flat=True)
            )
            allowed_user_ids.add(target_workspace.owner_id)
            memberships = memberships.filter(user_id__in=allowed_user_ids)
        ProjectMembership.objects.bulk_create([
            ProjectMembership(project=copied, user=item.user, role=item.role, is_active=item.is_active, invited_by=request.user)
            for item in memberships
        ])
    audit(
        workspace=target_workspace,
        actor=request.user,
        action="PROJECT_COPIED",
        instance=copied,
        metadata={
            "sourceProjectId": str(source.id),
            "sourceWorkspaceId": str(source.workspace_id),
            "targetWorkspaceId": str(target_workspace.id),
            "copiedImageCount": len(asset_map),
        },
    )
    if target_workspace.id == source.workspace_id:
        messages.success(request, "Project copied with shared Workspace images")
    else:
        messages.success(request, f"Project copied to {target_workspace.name} with its images")
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
            deleted_at__isnull=False,
            purged_at__isnull=True,
        ),
        id=asset_id,
    )
    field = asset.file
    if thumbnail and asset.thumbnail.name:
        try:
            if asset.thumbnail.storage.exists(asset.thumbnail.name):
                field = asset.thumbnail
        except OSError:
            pass
    if not field.name:
        raise Http404("File not found.")
    try:
        response = FileResponse(field.open("rb"), content_type="image/jpeg" if field == asset.thumbnail else asset.content_type)
    except (FileNotFoundError, OSError) as exc:
        raise Http404("File not found.") from exc
    audit(workspace=asset.workspace, actor=request.user, action="ASSET_TRASH_VIEW", instance=asset)
    return response

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

def _prompt_editor_context(request, project=None):
    return {
        "ai_models": AiModelProfile.objects.filter(is_active=True),
        "media_models": AiModelProfile.objects.filter(
            is_active=True,
            media_type__in=[AiModelProfile.MediaType.IMAGE, AiModelProfile.MediaType.VIDEO],
        ),
        "image_models": AiModelProfile.objects.filter(
            is_active=True,
            media_type=AiModelProfile.MediaType.IMAGE,
            provider__iexact="openai",
        ),
        "text_models": active_text_models(),
        "default_text_model": project_text_model_id(project) if project else default_text_model_id(),
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
        "statusComment": prompt.status_comment,
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
    expected_media_type = {
        Prompt.Type.IMAGE: AiModelProfile.MediaType.IMAGE,
        Prompt.Type.VIDEO: AiModelProfile.MediaType.VIDEO,
    }.get(prompt_type)
    if expected_media_type and ai_model.media_type != expected_media_type:
        messages.error(request, "Choose a model that matches the prompt type.")
        return redirect("studio:scene_detail", scene_id=scene.id)
    content = request.POST.get("content", "").strip()
    prompt = Prompt.objects.create(
        scene=scene,
        ai_model=ai_model,
        template=default_prompt_template(prompt_type),
        prompt_type=prompt_type,
        title=request.POST.get("title", "").strip(),
        status=status,
        status_comment=request.POST.get("status_comment", "").strip(),
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
    reference_ids = request.POST.getlist("reference_asset_ids")[:3]
    if reference_ids:
        references = list(
            _accessible_workspace_images(request.user, scene.episode.project.workspace)
            .filter(id__in=reference_ids)
        )
        prompt.reference_assets.add(*references)
        scene.episode.project.media_assets.add(*references)
    audit(workspace=workspace, actor=request.user, action="PROMPT_INLINE_CREATED", instance=prompt)
    messages.success(request, "Prompt created.")
    return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": scene.id}) + f"#prompt-{prompt.id}")


@login_required
def prompt_delete(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__in=accessible_projects(request.user)
        ),
        id=prompt_id,
    )
    if request.method != "POST" or not has_object_capability(request.user, prompt, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    now = timezone.now()
    Prompt.objects.filter(Q(id=prompt.id) | Q(source_prompt_id=prompt.id)).update(
        deleted_at=now, deleted_by=request.user, updated_by=request.user,
    )
    audit(
        workspace=prompt.scene.episode.project.workspace,
        actor=request.user,
        action="PROMPT_DELETED",
        instance=prompt,
    )
    messages.success(request, "Prompt deleted")
    return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": prompt.scene_id}) + "#prompts")


@login_required
def project_remove_avatar(request, project_id):
    project = get_object_or_404(accessible_projects(request.user), id=project_id)
    if request.method != "POST" or not has_project_capability(request.user, project, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    project.cover_asset = None
    project.updated_by = request.user
    project.save(update_fields=["cover_asset", "updated_by", "updated_at"])
    audit(workspace=project.workspace, actor=request.user, action="PROJECT_COVER_REMOVED", instance=project)
    if request.headers.get("X-Requested-With") == "XMLHttpRequest":
        return JsonResponse({"ok": True})
    messages.success(request, "Project avatar removed")
    return redirect("studio:project_detail", project_id=project.id)


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
        prompt.status_comment = request.POST.get("status_comment", "").strip()
        if not prompt.source_prompt_id:
            prompt.original_language = requested_original_language
            prompt.language = requested_original_language
        content = request.POST.get("content", prompt.editor_content)
        prompt.updated_by = request.user
        prompt.full_clean()
        prompt.save(update_fields=["ai_model", "template", "prompt_type", "title", "status", "status_comment", "original_language", "language", "updated_by", "updated_at"])
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
def prompt_generate_image(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related(
            "ai_model", "scene__episode__project__workspace__default_image_model",
        ).prefetch_related("reference_assets").filter(
            scene__episode__project__in=accessible_projects(request.user)
        ),
        id=prompt_id,
    )
    project = prompt.scene.episode.project
    if request.method != "POST" or prompt.prompt_type != Prompt.Type.IMAGE:
        return JsonResponse({"error": "Image generation is available only for Photo prompts."}, status=400)
    if not has_object_capability(request.user, prompt, "use_ai") or not user_has_ai_access(request.user):
        return JsonResponse({"error": "AI access is not enabled for this account."}, status=403)
    try:
        payload = json.loads(request.body or b"{}")
    except (TypeError, ValueError):
        return JsonResponse({"error": "The generation request is not valid JSON."}, status=400)
    requested_profile = None
    if payload.get("modelProfileId"):
        requested_profile = AiModelProfile.objects.filter(
            id=payload["modelProfileId"],
            media_type=AiModelProfile.MediaType.IMAGE, is_active=True,
            provider__iexact="openai",
        ).first()
    image_model = requested_profile or (prompt.ai_model if (
        prompt.ai_model.media_type == AiModelProfile.MediaType.IMAGE
        and prompt.ai_model.provider.strip().lower() == "openai"
    ) else project.workspace.default_image_model)
    if image_model is None:
        return JsonResponse({"error": "Choose a default OpenAI image model in Workspace settings."}, status=400)
    defaults = image_model.defaults if isinstance(image_model.defaults, dict) else {}
    request_prompt = str(payload.get("prompt") or prompt.editor_content).strip()
    if not request_prompt:
        return JsonResponse({"error": "Copy a non-empty prompt into the generation request."}, status=400)
    if len(request_prompt) > settings.AI_MAX_TEXT_CHARS:
        return JsonResponse({"error": "The image prompt exceeds the server limit."}, status=400)
    try:
        output_compression = int(payload.get("outputCompression", defaults.get("output_compression", 100)))
    except (TypeError, ValueError):
        return JsonResponse({"error": "Image compression must be a number from 0 to 100."}, status=400)
    image_options = {
        "size": str(payload.get("size") or defaults.get("size") or "1024x1024"),
        "composition_preset": str(payload.get("compositionPreset") or "square"),
        "quality": str(payload.get("quality") or defaults.get("quality") or "low"),
        "output_format": str(payload.get("outputFormat") or defaults.get("output_format") or "png"),
        "output_compression": output_compression,
        "background": str(payload.get("background") or defaults.get("background") or "auto"),
        "moderation": str(payload.get("moderation") or defaults.get("moderation") or "auto"),
    }
    reference_assets = list(
        prompt.reference_assets.filter(content_type__startswith="image/")[:3]
    )
    job = ImageGenerationJob.objects.create(
        workspace=project.workspace,
        project=project,
        prompt=prompt,
        requested_by=request.user,
        model_profile=image_model,
        request_prompt=request_prompt,
        options=image_options,
        reference_asset_ids=[str(item.id) for item in reference_assets],
    )
    audit(
        workspace=project.workspace, actor=request.user, action="PROMPT_IMAGE_QUEUED",
        instance=prompt, metadata={
            "jobId": str(job.id), "model": image_model.model_id,
            "requestPrompt": request_prompt, "settings": image_options,
            "referenceAssetIds": [str(item.id) for item in reference_assets],
        },
    )
    return JsonResponse(_image_generation_job_payload(job), status=202)


@login_required
def prompt_image_jobs(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.filter(scene__episode__project__in=accessible_projects(request.user)),
        id=prompt_id,
    )
    jobs = ImageGenerationJob.objects.select_related("result_asset", "model_profile").filter(
        prompt=prompt,
        requested_by=request.user,
    )[:10]
    return JsonResponse({"jobs": [_image_generation_job_payload(job) for job in jobs]})


@login_required
def image_generation_job_status(request, job_id):
    job = get_object_or_404(
        ImageGenerationJob.objects.select_related("result_asset", "model_profile", "requested_by").filter(
            Q(project__in=accessible_projects(request.user))
            | Q(prompt__scene__episode__project__in=accessible_projects(request.user)),
        ).distinct(),
        id=job_id,
    )
    return JsonResponse(_image_generation_job_payload(job))


def _image_generation_job_payload(job):
    reference_map = {
        str(asset.id): asset
        for asset in Asset.objects.filter(id__in=job.reference_asset_ids)
    }
    reference_images = []
    for asset_id in job.reference_asset_ids:
        asset = reference_map.get(str(asset_id))
        if asset is None:
            continue
        reference_images.append({
            "assetId": str(asset.id),
            "name": asset.original_filename,
            "viewUrl": reverse("studio_api:asset_view", kwargs={"asset_id": asset.id}),
            "thumbnailUrl": reverse("studio_api:asset_thumbnail", kwargs={"asset_id": asset.id}),
        })
    payload = {
        "jobId": str(job.id),
        "status": job.status,
        "model": job.provider_model or job.model_profile.model_id,
        "referenceCount": len(job.reference_asset_ids),
        "settings": job.options,
        "error": job.error_message,
        "createdAt": job.created_at.isoformat(),
        "requestPrompt": job.request_prompt,
        "requestedBy": job.requested_by.get_full_name() or job.requested_by.username,
        "referenceImages": reference_images,
        "startedAt": job.started_at.isoformat() if job.started_at else None,
        "finishedAt": job.finished_at.isoformat() if job.finished_at else None,
        "statusUrl": reverse("studio:image_generation_job_status", kwargs={"job_id": job.id}),
    }
    if job.result_asset_id:
        payload.update({
            "assetId": str(job.result_asset_id),
            "viewUrl": reverse("studio_api:asset_view", kwargs={"asset_id": job.result_asset_id}),
            "thumbnailUrl": reverse("studio_api:asset_thumbnail", kwargs={"asset_id": job.result_asset_id}),
            "starred": job.result_asset.is_starred,
            "starUrl": reverse("studio:asset_star_toggle", kwargs={"asset_id": job.result_asset_id}),
            "detachUrl": reverse("studio:asset_detach", kwargs={
                "asset_id": job.result_asset_id,
                "scope": "project",
                "owner_id": job.project_id or job.prompt.scene.episode.project_id,
            }),
        })
    return payload


@login_required
def user_token_usage(request):
    logs = AiUsageLog.objects.filter(user=request.user).select_related("workspace")
    for key, field in (("workspace", "workspace_id"), ("model", "model"), ("action", "action")):
        value = request.GET.get(key, "").strip()
        if value:
            logs = logs.filter(**{field: value})
    status = request.GET.get("status", "").strip().upper()
    if status:
        logs = logs.filter(status=status)
    try:
        per_page = int(request.GET.get("per_page", 50))
    except (TypeError, ValueError):
        per_page = 50
    if per_page not in {20, 50, 100, 500}:
        per_page = 50
    page = Paginator(logs, per_page).get_page(request.GET.get("page"))
    return JsonResponse({
        "summary": token_summary(request.user), "page": page.number,
        "pages": page.paginator.num_pages, "count": page.paginator.count,
        "operations": [{
            "time": item.created_at.isoformat(), "workspace": item.workspace.name,
            "action": item.action, "model": item.model, "tokens": item.total_tokens,
            "status": item.status,
        } for item in page],
    })


@login_required
def project_assistant(request, project_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    if request.method != "POST" or not has_project_capability(request.user, project, "use_ai"):
        return JsonResponse({"error": "AI access is required"}, status=403)
    try:
        payload = json.loads(request.body or b"{}")
        question = str(payload.get("message") or "").strip()
        model = selected_text_model(payload.get("model") or project_text_model_id(project))
    except (ValueError, ValidationError) as exc:
        return JsonResponse({"error": str(exc)}, status=400)
    if not question:
        return JsonResponse({"error": "Enter a message"}, status=400)
    context = ProjectAssistantContext.objects.filter(project=project).first()
    history = payload.get("history") if isinstance(payload.get("history"), list) else []
    request_text = json.dumps({
        "role": "You are a practical film production assistant inside Lexamora Studio",
        "project": {"title": project.title, "description": project.description, "concept": project.concept},
        "saved_context": context.content if context else "", "history": history[-12:], "question": question,
    }, ensure_ascii=False)
    try:
        answer, used_model = _run_logged_text(workspace=project.workspace, user=request.user, action="PROJECT_ASSISTANT", model=model, text=request_text)
    except (ProviderError, TokenQuotaExceeded, ValidationError) as exc:
        return JsonResponse({"error": str(exc)}, status=400)
    return JsonResponse({"answer": answer, "model": used_model})


@login_required
def project_assistant_context(request, project_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    if not has_project_capability(request.user, project, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    context, _ = ProjectAssistantContext.objects.get_or_create(project=project)
    if request.method == "GET":
        return JsonResponse({"content": context.content, "downloadUrl": reverse("studio_api:asset_download", kwargs={"asset_id": context.asset_id}) if context.asset_id else ""})
    uploaded = request.FILES.get("file")
    incoming = request.POST.get("content", "")
    if uploaded:
        try:
            incoming = uploaded.read().decode("utf-8")
        except UnicodeDecodeError:
            return JsonResponse({"error": "Context file must be UTF-8 text"}, status=400)
    merge = request.POST.get("merge", "1") == "1"
    content = "\n\n".join(part for part in ([context.content, incoming] if merge else [incoming]) if part.strip()).strip()
    uploaded_context = SimpleUploadedFile(f"{slugify(project.title)}-assistant-context.txt", content.encode("utf-8"), content_type="text/plain")
    asset = create_asset(user=request.user, workspace=project.workspace, uploaded=uploaded_context, kind=Asset.Kind.OTHER, project=project)
    if context.asset_id:
        context.asset.projects.remove(project)
    context.content = content; context.asset = asset; context.updated_by = request.user
    context.save()
    return JsonResponse({"ok": True, "content": content, "downloadUrl": reverse("studio_api:asset_download", kwargs={"asset_id": asset.id})})


@login_required
def project_image_generation(request, project_id):
    project = get_object_or_404(
        accessible_projects(request.user).select_related("workspace__default_image_model", "cover_asset"),
        id=project_id,
    )
    if not has_project_capability(request.user, project, "use_ai") or not user_has_ai_access(request.user):
        return HttpResponseForbidden("AI access is not enabled for this account.")
    project_assets, workspace_assets = _picker_assets(request.user, project)
    jobs = _project_generation_jobs(project)[:100]
    return render(request, "studio/project_image_generation.html", {
        "project": project,
        "project_assets": project_assets,
        "workspace_assets": workspace_assets,
        "media_models": AiModelProfile.objects.filter(
            is_active=True,
            media_type__in=[AiModelProfile.MediaType.IMAGE, AiModelProfile.MediaType.VIDEO],
        ),
        "text_models": active_text_models(),
        "default_text_model": project_text_model_id(project),
        "prompt_languages": PROMPT_LANGUAGES,
        "jobs": jobs,
        **_project_header_context(request.user, project),
    })


@login_required
def project_image_prompt_preview(request, project_id):
    project = get_object_or_404(accessible_projects(request.user).select_related("workspace"), id=project_id)
    if request.method != "POST":
        return JsonResponse({"error": "POST is required."}, status=405)
    if not has_project_capability(request.user, project, "use_ai") or not user_has_ai_access(request.user):
        return JsonResponse({"error": "AI access is not enabled for this account."}, status=403)
    try:
        payload = json.loads(request.body or b"{}")
    except (TypeError, ValueError):
        return JsonResponse({"error": "The prompt request is not valid JSON."}, status=400)
    source = str(payload.get("prompt") or "").strip()
    action = str(payload.get("action") or "").strip().lower()
    if not source:
        return JsonResponse({"error": "Enter an image prompt first."}, status=400)
    if action not in {"translate", "improve"}:
        return JsonResponse({"error": "Choose Translate or Improve."}, status=400)
    try:
        model_id = selected_text_model(payload.get("textModel") or project_text_model_id(project))
        if action == "translate":
            target = str(payload.get("targetLanguage") or "EN").strip()
            target_name = PROMPT_LANGUAGE_NAMES.get(target.upper(), target)
            if not target_name or len(target_name) > 60:
                raise ValidationError("Enter a valid target language.")
            instruction = (
                f"Translate this media-generation prompt into {target_name}. Preserve names, visual details, "
                "camera instructions, and formatting. If the prompt explicitly says that a character speaks in "
                "a named language, translate that direct speech into the named language, not the body language; "
                "keep the language instruction explicit. Return only the translated prompt."
            )
        else:
            target = ""
            instruction = (
                "Improve this image-generation prompt in its current language. Make it precise, visual, coherent, "
                "and production-ready without changing the requested subject. Return only the improved prompt."
            )
        result, used_model = _run_logged_text(
            workspace=project.workspace,
            user=request.user,
            action=f"PROJECT_IMAGE_PROMPT_{action.upper()}",
            model=model_id,
            text=f"{instruction}\n\nPROMPT:\n{source}",
        )
    except (ProviderError, TokenQuotaExceeded, ValidationError) as exc:
        return JsonResponse({"error": str(exc)}, status=400)
    except Exception:
        return JsonResponse({"error": "The AI service returned an unexpected error. Please retry."}, status=500)
    return JsonResponse({"content": result.strip(), "model": used_model, "language": target})


@login_required
def project_generate_image(request, project_id):
    project = get_object_or_404(
        accessible_projects(request.user).select_related("workspace__default_image_model"),
        id=project_id,
    )
    if request.method != "POST":
        return JsonResponse({"error": "POST is required."}, status=405)
    if not has_project_capability(request.user, project, "use_ai") or not user_has_ai_access(request.user):
        return JsonResponse({"error": "AI access is not enabled for this account."}, status=403)
    try:
        require_token_quota(request.user)
    except TokenQuotaExceeded as exc:
        return JsonResponse({"error": str(exc)}, status=402)
    try:
        payload = json.loads(request.body or b"{}")
    except (TypeError, ValueError):
        return JsonResponse({"error": "The generation request is not valid JSON."}, status=400)
    image_model = AiModelProfile.objects.filter(
        id=payload.get("modelProfileId"),
        media_type=AiModelProfile.MediaType.IMAGE,
        is_active=True,
        provider__iexact="openai",
    ).first() or project.workspace.default_image_model
    if image_model is None:
        return JsonResponse({"error": "Choose an OpenAI image model."}, status=400)
    request_prompt = str(payload.get("prompt") or "").strip()
    if not request_prompt or len(request_prompt) > settings.AI_MAX_TEXT_CHARS:
        return JsonResponse({"error": "Enter a prompt within the server text limit."}, status=400)
    defaults = image_model.defaults if isinstance(image_model.defaults, dict) else {}
    try:
        output_compression = int(payload.get("outputCompression", defaults.get("output_compression", 100)))
    except (TypeError, ValueError):
        return JsonResponse({"error": "Image compression must be a number from 0 to 100."}, status=400)
    reference_ids = [str(value) for value in payload.get("referenceAssetIds", [])][:3]
    accessible_ids = {
        str(value)
        for value in _accessible_workspace_images(request.user, project.workspace).filter(
            id__in=reference_ids,
        ).values_list("id", flat=True)
    }
    if len(accessible_ids) != len(set(reference_ids)):
        return JsonResponse({"error": "One or more reference images are not accessible."}, status=400)
    image_options = {
        "size": str(payload.get("size") or defaults.get("size") or "1024x1024"),
        "composition_preset": str(payload.get("compositionPreset") or "square"),
        "quality": str(payload.get("quality") or defaults.get("quality") or "low"),
        "output_format": str(payload.get("outputFormat") or defaults.get("output_format") or "png"),
        "output_compression": output_compression,
        "background": str(payload.get("background") or defaults.get("background") or "auto"),
        "moderation": str(payload.get("moderation") or defaults.get("moderation") or "auto"),
    }
    job = ImageGenerationJob.objects.create(
        workspace=project.workspace,
        project=project,
        requested_by=request.user,
        model_profile=image_model,
        request_prompt=request_prompt,
        options=image_options,
        reference_asset_ids=reference_ids,
    )
    audit(
        workspace=project.workspace,
        actor=request.user,
        action="PROJECT_IMAGE_QUEUED",
        instance=project,
        metadata={
            "jobId": str(job.id),
            "model": image_model.model_id,
            "referenceAssetIds": reference_ids,
            "settings": image_options,
        },
    )
    return JsonResponse(_image_generation_job_payload(job), status=202)


@login_required
def project_image_jobs(request, project_id):
    project = get_object_or_404(accessible_projects(request.user), id=project_id)
    jobs = _project_generation_jobs(project)[:100]
    return JsonResponse({"jobs": [_image_generation_job_payload(job) for job in jobs]})


def _project_generation_jobs(project):
    visible = (
        Q(status__in=[ImageGenerationJob.Status.QUEUED, ImageGenerationJob.Status.RUNNING, ImageGenerationJob.Status.ERROR])
        | Q(result_asset__projects=project)
    )
    return (
        ImageGenerationJob.objects
        .select_related("result_asset", "model_profile", "requested_by")
        .filter(project=project, prompt__isnull=True)
        .filter(visible)
        .distinct()
    )


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
        ).distinct(),
        id=request.POST.get("asset_id"),
    )
    if (
        not prompt.reference_assets.filter(id=asset.id).exists()
        and prompt.generation_reference_count >= 3
    ):
        messages.warning(request, "A prompt can use up to three reference images")
        return HttpResponseRedirect(
            reverse("studio:scene_detail", kwargs={"scene_id": prompt.scene_id})
            + f"#prompt-{prompt.id}"
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
        highest_number = max(
            Scene.all_objects.filter(episode=episode).values_list("number", flat=True),
            default=0,
        )
        temporary = highest_number + len(scenes) + 100
        for offset, scene_id in enumerate(ordered_ids):
            scene = by_id[scene_id]
            scene.position = temporary + offset
            scene.number = temporary + offset
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
