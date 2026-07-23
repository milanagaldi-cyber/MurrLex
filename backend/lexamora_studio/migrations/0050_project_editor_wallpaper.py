from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("lexamora_studio", "0049_movie_timeline_workspace_projects"),
    ]

    operations = [
        migrations.AddField(
            model_name="project",
            name="editor_wallpaper",
            field=models.ImageField(blank=True, upload_to="studio/editor-wallpapers/"),
        ),
    ]
