import django.db.models.deletion

from django.db import migrations, models


def simplify_seeded_prompt_addition(apps, schema_editor):
    PromptTemplate = apps.get_model("lexamora_studio", "PromptTemplate")
    seeded = PromptTemplate.objects.filter(
        is_default=True,
        content__startswith="Write a production-ready English generation prompt.",
    ).first()
    if seeded is not None:
        if not PromptTemplate.objects.exclude(pk=seeded.pk).filter(name="Default prompt addition").exists():
            seeded.name = "Default prompt addition"
        seeded.content = "Negative Prompt: No Music"
        seeded.scope = "ALL"
        seeded.is_active = True
        seeded.save(update_fields=["name", "content", "scope", "is_active"])


class Migration(migrations.Migration):
    dependencies = [
        ("lexamora_studio", "0011_prompt_ai_settings_and_asset_trash"),
    ]

    operations = [
        migrations.AlterField(
            model_name="prompttemplate",
            name="content",
            field=models.TextField(blank=True),
        ),
        migrations.AddField(
            model_name="prompt",
            name="language",
            field=models.CharField(
                choices=[
                    ("BL", "BL"), ("DE", "DE"), ("EN", "EN"), ("ES", "ES"),
                    ("PL", "PL"), ("PT", "PT"), ("RU", "RU"), ("UA", "UA"),
                ],
                default="EN",
                max_length=2,
            ),
        ),
        migrations.AddField(
            model_name="prompt",
            name="source_prompt",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.PROTECT,
                related_name="translations",
                to="lexamora_studio.prompt",
            ),
        ),
        migrations.AddField(
            model_name="prompt",
            name="translation_scope",
            field=models.CharField(
                choices=[
                    ("ORIGINAL", "Original"),
                    ("FULL", "Full text"),
                    ("DIALOGUE", "Dialogue only"),
                ],
                default="ORIGINAL",
                max_length=16,
            ),
        ),
        migrations.AddField(
            model_name="aisuggestion",
            name="original_blocks",
            field=models.JSONField(default=list),
        ),
        migrations.AlterField(
            model_name="aisuggestion",
            name="status",
            field=models.CharField(
                choices=[
                    ("PENDING", "Pending"),
                    ("ACCEPTED", "Accepted"),
                    ("REJECTED", "Rejected"),
                    ("UNDONE", "Undone"),
                ],
                default="PENDING",
                max_length=16,
            ),
        ),
        migrations.RunPython(simplify_seeded_prompt_addition, migrations.RunPython.noop),
    ]
