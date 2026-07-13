from django.db import migrations, models


def combine_prompt_blocks(apps, schema_editor):
    Prompt = apps.get_model("lexamora_studio", "Prompt")
    PromptBlock = apps.get_model("lexamora_studio", "PromptBlock")
    for prompt in Prompt.objects.all().iterator():
        rows = PromptBlock.objects.filter(prompt_id=prompt.id, deleted_at__isnull=True).order_by("position", "id")
        content = "\n\n".join(value.strip() for value in rows.values_list("content", flat=True) if value.strip())
        if content:
            Prompt.objects.filter(id=prompt.id).update(content=content)


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0014_grant_project_members_ai_access")]

    operations = [
        migrations.AddField(
            model_name="prompt",
            name="content",
            field=models.TextField(blank=True),
        ),
        migrations.AlterField(
            model_name="prompt",
            name="translation_scope",
            field=models.CharField(
                choices=[
                    ("ORIGINAL", "Original"),
                    ("FULL", "Full text"),
                    ("DIALOGUE", "Dialogue only"),
                    ("SELECTED", "Selected text"),
                ],
                default="ORIGINAL",
                max_length=16,
            ),
        ),
        migrations.RunPython(combine_prompt_blocks, migrations.RunPython.noop),
    ]
