import json

from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.core.exceptions import ValidationError
from django.db import transaction
from django.db.models import Prefetch
from django.http import Http404, HttpResponseForbidden, HttpResponseRedirect, JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.urls import reverse
from django.utils.text import slugify

from lessons.ai_gateway import ProviderError
from lessons.provider_credentials import user_has_ai_access

from .ai import StudioAiError, accept_suggestion, improve_prompt, reject_suggestion
from .exports import ALL_SECTIONS, ExportError, generate_export
from .docx_imports import accept_docx_import, parse_docx
from .docx_exports import generate_docx_export
from .docx_roundtrip import compare_docx_export
from .forms import AdditionalGenerationForm, AssetEditForm, CharacterForm, DialogueLineForm, DocxImportUploadForm, EpisodeForm, GenerationOutputUploadForm, ImageUploadForm, ProjectForm, PromptBlockForm, PromptForm, SceneForm
from .models import AdditionalGeneration, AiModelProfile, AiSuggestion, Asset, Character, DialogueLine, DocxImport, Episode, ExportJob, GenerationOutput, Project, Prompt, PromptBlock, Scene, SubtitleTrack, TranslationUnit, Workspace
from .permissions import accessible_workspaces, has_capability
from .revisions import audit, record_revision
from .services import bulk_replace_subtitle_lines, create_workspace, reorder_subtitle_lines, save_translation
from .storage import create_asset


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
    return render(request, "studio/workspace_detail.html", {"workspace": workspace, "can_edit": has_capability(request.user, workspace, "edit"), "imports": workspace.docx_imports.select_related("project")[:10]})


def _create_entity(request, *, form_class, parent, parent_field, workspace, title, success_url, position_manager=None):
    if not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = form_class(request.POST or None)
    if request.method == "POST" and form.is_valid():
        item = form.save(commit=False)
        setattr(item, parent_field, parent)
        item.created_by = request.user
        item.updated_by = request.user
        if position_manager is not None:
            item.position = position_manager.count()
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
    return _create_entity(request, form_class=ProjectForm, parent=workspace, parent_field="workspace", workspace=workspace, title="New project", success_url=lambda item: ("studio:project_detail", item.id))


@login_required
def character_create(request, project_id):
    project = get_object_or_404(Project.objects.filter(workspace__in=accessible_workspaces(request.user)), id=project_id)
    return _create_entity(request, form_class=CharacterForm, parent=project, parent_field="project", workspace=project.workspace, title="New character", success_url=lambda item: ("studio:project_detail", item.project_id), position_manager=project.characters)


@login_required
def episode_create(request, project_id):
    project = get_object_or_404(Project.objects.filter(workspace__in=accessible_workspaces(request.user)), id=project_id)
    return _create_entity(request, form_class=EpisodeForm, parent=project, parent_field="project", workspace=project.workspace, title="New episode", success_url=lambda item: ("studio:project_detail", item.project_id), position_manager=project.episodes)


@login_required
def scene_create(request, episode_id):
    episode = get_object_or_404(Episode.objects.filter(project__workspace__in=accessible_workspaces(request.user)), id=episode_id)
    return _create_entity(request, form_class=SceneForm, parent=episode, parent_field="episode", workspace=episode.project.workspace, title="New scene", success_url=lambda item: ("studio:scene_detail", item.id), position_manager=episode.scenes)


@login_required
def dialogue_create(request, scene_id):
    scene = get_object_or_404(Scene.objects.filter(episode__project__workspace__in=accessible_workspaces(request.user)), id=scene_id)
    return _create_entity(request, form_class=DialogueLineForm, parent=scene, parent_field="scene", workspace=scene.episode.project.workspace, title="New dialogue line", success_url=lambda item: ("studio:scene_detail", item.scene_id), position_manager=scene.dialogue_lines)


