from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0017_prompt_reference_assets")]

    operations = [
        migrations.AddField(
            model_name="project",
            name="prompt_template",
            field=models.TextField(blank=True),
        ),
    ]
