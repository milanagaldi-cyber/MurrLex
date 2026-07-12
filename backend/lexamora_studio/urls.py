from django.urls import path

from . import views

app_name = "studio"

urlpatterns = [
    path("", views.dashboard, name="dashboard"),
    path("workspaces/new/", views.workspace_create, name="workspace_create"),
    path("workspaces/<uuid:workspace_id>/", views.workspace_detail, name="workspace_detail"),
    path("projects/<uuid:project_id>/", views.project_detail, name="project_detail"),
    path("scenes/<uuid:scene_id>/", views.scene_detail, name="scene_detail"),
]