@login_required
def prompt_create(request, scene_id):
    scene = get_object_or_404(Scene.objects.filter(episode__project__workspace__in=accessible_workspaces(request.user)), id=scene_id)
    return _create_entity(request, form_class=PromptForm, parent=scene, parent_field="scene", workspace=scene.episode.project.workspace, title="New prompt", success_url=lambda item: ("studio:prompt_detail", item.id), position_manager=scene.prompts)


@login_required
def prompt_block_create(request, prompt_id):
    prompt = get_object_or_404(Prompt.objects.filter(scene__episode__project__workspace__in=accessible_workspaces(request.user)), id=prompt_id)
    return _create_entity(request, form_class=PromptBlockForm, parent=prompt, parent_field="prompt", workspace=prompt.scene.episode.project.workspace, title="New prompt block", success_url=lambda item: ("studio:prompt_detail", item.prompt_id), position_manager=prompt.blocks)


def _edit_entity(request, *, item, form_class, workspace, title, success_url):
    if not has_capability(request.user, workspace, "edit"):
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
    if not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = ImageUploadForm(request.POST or None, request.FILES or None)
    if request.method == "POST" and form.is_valid():
        try:
            create_asset(user=request.user, workspace=workspace, uploaded=form.cleaned_data["file"], kind=kind, project=project, scene=scene, character=character)
        except ValidationError as exc:
            form.add_error("file", exc)
        else:
            messages.success(request, "Image uploaded.")
            return redirect(*success_url(target))
    return render(request, "studio/entity_form.html", {"form": form, "title": title, "submit_label": "Upload image", "multipart": True})


@login_required
def project_edit(request, project_id):
    item = get_object_or_404(Project.objects.filter(workspace__in=accessible_workspaces(request.user)), id=project_id)
    return _edit_entity(request, item=item, form_class=ProjectForm, workspace=item.workspace, title="Edit project", success_url=lambda value: ("studio:project_detail", value.id))


@login_required
def character_edit(request, character_id):
    item = get_object_or_404(Character.objects.select_related("project__workspace").filter(project__workspace__in=accessible_workspaces(request.user)), id=character_id)
    return _edit_entity(request, item=item, form_class=CharacterForm, workspace=item.project.workspace, title="Edit character", success_url=lambda value: ("studio:project_detail", value.project_id))


@login_required
def episode_edit(request, episode_id):
    item = get_object_or_404(Episode.objects.select_related("project__workspace").filter(project__workspace__in=accessible_workspaces(request.user)), id=episode_id)
    return _edit_entity(request, item=item, form_class=EpisodeForm, workspace=item.project.workspace, title="Edit episode", success_url=lambda value: ("studio:project_detail", value.project_id))


@login_required
def scene_edit(request, scene_id):
    item = get_object_or_404(Scene.objects.select_related("episode__project__workspace").filter(episode__project__workspace__in=accessible_workspaces(request.user)), id=scene_id)
    return _edit_entity(request, item=item, form_class=SceneForm, workspace=item.episode.project.workspace, title="Edit scene", success_url=lambda value: ("studio:scene_detail", value.id))


@login_required
def dialogue_edit(request, line_id):
    item = get_object_or_404(DialogueLine.objects.select_related("scene__episode__project__workspace").filter(scene__episode__project__workspace__in=accessible_workspaces(request.user)), id=line_id)
    workspace = item.scene.episode.project.workspace
    if not has_capability(request.user, workspace, "edit"):
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
    item = get_object_or_404(Prompt.objects.select_related("scene__episode__project__workspace").filter(scene__episode__project__workspace__in=accessible_workspaces(request.user)), id=prompt_id)
    return _edit_entity(request, item=item, form_class=PromptForm, workspace=item.scene.episode.project.workspace, title="Edit prompt", success_url=lambda value: ("studio:prompt_detail", value.id))


@login_required
def prompt_block_edit(request, block_id):
    item = get_object_or_404(PromptBlock.objects.select_related("prompt__scene__episode__project__workspace").filter(prompt__scene__episode__project__workspace__in=accessible_workspaces(request.user)), id=block_id)
    return _edit_entity(request, item=item, form_class=PromptBlockForm, workspace=item.prompt.scene.episode.project.workspace, title="Edit prompt block", success_url=lambda value: ("studio:prompt_detail", value.prompt_id))


