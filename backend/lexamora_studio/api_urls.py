from django.urls import path

from . import api

app_name = "studio_api"

urlpatterns = [
    path("workspaces", api.workspaces, name="workspaces"),
    path("projects", api.projects, name="projects"),
    path("projects/<uuid:project_id>", api.project_detail, name="project_detail"),
    path("scenes/<uuid:scene_id>", api.scene_detail, name="scene_detail"),
    path("scenes/<uuid:scene_id>/dialogue", api.scene_dialogue, name="scene_dialogue"),
    path("dialogue/<uuid:line_id>", api.dialogue_detail, name="dialogue_detail"),
    path("scenes/<uuid:scene_id>/prompts", api.scene_prompts, name="scene_prompts"),
    path("imports/docx", api.docx_imports, name="docx_imports"),
    path("imports/<uuid:import_id>", api.docx_import_detail, name="docx_import_detail"),
    path("imports/<uuid:import_id>/accept", api.docx_import_accept, name="docx_import_accept"),
    path("assets", api.assets, name="assets"),
    path("assets/<uuid:asset_id>", api.asset_detail, name="asset_detail"),
    path("assets/<uuid:asset_id>/view", api.asset_view, name="asset_view"),
    path("assets/<uuid:asset_id>/download", api.asset_download, name="asset_download"),
    path("assets/<uuid:asset_id>/thumbnail", api.asset_thumbnail, name="asset_thumbnail"),
    path("scenes/<uuid:scene_id>/generations", api.scene_generations, name="scene_generations"),
    path("generations/<uuid:generation_id>/outputs", api.generation_outputs, name="generation_outputs"),
    path("generation-outputs/<uuid:output_id>/final", api.generation_output_final, name="generation_output_final"),
    path("entities/<str:entity_type>/<uuid:entity_id>/revisions", api.entity_revisions, name="entity_revisions"),
    path("entities/<str:entity_type>/<uuid:entity_id>/revisions/compare", api.revision_compare, name="revision_compare"),
    path("entities/<str:entity_type>/<uuid:entity_id>/restore/<uuid:revision_id>", api.revision_restore, name="revision_restore"),
    path("prompts/<uuid:prompt_id>/improve", api.prompt_improve, name="prompt_improve"),
    path("prompts/<uuid:prompt_id>/translate-dialogue", api.prompt_translate_dialogue, name="prompt_translate_dialogue"),
    path("prompts/<uuid:prompt_id>/translate", api.prompt_translate, name="prompt_translate"),
    path("suggestions/<uuid:suggestion_id>/accept", api.suggestion_accept, name="suggestion_accept"),
    path("suggestions/<uuid:suggestion_id>/reject", api.suggestion_reject, name="suggestion_reject"),
    path("suggestions/<uuid:suggestion_id>/undo", api.suggestion_undo, name="suggestion_undo"),
    path("episodes/<uuid:episode_id>/subtitle-tracks", api.episode_subtitle_tracks, name="episode_subtitle_tracks"),
    path("subtitle-tracks/<uuid:track_id>/lines", api.subtitle_lines, name="subtitle_lines"),
    path("subtitle-tracks/<uuid:track_id>/lines/reorder", api.subtitle_lines_reorder, name="subtitle_lines_reorder"),
    path("projects/<uuid:project_id>/translations", api.project_translations, name="project_translations"),
    path("dialogue/<uuid:line_id>/translations/<str:target_language>", api.dialogue_translation, name="dialogue_translation"),
    path("projects/<uuid:project_id>/export/pdf", api.project_export_pdf, name="project_export_pdf"),
    path("exports/<uuid:export_id>", api.export_detail, name="export_detail"),
    path("exports/<uuid:export_id>/download", api.export_download, name="export_download"),
]
