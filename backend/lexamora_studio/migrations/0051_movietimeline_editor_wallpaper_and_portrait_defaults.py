from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("lexamora_studio", "0050_project_editor_wallpaper"),
    ]

    operations = [
        migrations.AddField(
            model_name="movietimeline",
            name="editor_wallpaper",
            field=models.ImageField(
                blank=True,
                upload_to="studio/movie-edit-wallpapers/",
            ),
        ),
        migrations.AlterField(
            model_name="movietimeline",
            name="aspect_ratio",
            field=models.CharField(default="9:16", max_length=12),
        ),
        migrations.AlterField(
            model_name="movietimeline",
            name="resolution",
            field=models.CharField(default="1080x1920", max_length=20),
        ),
    ]
