from django.urls import path

from . import views

app_name = "studio"

urlpatterns = [
    path("", views.dashboard, name="dashboard"),
    path("workspaces/new/", views.workspace_create, name="workspace_create"),
    path("workspaces/<uuid:workspace_id>/", views.workspace_detail, name="workspace_detail"),
    path("workspaces/<uuid:workspace_id>/imports/docx/new/", views.docx_import_create, name="docx_import_create"),
    path("imports/<uuid:import_id>/", views.docx_import_detail, name="docx_import_detail"),
    path("imports/<uuid:import_id>/accept/", views.docx_import_accept, name="docx_import_accept"),
    path("workspaces/<uuid:workspace_id>/projects/new/", views.project_create, name="project_create"),
    path("projects/<uuid:project_id>/", views.project_detail, name="project_detail"),
    path("projects/<uuid:project_id>/edit/", views.project_edit, name="project_edit"),
    path("projects/<uuid:project_id>/images/new/", views.project_image_upload, name="project_image_upload"),
    path("projects/<uuid:project_id>/characters/new/", views.character_create, name="character_create"),
    path("characters/<uuid:character_id>/edit/", views.character_edit, name="character_edit"),
    path("characters/<uuid:character_id>/images/new/", views.character_image_upload, name="character_image_upload"),
    path("projects/<uuid:project_id>/episodes/new/", views.episode_create, name="episode_create"),
    path("episodes/<uuid:episode_id>/edit/", views.episode_edit, name="episode_edit"),
    path("episodes/<uuid:episode_id>/scenes/new/", views.scene_create, name="scene_create"),
    path("scenes/<uuid:scene_id>/", views.scene_detail, name="scene_detail"),
    path("scenes/<uuid:scene_id>/edit/", views.scene_edit, name="scene_edit"),
    path("scenes/<uuid:scene_id>/images/new/", views.scene_image_upload, name="scene_image_upload"),
    path("scenes/<uuid:scene_id>/dialogue/new/", views.dialogue_create, name="dialogue_create"),
    path("dialogue/<uuid:line_id>/edit/", views.dialogue_edit, name="dialogue_edit"),
    path("scenes/<uuid:scene_id>/prompts/new/", views.prompt_create, name="prompt_create"),
    path("prompts/<uuid:prompt_id>/", views.prompt_detail, name="prompt_detail"),
    path("prompts/<uuid:prompt_id>/edit/", views.prompt_edit, name="prompt_edit"),
    path("prompts/<uuid:prompt_id>/blocks/new/", views.prompt_block_create, name="prompt_block_create"),
    path("prompt-blocks/<uuid:block_id>/edit/", views.prompt_block_edit, name="prompt_block_edit"),
    path("prompts/<uuid:prompt_id>/improve/", views.prompt_improve, name="prompt_improve"),
    path("suggestions/<uuid:suggestion_id>/<str:decision>/", views.suggestion_decide, name="suggestion_decide"),
    path("projects/<uuid:project_id>/translations/", views.translation_workspace, name="translations"),
    path("episodes/<uuid:episode_id>/subtitles/", views.episode_subtitles, name="episode_subtitles"),
    path("subtitle-tracks/<uuid:track_id>/", views.subtitle_track, name="subtitle_track"),
    path("projects/<uuid:project_id>/exports/", views.project_exports, name="project_exports"),
]
