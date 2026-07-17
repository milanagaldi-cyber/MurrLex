from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0036_project_image_generation_jobs")]

    operations = [
        migrations.AddField(
            model_name="asset",
            name="is_starred",
            field=models.BooleanField(default=False),
        ),
    ]
