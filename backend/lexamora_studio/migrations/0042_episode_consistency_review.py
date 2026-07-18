import uuid

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("lexamora_studio", "0041_studiouserpreference_ai_enabled_and_more"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="EpisodeConsistencyReview",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("status", models.CharField(choices=[("SUCCESS", "Completed"), ("ERROR", "Failed")], default="SUCCESS", max_length=16)),
                ("model", models.CharField(blank=True, max_length=160)),
                ("content", models.TextField(blank=True)),
                ("error_message", models.TextField(blank=True)),
                ("image_count", models.PositiveIntegerField(default=0)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("created_by", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="studio_episode_consistency_reviews", to=settings.AUTH_USER_MODEL)),
                ("episode", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="consistency_reviews", to="lexamora_studio.episode")),
            ],
            options={"ordering": ["-created_at", "id"]},
        ),
    ]
