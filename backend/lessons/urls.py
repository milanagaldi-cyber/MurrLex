from django.urls import path

from . import views

app_name = "lessons"

urlpatterns = [
    path("import-json/", views.import_json, name="import_json"),
    path("lessons/", views.lesson_list, name="lesson_list"),
    path("lessons/<int:lesson_id>/", views.lesson_detail, name="lesson_detail"),
    path("imports/", views.import_log_list, name="import_log_list"),
]
