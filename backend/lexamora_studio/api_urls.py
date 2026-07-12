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
]
