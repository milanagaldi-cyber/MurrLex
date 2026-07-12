from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.shortcuts import get_object_or_404, redirect, render
from django.utils.text import slugify

from lessons.ai_gateway import ProviderError
from lessons.provider_credentials import user_has_ai_access

from .ai import StudioAiError, accept_suggestion, improve_prompt, reject_suggestion
from .exports import ALL_SECTIONS, ExportError, generate_export
from .models import AiSuggestion, DialogueLine, Episode, ExportJob, Project, Prompt, Scene, SubtitleTrack, TranslationUnit, Workspace
from .permissions import accessible_workspaces, has_capability
from .services import bulk_replace_subtitle_lines, create_workspace, reorder_subtitle_lines, save_translation


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
            "episodes__scenes__additional_generations__outputs__asset", "episodes__subtitle_tracks__lines",
        ).filter(workspace__in=accessible_workspaces(request.user)),
        id=project_id,
    )
    can_export = has_capability(request.user, project.workspace, "export")
    if request.method == "POST" and can_export:
        episode = None
        if request.POST.get("episode_id"):
            episode = get_object_or_404(Episode.objects.filter(project=project), id=request.POST["episode_id"])
        try:
            generate_export(
                project=project, episode=episode,
                sections=request.POST.getlist("sections"), user=request.user,
            )
        except ExportError as exc:
            messages.error(request, str(exc))
        except Exception:
            messages.error(request, "PDF generation failed.")
        else:
            messages.success(request, "PDF export is ready.")
        return redirect("studio:project_exports", project_id=project.id)
    return render(request, "studio/exports.html", {
        "project": project, "jobs": project.export_jobs.select_related("episode", "output_asset")[:50],
        "sections": ALL_SECTIONS, "can_export": can_export,
    })