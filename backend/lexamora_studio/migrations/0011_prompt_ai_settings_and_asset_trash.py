import django.db.models.deletion
import uuid

from django.conf import settings
from django.db import migrations, models


DEFAULT_TEMPLATE = (
    "Write a production-ready English generation prompt. Preserve the established characters, "
    "location, continuity, visual style, camera intent, and all explicit scene constraints. "
    "Keep direct speech in its separately supplied dialogue block and do not invent dialogue."
)


def seed_ai_settings(apps, schema_editor):
    PromptTemplate = apps.get_model("lexamora_studio", "PromptTemplate")
    StudioTextModel = apps.get_model("lexamora_studio", "StudioTextModel")
    Prompt = apps.get_model("lexamora_studio", "Prompt")

    template, _ = PromptTemplate.objects.get_or_create(
        name="Default production prompt",
        defaults={"content": DEFAULT_TEMPLATE, "scope": "ALL", "is_active": True, "is_default": True},
    )
    if not template.is_default:
        template.is_default = True
        template.is_active = True
        template.save(update_fields=["is_default", "is_active"])
    Prompt._base_manager.filter(template__isnull=True).update(template=template)

    model_ids = ("gpt-5.4-nano", "gpt-5.4-mini", "gpt-5.4", "gpt-5.5")
    murrlex_default = settings.OPENAI_TRANSLATION_MODEL if settings.OPENAI_TRANSLATION_MODEL in model_ids else "gpt-5.4-mini"
    for model_id in model_ids:
        StudioTextModel.objects.get_or_create(
            model_id=model_id,
            defaults={
                "name": model_id,
                "provider": "openai",
                "is_active": True,
                "is_default": model_id == murrlex_default,
            },
        )


class Migration(migrations.Migration):
    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("lexamora_studio", "0010_asset_prompt"),
    ]

    operations = [
        migrations.CreateModel(
            name="PromptTemplate",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("name", models.CharField(max_length=160, unique=True)),
                ("content", models.TextField()),
                ("scope", models.CharField(choices=[("ALL", "Image and video"), ("IMAGE", "Image"), ("VIDEO", "Video")], default="ALL", max_length=16)),
                ("is_active", models.BooleanField(default=True)),
                ("is_default", models.BooleanField(default=False)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("updated_by", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="updated_prompt_templates", to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ["name", "id"]},
        ),
        migrations.CreateModel(
            name="StudioTextModel",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("name", models.CharField(max_length=120)),
                ("provider", models.CharField(choices=[("openai", "OpenAI")], default="openai", max_length=32)),
                ("model_id", models.CharField(max_length=160, unique=True)),
                ("is_active", models.BooleanField(default=True)),
                ("is_default", models.BooleanField(default=False)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("updated_by", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="updated_studio_text_models", to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ["name", "model_id"]},
        ),
        migrations.AddField(
            model_name="asset",
            name="purged_at",
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="asset",
            name="purged_by",
            field=models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.PROTECT, related_name="purged_studio_assets", to=settings.AUTH_USER_MODEL),
        ),
        migrations.AddField(
            model_name="promptblock",
            name="translated_content",
            field=models.TextField(blank=True),
        ),
        migrations.AddField(
            model_name="promptblock",
            name="translation_language",
            field=models.CharField(blank=True, max_length=16),
        ),
        migrations.AddField(
            model_name="promptblock",
            name="translation_model",
            field=models.CharField(blank=True, max_length=160),
        ),
        migrations.AddField(
            model_name="prompt",
            name="template",
            field=models.ForeignKey(null=True, on_delete=django.db.models.deletion.PROTECT, related_name="prompts", to="lexamora_studio.prompttemplate"),
        ),
        migrations.RunPython(seed_ai_settings, migrations.RunPython.noop),
        migrations.AlterField(
            model_name="prompt",
            name="template",
            field=models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="prompts", to="lexamora_studio.prompttemplate"),
        ),
    ]
