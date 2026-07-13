from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0016_prompt_original_language")]
    operations = [
        migrations.AddField(
            model_name="prompt",
            name="reference_assets",
            field=models.ManyToManyField(blank=True, related_name="referenced_by_prompts", to="lexamora_studio.asset"),
        ),
        migrations.AddField(
            model_name="project",
            name="purged_at",
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="project",
            name="purged_by",
            field=models.ForeignKey(blank=True, null=True, on_delete=models.deletion.PROTECT, related_name="purged_studio_projects", to=settings.AUTH_USER_MODEL),
        ),
    ]