@login_required
def project_image_upload(request, project_id):
    project = get_object_or_404(Project.objects.select_related("workspace").prefetch_related("assets", "characters__assets", "episodes__scenes").filter(workspace__in=accessible_workspaces(request.user)), id=project_id)
    return _image_upload(request, target=project, workspace=project.workspace, title="Upload project image", success_url=lambda value: ("studio:project_detail", value.id), kind=Asset.Kind.OTHER, project=project)


@login_required
def character_image_upload(request, character_id):
    character = get_object_or_404(Character.objects.select_related("project__workspace").filter(project__workspace__in=accessible_workspaces(request.user)), id=character_id)
    return _image_upload(request, target=character, workspace=character.project.workspace, title="Upload character reference", success_url=lambda value: ("studio:project_detail", value.project_id), kind=Asset.Kind.CHARACTER_REFERENCE, project=character.project, character=character)


@login_required
def scene_image_upload(request, scene_id):
    scene = get_object_or_404(Scene.objects.select_related("episode__project__workspace").filter(episode__project__workspace__in=accessible_workspaces(request.user)), id=scene_id)
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
        ).filter(workspace__in=accessible_workspaces(request.user)), id=project_id,
    )
    return render(request, "studio/project_master.html", {"project": project, "can_edit": has_capability(request.user, project.workspace, "edit")})


@login_required
def entity_move(request, entity_type, entity_id, direction):
    if request.method != "POST" or direction not in {"up", "down"}:
        return HttpResponseForbidden("POST with a valid direction is required.")
    if entity_type == "character":
        item = get_object_or_404(Character.objects.select_related("project__workspace").filter(project__workspace__in=accessible_workspaces(request.user)), id=entity_id)
        siblings, project, workspace = Character.objects.filter(project=item.project), item.project, item.project.workspace
    elif entity_type == "episode":
        item = get_object_or_404(Episode.objects.select_related("project__workspace").filter(project__workspace__in=accessible_workspaces(request.user)), id=entity_id)
        siblings, project, workspace = Episode.objects.filter(project=item.project), item.project, item.project.workspace
    elif entity_type == "scene":
        item = get_object_or_404(Scene.objects.select_related("episode__project__workspace").filter(episode__project__workspace__in=accessible_workspaces(request.user)), id=entity_id)
        siblings, project, workspace = Scene.objects.filter(episode=item.episode), item.episode.project, item.episode.project.workspace
    elif entity_type == "dialogue":
        item = get_object_or_404(DialogueLine.objects.select_related("scene__episode__project__workspace").filter(scene__episode__project__workspace__in=accessible_workspaces(request.user)), id=entity_id)
        siblings, project, workspace = DialogueLine.objects.filter(scene=item.scene), item.scene.episode.project, item.scene.episode.project.workspace
    elif entity_type == "prompt":
        item = get_object_or_404(Prompt.objects.select_related("scene__episode__project__workspace").filter(scene__episode__project__workspace__in=accessible_workspaces(request.user)), id=entity_id)
        siblings, project, workspace = Prompt.objects.filter(scene=item.scene), item.scene.episode.project, item.scene.episode.project.workspace
    else:
        return HttpResponseForbidden("Unsupported entity type.")
    if not has_capability(request.user, workspace, "edit"):
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
        Project.objects.select_related("workspace").prefetch_related("assets", "characters__assets", "episodes__scenes").filter(workspace__in=accessible_workspaces(request.user)),
        id=project_id,
    )
    return render(request, "studio/project_detail.html", {"project": project, "can_edit": has_capability(request.user, project.workspace, "edit")})


