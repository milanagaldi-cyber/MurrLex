from django.db import migrations, models
import django.db.models.deletion


def populate_movie_workspaces(apps, schema_editor):
    MovieTimeline = apps.get_model("lexamora_studio", "MovieTimeline")
    for timeline in MovieTimeline.objects.select_related("project").all():
        timeline.workspace_id = timeline.project.workspace_id
        timeline.save(update_fields=["workspace"])
        timeline.projects.add(timeline.project_id)


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0048_asset_filmstrip_file")]

    operations = [
        migrations.AddField(
            model_name="movietimeline",
            name="workspace",
            field=models.ForeignKey(
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="movie_timelines",
                to="lexamora_studio.workspace",
            ),
        ),
        migrations.AddField(
            model_name="movietimeline",
            name="projects",
            field=models.ManyToManyField(blank=True, related_name="movie_timelines", to="lexamora_studio.project"),
        ),
        migrations.AddField(
            model_name="movietimeline",
            name="is_archived",
            field=models.BooleanField(default=False),
        ),
        migrations.AlterField(
            model_name="movietimeline",
            name="project",
            field=models.ForeignKey(
                on_delete=django.db.models.deletion.PROTECT,
                related_name="owned_movie_timelines",
                to="lexamora_studio.project",
            ),
        ),
        migrations.RunPython(populate_movie_workspaces, migrations.RunPython.noop),
        migrations.AlterField(
            model_name="movietimeline",
            name="workspace",
            field=models.ForeignKey(
                on_delete=django.db.models.deletion.CASCADE,
                related_name="movie_timelines",
                to="lexamora_studio.workspace",
            ),
        ),
        migrations.AlterModelOptions(
            name="movietimeline",
            options={"ordering": ["workspace_id", "title", "id"]},
        ),
    ]
