from django.db import migrations, models


LANGUAGE_MODELS = {
    "Workspace": ("documentation_language", "dialogue_language", "prompt_language"),
    "Project": ("original_language", "documentation_language", "dialogue_language", "prompt_language"),
    "Episode": ("language",),
    "EpisodeCover": ("language_code",),
    "DialogueLine": ("language",),
    "Prompt": ("original_language", "language"),
    "SubtitleTrack": ("language",),
    "TranslationUnit": ("target_language",),
}


def rename_belarus_code(apps, schema_editor):
    for model_name, field_names in LANGUAGE_MODELS.items():
        model = apps.get_model("lexamora_studio", model_name)
        for field_name in field_names:
            model.objects.filter(**{field_name: "BL"}).update(**{field_name: "BY"})

    project_model = apps.get_model("lexamora_studio", "Project")
    for item in project_model.objects.all().only("id", "translation_languages"):
        languages = list(item.translation_languages or [])
        updated = ["BY" if value == "BL" else value for value in languages]
        if updated != languages:
            item.translation_languages = updated
            item.save(update_fields=["translation_languages"])


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0032_project_translation_model_docx_archive")]

    operations = [
        migrations.RunPython(rename_belarus_code, migrations.RunPython.noop),
        migrations.AlterField(
            model_name="prompt",
            name="language",
            field=models.CharField(
                choices=[("BY", "BY"), ("DE", "DE"), ("EN", "EN"), ("ES", "ES"), ("PL", "PL"), ("PT", "PT"), ("RU", "RU"), ("UA", "UA")],
                default="EN",
                max_length=2,
            ),
        ),
        migrations.AlterField(
            model_name="prompt",
            name="original_language",
            field=models.CharField(
                choices=[("BY", "BY"), ("DE", "DE"), ("EN", "EN"), ("ES", "ES"), ("PL", "PL"), ("PT", "PT"), ("RU", "RU"), ("UA", "UA")],
                default="EN",
                max_length=2,
            ),
        ),
    ]