@login_required
def scene_detail(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").prefetch_related(
            "dialogue_lines", "prompts__ai_model", "prompts__blocks", "prompts__assets", "assets"
        ).filter(episode__project__workspace__in=accessible_workspaces(request.user)),
        id=scene_id,
    )
    return render(request, "studio/scene_detail.html", {
        "scene": scene,
        "can_edit": has_capability(request.user, scene.episode.project.workspace, "edit"),
        "ai_models": AiModelProfile.objects.filter(is_active=True),
        "prompt_types": Prompt.Type.choices,
        "prompt_statuses": Prompt.Status.choices,
        "block_types": PromptBlock.Type.choices,
        "scene_images": scene.assets.filter(prompt__isnull=True),
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

@login_required
def translation_workspace(request, project_id):
    project = get_object_or_404(Project.objects.filter(workspace__in=accessible_workspaces(request.user)), id=project_id)
    workspace = project.workspace
    target_language = (request.POST.get("target_language") or request.GET.get("target_language") or "en").strip().lower()[:16]
    if request.method == "POST" and request.POST.get("line_id"):
        if not has_capability(request.user, workspace, "translate"):
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
    lines = DialogueLine.objects.filter(scene__episode__project=project).prefetch_related("translations")
    rows = []
    for line in lines:
        unit = next((item for item in line.translations.all() if item.target_language == target_language), None)
        rows.append({"line": line, "unit": unit})
    return render(request, "studio/translations.html", {
        "project": project, "rows": rows, "target_language": target_language,
        "can_translate": has_capability(request.user, workspace, "translate"),
        "translation_statuses": [TranslationUnit.Status.DRAFT, TranslationUnit.Status.IN_REVIEW, TranslationUnit.Status.APPROVED],
    })


@login_required
def episode_subtitles(request, episode_id):
    episode = get_object_or_404(
        Episode.objects.select_related("project__workspace").filter(project__workspace__in=accessible_workspaces(request.user)),
        id=episode_id,
    )
    if request.method == "POST" and has_capability(request.user, episode.project.workspace, "translate"):
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
        "can_translate": has_capability(request.user, episode.project.workspace, "translate"),
    })


@login_required
def subtitle_track(request, track_id):
    track = get_object_or_404(
        SubtitleTrack.objects.select_related("episode__project__workspace").filter(
            episode__project__workspace__in=accessible_workspaces(request.user)
        ),
        id=track_id,
    )
    can_translate = has_capability(request.user, track.episode.project.workspace, "translate")
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
        ).filter(workspace__in=accessible_workspaces(request.user)),
        id=project_id,
    )
    can_export = has_capability(request.user, project.workspace, "export")
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
        ).filter(episode__project__workspace__in=accessible_workspaces(request.user)),
        id=scene_id,
    )
    workspace = scene.episode.project.workspace
    return render(request, "studio/scene_media.html", {
        "scene": scene,
        "can_edit": has_capability(request.user, workspace, "edit"),
    })


@login_required
def generation_create(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__workspace__in=accessible_workspaces(request.user)
        ),
        id=scene_id,
    )
    workspace = scene.episode.project.workspace
    if not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = AdditionalGenerationForm(request.POST or None, project=scene.episode.project)
    if request.method == "POST" and form.is_valid():
        item = form.save(commit=False)
        item.scene = scene
        item.position = scene.additional_generations.count()
        item.created_by = request.user
        item.updated_by = request.user
        item.save()
        record_revision(instance=item, user=request.user, operation="CREATE")
        audit(workspace=workspace, actor=request.user, action="GENERATION_CREATED", instance=item)
        messages.success(request, "Additional generation created. Upload one or more results.")
        return redirect("studio:scene_media", scene_id=scene.id)
    return render(request, "studio/entity_form.html", {
        "form": form, "title": "New additional generation", "submit_label": "Create generation",
    })


