from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("lexamora_studio", "0031_project_hidden_sections"),
    ]

    operations = [
        migrations.AddField(
            model_name="project",
            name="default_translation_model",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.SET_NULL,
                related_name="default_for_projects",
                to="lexamora_studio.studiotextmodel",
            ),
        ),
        migrations.AddField(
            model_name="docximport",
            name="archived_at",
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="docximport",
            name="archived_by",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.PROTECT,
                related_name="archived_studio_docx_imports",
                to=settings.AUTH_USER_MODEL,
            ),
        ),
    ]
