from django.db import migrations, models


SUPPORTED_LANGUAGES = {"BL", "DE", "EN", "ES", "PL", "PT", "RU", "UA"}


def populate_original_languages(apps, schema_editor):
    Prompt = apps.get_model("lexamora_studio", "Prompt")
    roots = Prompt.objects.filter(source_prompt_id__isnull=True).select_related("scene__episode__project")
    for prompt in roots.iterator():
        project_language = (prompt.scene.episode.project.original_language or "").strip().upper()
        original_language = project_language if project_language in SUPPORTED_LANGUAGES else prompt.language
        Prompt.objects.filter(id=prompt.id).update(
            original_language=original_language,
            language=original_language,
        )
        Prompt.objects.filter(source_prompt_id=prompt.id).update(original_language=original_language)


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0015_prompt_unified_content")]

    operations = [
        migrations.AddField(
            model_name="prompt",
            name="original_language",
            field=models.CharField(
                choices=[
                    ("BL", "BL"), ("DE", "DE"), ("EN", "EN"), ("ES", "ES"),
                    ("PL", "PL"), ("PT", "PT"), ("RU", "RU"), ("UA", "UA"),
                ],
                default="EN",
                max_length=2,
            ),
        ),
        migrations.RunPython(populate_original_languages, migrations.RunPython.noop),
    ]