@login_required
def generation_edit(request, generation_id):
    item = get_object_or_404(
        AdditionalGeneration.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__workspace__in=accessible_workspaces(request.user)
        ),
        id=generation_id,
    )
    workspace = item.scene.episode.project.workspace
    if not has_capability(request.user, workspace, "edit"):
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
            scene__episode__project__workspace__in=accessible_workspaces(request.user)
        ),
        id=generation_id,
    )
    scene = generation.scene
    workspace = scene.episode.project.workspace
    if not has_capability(request.user, workspace, "edit"):
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
        ).filter(generation__scene__episode__project__workspace__in=accessible_workspaces(request.user)),
        id=output_id,
    )
    generation = output.generation
    workspace = generation.scene.episode.project.workspace
    if not has_capability(request.user, workspace, "edit"):
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
        Asset.objects.select_related("workspace", "project", "scene").filter(
            workspace__in=accessible_workspaces(request.user)
        ),
        id=asset_id,
    )
    if not has_capability(request.user, asset.workspace, "edit"):
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

@login_required
def docx_roundtrip(request, job_id):
    job = get_object_or_404(
        ExportJob.objects.select_related("project__workspace", "output_asset").filter(
            project__workspace__in=accessible_workspaces(request.user),
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
        "can_export": has_capability(request.user, job.workspace, "export"),
    })

def _prompt_editor_context():
    return {
        "ai_models": AiModelProfile.objects.filter(is_active=True),
        "prompt_types": Prompt.Type.choices,
        "prompt_statuses": Prompt.Status.choices,
        "block_types": PromptBlock.Type.choices,
    }


@login_required
def scene_prompt_quick_create(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__workspace__in=accessible_workspaces(request.user)
        ),
        id=scene_id,
    )
    workspace = scene.episode.project.workspace
    if request.method != "POST" or not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    ai_model = get_object_or_404(AiModelProfile.objects.filter(is_active=True), id=request.POST.get("ai_model"))
    prompt_type = request.POST.get("prompt_type", Prompt.Type.IMAGE)
    status = request.POST.get("status", Prompt.Status.DRAFT)
    if prompt_type not in Prompt.Type.values or status not in Prompt.Status.values:
        messages.error(request, "Choose a valid prompt type and status.")
        return redirect("studio:scene_detail", scene_id=scene.id)
    prompt = Prompt.objects.create(
        scene=scene,
        ai_model=ai_model,
        prompt_type=prompt_type,
        title=request.POST.get("title", "").strip(),
        status=status,
        position=scene.prompts.count(),
        created_by=request.user,
        updated_by=request.user,
    )
    content = request.POST.get("content", "").strip()
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
        Prompt.objects.select_related("scene__episode__project__workspace", "ai_model")
        .prefetch_related("blocks")
        .filter(scene__episode__project__workspace__in=accessible_workspaces(request.user)),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    if request.method != "POST" or not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    ai_model = get_object_or_404(AiModelProfile.objects.filter(is_active=True), id=request.POST.get("ai_model"))
    prompt_type = request.POST.get("prompt_type", prompt.prompt_type)
    status = request.POST.get("status", prompt.status)
    if prompt_type not in Prompt.Type.values or status not in Prompt.Status.values:
        messages.error(request, "Choose a valid prompt type and status.")
        return redirect("studio:scene_detail", scene_id=prompt.scene_id)
    with transaction.atomic():
        prompt.ai_model = ai_model
        prompt.prompt_type = prompt_type
        prompt.title = request.POST.get("title", "").strip()
        prompt.status = status
        prompt.updated_by = request.user
        prompt.full_clean()
        prompt.save()
        record_revision(instance=prompt, user=request.user, operation="INLINE_UPDATE")
        for block in prompt.blocks.all():
            if block.block_type == PromptBlock.Type.DIALOGUE_REFERENCE:
                continue
            value = request.POST.get(f"block_{block.id}")
            block_type = request.POST.get(f"block_type_{block.id}", block.block_type)
            if value is None or block_type not in PromptBlock.Type.values:
                continue
            block.content = value.strip()
            block.block_type = block_type
            block.updated_by = request.user
            block.save()
            record_revision(instance=block, user=request.user, operation="INLINE_UPDATE")
        new_content = request.POST.get("new_block_content", "").strip()
        new_type = request.POST.get("new_block_type", PromptBlock.Type.NARRATIVE)
        if new_content and new_type in PromptBlock.Type.values:
            block = PromptBlock.objects.create(
                prompt=prompt,
                block_type=new_type,
                content=new_content,
                position=prompt.blocks.count(),
                created_by=request.user,
                updated_by=request.user,
            )
            record_revision(instance=block, user=request.user, operation="CREATE")
        audit(workspace=workspace, actor=request.user, action="PROMPT_INLINE_UPDATED", instance=prompt)
    messages.success(request, "Prompt saved.")
    if request.POST.get("return_to") == "chain":
        return HttpResponseRedirect(reverse("studio:project_scene_chain", kwargs={"project_id": prompt.scene.episode.project_id}) + f"#prompt-{prompt.id}")
    return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": prompt.scene_id}) + f"#prompt-{prompt.id}")


