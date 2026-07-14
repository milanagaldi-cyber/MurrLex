import uuid

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("lexamora_studio", "0019_normalize_scene_numbers"),
    ]

    operations = [
        migrations.AddField(
            model_name="workspace",
            name="avatar_asset",
            field=models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="workspace_avatar_for", to="lexamora_studio.asset"),
        ),
        migrations.CreateModel(
            name="ProjectAccessExclusion",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("project", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="access_exclusions", to="lexamora_studio.project")),
                ("revoked_by", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="revoked_studio_project_access", to=settings.AUTH_USER_MODEL)),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="studio_project_access_exclusions", to=settings.AUTH_USER_MODEL)),
            ],
            options={"constraints": [models.UniqueConstraint(fields=("project", "user"), name="studio_unique_project_access_exclusion")]},
        ),
    ]
