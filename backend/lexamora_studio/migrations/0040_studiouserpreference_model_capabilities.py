from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


def update_model_capabilities(apps, schema_editor):
    Profile = apps.get_model("lexamora_studio", "AiModelProfile")
    capabilities = {
        "gpt-image-1": {
            "max_references": 10,
            "max_outputs": 10,
            "sizes": ["1024x1024", "1024x1536", "1536x1024"],
            "aspect_ratios": ["1:1", "2:3", "3:2", "9:16", "16:9"],
            "formats": ["png", "jpeg", "webp"],
            "size": "1024x1024",
            "quality": "low",
            "output_format": "png",
            "ui_fields": ["composition", "image-size", "quality", "format", "quantity"],
        },
        "gpt-image-2": {
            "max_references": 10,
            "max_outputs": 10,
            "sizes": ["1024x1024", "1024x1536", "1536x1024"],
            "aspect_ratios": ["1:1", "2:3", "3:2", "9:16", "16:9"],
            "formats": ["png", "jpeg", "webp"],
            "size": "1024x1024",
            "quality": "low",
            "output_format": "png",
            "ui_fields": ["composition", "image-size", "quality", "format", "quantity"],
        },
        "gemini-3.1-flash-lite-image": {
            "max_references": 14,
            "max_outputs": 1,
            "sizes": ["1K"],
            "formats": ["png", "jpeg"],
            "ui_fields": ["composition", "image-size", "format"],
        },
        "gemini-3.1-flash-image": {
            "max_references": 14,
            "max_outputs": 1,
            "sizes": ["0.5K", "1K", "2K", "4K"],
            "formats": ["png", "jpeg"],
            "ui_fields": ["composition", "image-size", "format"],
        },
        "veo-3.1-lite-generate-preview": {
            "max_references": 3,
            "max_outputs": 2,
            "sizes": ["720p", "1080p"],
            "aspect_ratios": ["9:16", "16:9"],
            "ui_fields": ["video-ratio", "video-size", "quantity"],
        },
        "veo-3.1-fast-generate-preview": {
            "max_references": 3,
            "max_outputs": 2,
            "sizes": ["720p", "1080p"],
            "aspect_ratios": ["9:16", "16:9"],
            "ui_fields": ["video-ratio", "video-size", "quantity"],
        },
    }
    for model_id, values in capabilities.items():
        profile = Profile.objects.filter(model_id=model_id).first()
        if profile is None:
            continue
        defaults = dict(profile.defaults or {})
        defaults.update(values)
        profile.defaults = defaults
        profile.save(update_fields=["defaults"])


class Migration(migrations.Migration):
    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("lexamora_studio", "0039_google_ai_models"),
    ]

    operations = [
        migrations.CreateModel(
            name="StudioUserPreference",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("speech_language", models.CharField(default="en-US", max_length=16)),
                ("speech_continuous", models.BooleanField(default=False)),
                ("speech_interim", models.BooleanField(default=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("user", models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name="studio_preferences", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.RunPython(update_model_capabilities, migrations.RunPython.noop),
    ]