@login_required
def prompt_image_upload(request, prompt_id):
    prompt = get_object_or_404(
        Prompt.objects.select_related("scene__episode__project__workspace").filter(
            scene__episode__project__workspace__in=accessible_workspaces(request.user)
        ),
        id=prompt_id,
    )
    workspace = prompt.scene.episode.project.workspace
    if request.method != "POST" or not has_capability(request.user, workspace, "edit"):
        return HttpResponseForbidden("Edit permission is required.")
    form = ImageUploadForm(request.POST, request.FILES)
    if form.is_valid():
        try:
            create_asset(
                user=request.user,
                workspace=workspace,
                uploaded=form.cleaned_data["file"],
                kind=Asset.Kind.OTHER,
                project=prompt.scene.episode.project,
                scene=prompt.scene,
                prompt=prompt,
            )
        except ValidationError as exc:
            messages.error(request, "; ".join(exc.messages))
        else:
            messages.success(request, "Prompt image attached.")
    else:
        messages.error(request, "Choose a valid JPG, PNG or WEBP image.")
    if request.POST.get("return_to") == "chain":
        return HttpResponseRedirect(reverse("studio:project_scene_chain", kwargs={"project_id": prompt.scene.episode.project_id}) + f"#prompt-{prompt.id}")
    return HttpResponseRedirect(reverse("studio:scene_detail", kwargs={"scene_id": prompt.scene_id}) + f"#prompt-{prompt.id}")


@login_required
def project_scene_chain(request, project_id):
    project = get_object_or_404(
        Project.objects.select_related("workspace").prefetch_related(
            Prefetch("episodes__scenes__assets", queryset=Asset.objects.filter(prompt__isnull=True), to_attr="chain_images"),
            "episodes__scenes__prompts__ai_model",
            "episodes__scenes__prompts__blocks",
            "episodes__scenes__prompts__assets",
        ).filter(workspace__in=accessible_workspaces(request.user)),
        id=project_id,
    )
    context = {
        "project": project,
        "can_edit": has_capability(request.user, project.workspace, "edit"),
        **_prompt_editor_context(),
    }
    return render(request, "studio/project_scene_chain.html", context)


@login_required
def scene_quick_save(request, scene_id):
    scene = get_object_or_404(
        Scene.objects.select_related("episode__project__workspace").filter(
            episode__project__workspace__in=accessible_workspaces(request.user)
        ),
        id=scene_id,
    )
    workspace = scene.episode.project.workspace
    if request.method != "POST" or not has_capability(request.user, workspace, "edit"):
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
            project__workspace__in=accessible_workspaces(request.user)
        ),
        id=episode_id,
    )
    workspace = episode.project.workspace
    if request.method != "POST" or not has_capability(request.user, workspace, "edit"):
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
            scene.save(update_fields=["position", "updated_at"])
        for position, scene_id in enumerate(ordered_ids):
            scene = by_id[scene_id]
            scene.position = position
            scene.updated_by = request.user
            scene.save(update_fields=["position", "updated_by", "updated_at"])
        audit(
            workspace=workspace,
            actor=request.user,
            action="SCENES_DRAG_REORDERED",
            instance=episode,
            metadata={"sceneIds": ordered_ids},
        )
    return JsonResponse({"ok": True, "sceneIds": ordered_ids})