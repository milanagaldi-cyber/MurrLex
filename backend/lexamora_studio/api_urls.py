from django.urls import path

from . import api

app_name = "studio_api"

urlpatterns = [
    path("workspaces", api.workspaces, name="workspaces"),
    path("projects", api.projects, name="projects"),
    path("projects/<uuid:project_id>", api.project_detail, name="project_detail"),
]
