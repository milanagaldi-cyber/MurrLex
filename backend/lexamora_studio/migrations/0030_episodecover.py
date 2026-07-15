from django.db import migrations, models
import django.db.models.deletion
import uuid
from django.conf import settings


def copy_existing_covers(apps, schema_editor):
    Episode = apps.get_model("lexamora_studio", "Episode")
    EpisodeCover = apps.get_model("lexamora_studio", "EpisodeCover")
    for episode in Episode.objects.prefetch_related("cover_assets").all():
        for asset in episode.cover_assets.all():
            EpisodeCover.objects.get_or_create(
                episode_id=episode.id,
                asset_id=asset.id,
                defaults={"language_code": episode.language or "EN", "platform": "OTHER"},
                created_by_id=episode.created_by_id,
                updated_by_id=episode.updated_by_id,
            )


class Migration(migrations.Migration):
    dependencies = [
        ("lexamora_studio", "0029_episode_avatar_asset_episode_cover_assets"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="EpisodeCover",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("deleted_at", models.DateTimeField(blank=True, null=True)),
                ("language_code", models.CharField(default="EN", max_length=16)),
                ("platform", models.CharField(choices=[("TIKTOK", "TikTok"), ("YOUTUBE", "YouTube"), ("INSTAGRAM", "Insta"), ("FACEBOOK", "Facebook"), ("OTHER", "Others")], default="OTHER", max_length=16)),
                ("custom_platform", models.CharField(blank=True, max_length=120)),
                ("asset", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="episode_cover_entries", to="lexamora_studio.asset")),
                ("created_by", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="%(app_label)s_%(class)s_created", to=settings.AUTH_USER_MODEL)),
                ("deleted_by", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.PROTECT, related_name="%(app_label)s_%(class)s_deleted", to=settings.AUTH_USER_MODEL)),
                ("episode", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="cover_entries", to="lexamora_studio.episode")),
                ("updated_by", models.ForeignKey(on_delete=django.db.models.deletion.PROTECT, related_name="%(app_label)s_%(class)s_updated", to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ["created_at", "id"]},
        ),
        migrations.AddConstraint(
            model_name="episodecover",
            constraint=models.UniqueConstraint(fields=("episode", "asset"), name="studio_unique_episode_cover_asset"),
        ),
        migrations.RunPython(copy_existing_covers, migrations.RunPython.noop),
    ]
