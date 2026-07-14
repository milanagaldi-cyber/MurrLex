from django.db import migrations, models


def final_to_production(apps, schema_editor):
    Scene = apps.get_model("lexamora_studio", "Scene")
    Scene._base_manager.filter(status="FINAL").update(status="PRODUCTION")


def production_to_final(apps, schema_editor):
    Scene = apps.get_model("lexamora_studio", "Scene")
    Scene._base_manager.filter(status="PRODUCTION").update(status="FINAL")


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0025_shared_asset_links")]

    operations = [
        migrations.AddField(
            model_name="scene",
            name="scene_type",
            field=models.CharField(
                choices=[
                    ("ORIGINAL", "Original"),
                    ("ALTERNATIVE", "Alternative"),
                    ("ADDITIONAL_GENERATION", "Additional generation"),
                ],
                default="ORIGINAL",
                max_length=24,
            ),
        ),
        migrations.RunPython(final_to_production, production_to_final),
        migrations.AlterField(
            model_name="scene",
            name="status",
            field=models.CharField(
                choices=[
                    ("DRAFT", "Draft"),
                    ("IN_REVIEW", "In review"),
                    ("APPROVED", "Approved"),
                    ("NEEDS_CHANGES", "Needs changes"),
                    ("PRODUCTION", "Production"),
                ],
                default="DRAFT",
                max_length=20,
            ),
        ),
    ]
