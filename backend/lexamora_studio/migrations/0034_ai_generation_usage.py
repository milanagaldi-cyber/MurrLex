from django.db import migrations, models
import django.db.models.deletion


def seed_openai_image_models(apps, schema_editor):
    profile = apps.get_model("lexamora_studio", "AiModelProfile")
    workspace = apps.get_model("lexamora_studio", "Workspace")
    legacy, _ = profile.objects.update_or_create(
        name="OpenAI GPT Image 1",
        defaults={
            "provider": "OpenAI",
            "model_id": "gpt-image-1",
            "media_type": "IMAGE",
            "is_active": True,
            "defaults": {"size": "1024x1024", "quality": "low"},
        },
    )
    profile.objects.update_or_create(
        name="OpenAI GPT Image 2",
        defaults={
            "provider": "OpenAI",
            "model_id": "gpt-image-2",
            "media_type": "IMAGE",
            "is_active": True,
            "defaults": {"size": "1024x1024", "quality": "low"},
        },
    )
    workspace.objects.filter(default_image_model__isnull=True).update(default_image_model=legacy)


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0033_belarus_language_code_by")]

    operations = [
        migrations.AddField(
            model_name="workspace",
            name="default_image_model",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.SET_NULL,
                related_name="default_for_workspaces",
                to="lexamora_studio.aimodelprofile",
            ),
        ),
        migrations.AddField(
            model_name="asset",
            name="ai_metadata",
            field=models.JSONField(blank=True, default=dict),
        ),
        migrations.AlterField(
            model_name="aiusagelog",
            name="prompt",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.PROTECT,
                related_name="ai_usage",
                to="lexamora_studio.prompt",
            ),
        ),
        migrations.AddField(
            model_name="aiusagelog",
            name="input_tokens",
            field=models.PositiveIntegerField(default=0),
        ),
        migrations.AddField(
            model_name="aiusagelog",
            name="output_tokens",
            field=models.PositiveIntegerField(default=0),
        ),
        migrations.AddField(
            model_name="aiusagelog",
            name="total_tokens",
            field=models.PositiveIntegerField(default=0),
        ),
        migrations.RunPython(seed_openai_image_models, migrations.RunPython.noop),
    ]
