from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0030_episodecover")]

    operations = [
        migrations.AddField(
            model_name="project",
            name="hidden_sections",
            field=models.JSONField(blank=True, default=list),
        ),
    ]
