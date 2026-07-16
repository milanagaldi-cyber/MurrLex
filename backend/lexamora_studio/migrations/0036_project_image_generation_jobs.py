from django.db import migrations, models
import django.db.models.deletion


def populate_project(apps, schema_editor):
    ImageGenerationJob = apps.get_model("lexamora_studio", "ImageGenerationJob")
    for job in ImageGenerationJob.objects.select_related("prompt__scene__episode").iterator():
        if job.prompt_id:
            job.project_id = job.prompt.scene.episode.project_id
            job.save(update_fields=["project"])


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0035_image_generation_jobs")]

    operations = [
        migrations.AddField(
            model_name="imagegenerationjob",
            name="project",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.PROTECT,
                related_name="image_generation_jobs",
                to="lexamora_studio.project",
            ),
        ),
        migrations.AlterField(
            model_name="imagegenerationjob",
            name="prompt",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.PROTECT,
                related_name="image_generation_jobs",
                to="lexamora_studio.prompt",
            ),
        ),
        migrations.RunPython(populate_project, migrations.RunPython.noop),
    ]
