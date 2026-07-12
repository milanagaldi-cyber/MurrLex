from django.db import migrations


MODELS = [
    {"name": "Nano Banana 2", "provider": "Google", "model_id": "nano-banana-2", "media_type": "IMAGE"},
    {"name": "Kling 3.0", "provider": "Kuaishou", "model_id": "kling-3.0", "media_type": "VIDEO"},
    {"name": "Veo 3.1 Fast", "provider": "Google", "model_id": "veo-3.1-fast", "media_type": "VIDEO"},
]


def seed_models(apps, schema_editor):
    profile = apps.get_model("lexamora_studio", "AiModelProfile")
    for item in MODELS:
        profile.objects.update_or_create(name=item["name"], defaults=item)


def unseed_models(apps, schema_editor):
    profile = apps.get_model("lexamora_studio", "AiModelProfile")
    profile.objects.filter(name__in=[item["name"] for item in MODELS]).delete()


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0001_initial")]
    operations = [migrations.RunPython(seed_models, unseed_models)]
