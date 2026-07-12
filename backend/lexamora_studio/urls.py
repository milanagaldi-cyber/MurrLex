from django.urls import path

from . import views

app_name = "studio"

urlpatterns = [
    path("", views.dashboard, name="dashboard"),
    path("workspaces/new/", views.workspace_create, name="workspace_create"),
    path("workspaces/<uuid:workspace_id>/", views.workspace_detail, name="workspace_detail"),
    path("projects/<uuid:project_id>/", views.project_detail, name="project_detail"),
    path("scenes/<uuid:scene_id>/", views.scene_detail, name="scene_detail"),
    path("prompts/<uuid:prompt_id>/", views.prompt_detail, name="prompt_detail"),
    path("prompts/<uuid:prompt_id>/improve/", views.prompt_improve, name="prompt_improve"),
    path("suggestions/<uuid:suggestion_id>/<str:decision>/", views.suggestion_decide, name="suggestion_decide"),
]
