from django.apps import AppConfig


class LexamoraStudioConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "lexamora_studio"

    def ready(self):
        from . import signals  # noqa: F401
