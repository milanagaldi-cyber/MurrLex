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
    path("assets", api.assets, name="assets"),
    path("assets/<uuid:asset_id>", api.asset_detail, name="asset_detail"),
    path("assets/<uuid:asset_id>/download", api.asset_download, name="asset_download"),
    path("assets/<uuid:asset_id>/thumbnail", api.asset_thumbnail, name="asset_thumbnail"),
    path("scenes/<uuid:scene_id>/generations", api.scene_generations, name="scene_generations"),
    path("generations/<uuid:generation_id>/outputs", api.generation_outputs, name="generation_outputs"),
    path("generation-outputs/<uuid:output_id>/final", api.generation_output_final, name="generation_output_final"),
]
