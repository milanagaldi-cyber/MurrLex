import uuid

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


def update_video_capabilities(apps, schema_editor):
    Profile = apps.get_model("lexamora_studio", "AiModelProfile")
    for profile in Profile.objects.filter(model_id__in=[
        "veo-3.1-lite-generate-preview", "veo-3.1-fast-generate-preview",
    ]):
        defaults = dict(profile.defaults or {})
        defaults.update({
            "reference_modes": ["FRAMES", "INGREDIENTS"],
            "max_ingredient_references": 3,
            "durations": [4, 6, 8],
            "duration": 8,
        })
        fields = list(defaults.get("ui_fields") or [])
        for field in ["video-reference-mode", "video-duration"]:
            if field not in fields:
                fields.append(field)
        defaults["ui_fields"] = fields
        profile.defaults = defaults
        profile.save(update_fields=["defaults"])


class Migration(migrations.Migration):
    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("lexamora_studio", "0042_episode_consistency_review"),
    ]

    operations = [
        migrations.AddField(
            model_name="episodeconsistencyreview",
            name="score",
            field=models.PositiveSmallIntegerField(blank=True, null=True),
        ),
        migrations.CreateModel(
            name="MovieTimeline",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("title", models.CharField(default="Main edit", max_length=200)),
                ("aspect_ratio", models.CharField(default="16:9", max_length=12)),
                ("resolution", models.CharField(default="1920x1080", max_length=20)),
                ("fps", models.PositiveSmallIntegerField(default=25)),
                ("timeline", models.JSONField(blank=True, default=dict)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("created_by", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="created_movie_timelines", to=settings.AUTH_USER_MODEL)),
                ("project", models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name="movie_timeline", to="lexamora_studio.project")),
                ("updated_by", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="updated_movie_timelines", to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ["project_id"]},
        ),
        migrations.RunPython(update_video_capabilities, migrations.RunPython.noop),
    ]
