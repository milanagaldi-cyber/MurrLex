from django.db import migrations, models


def seed_google_models(apps, schema_editor):
    Profile = apps.get_model("lexamora_studio", "AiModelProfile")
    TextModel = apps.get_model("lexamora_studio", "StudioTextModel")
    ratios = ["1:1", "2:3", "3:2", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9", "21:9"]
    profiles = (
        ("Nano Banana 2 Lite", "gemini-3.1-flash-lite-image", "IMAGE", {"max_references": 14, "sizes": ["1K"], "aspect_ratios": ratios, "size": "1K", "aspect_ratio": "1:1", "output_format": "png"}),
        ("Nano Banana 2", "gemini-3.1-flash-image", "IMAGE", {"max_references": 14, "sizes": ["1K", "2K", "4K"], "aspect_ratios": ratios, "size": "2K", "aspect_ratio": "1:1", "output_format": "png"}),
        ("Veo 3.1 Lite", "veo-3.1-lite-generate-preview", "VIDEO", {"max_references": 3, "sizes": ["720p", "1080p"], "aspect_ratios": ["9:16", "16:9"], "size": "720p", "aspect_ratio": "16:9"}),
        ("Veo 3.1 Fast", "veo-3.1-fast-generate-preview", "VIDEO", {"max_references": 3, "sizes": ["720p", "1080p"], "aspect_ratios": ["9:16", "16:9"], "size": "1080p", "aspect_ratio": "16:9"}),
    )
    for name, model_id, media_type, defaults in profiles:
        profile = Profile.objects.filter(name=name).first() or Profile.objects.filter(model_id=model_id).first()
        if profile is None:
            profile = Profile(name=name)
        profile.name = name
        profile.model_id = model_id
        profile.provider = "Google AI Studio"
        profile.media_type = media_type
        profile.is_active = True
        profile.defaults = defaults
        profile.save()
    TextModel.objects.update_or_create(
        model_id="gemini-3.1-flash-lite",
        defaults={"name": "Gemini 3.1 Flash Lite", "provider": "google", "is_active": True, "is_default": False},
    )


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0038_episodecomic_projectassistantcontext_usertokenquota")]
    operations = [
        migrations.AlterField(model_name="studiotextmodel", name="provider", field=models.CharField(choices=[("openai", "OpenAI"), ("google", "Google AI Studio")], default="openai", max_length=32)),
        migrations.RunPython(seed_google_models, migrations.RunPython.noop),
    ]
