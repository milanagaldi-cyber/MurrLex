import uuid

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
import lexamora_studio.movie_timeline


def upgrade_existing_timelines(apps, schema_editor):
    MovieTimeline = apps.get_model("lexamora_studio", "MovieTimeline")
    for timeline in MovieTimeline.objects.all().iterator():
        value = timeline.timeline if isinstance(timeline.timeline, dict) else {}
        if not isinstance(value.get("tracks"), list):
            value["tracks"] = []
        value["schemaVersion"] = 1
        timeline.timeline = value
        timeline.schema_version = 1
        timeline.save(update_fields=["timeline", "schema_version"])


class Migration(migrations.Migration):
    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("lexamora_studio", "0043_movie_timeline_and_video_reference_modes"),
    ]

    operations = [
        migrations.AddField(
            model_name="movietimeline",
            name="schema_version",
            field=models.PositiveSmallIntegerField(default=1),
        ),
        migrations.AlterField(
            model_name="movietimeline",
            name="timeline",
            field=models.JSONField(blank=True, default=lexamora_studio.movie_timeline.default_movie_timeline),
        ),
        migrations.CreateModel(
            name="MovieTimelineRevision",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("title", models.CharField(max_length=200)),
                ("aspect_ratio", models.CharField(max_length=12)),
                ("resolution", models.CharField(max_length=20)),
                ("fps", models.PositiveSmallIntegerField()),
                ("schema_version", models.PositiveSmallIntegerField(default=1)),
                ("snapshot", models.JSONField(default=dict)),
                ("reason", models.CharField(choices=[("SAVE", "Save"), ("RESTORE", "Restore")], default="SAVE", max_length=16)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("created_by", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="movie_timeline_revisions", to=settings.AUTH_USER_MODEL)),
                ("timeline", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="revisions", to="lexamora_studio.movietimeline")),
            ],
            options={"ordering": ["-created_at", "-id"]},
        ),
        migrations.AddIndex(
            model_name="movietimelinerevision",
            index=models.Index(fields=["timeline", "-created_at"], name="studio_movie_rev_time"),
        ),
        migrations.RunPython(upgrade_existing_timelines, migrations.RunPython.noop),
    ]
