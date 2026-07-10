from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


def create_existing_user_access(apps, schema_editor):
    user_model = apps.get_model(*settings.AUTH_USER_MODEL.split("."))
    access_model = apps.get_model("lessons", "UserApiAccess")
    access_model.objects.bulk_create(
        [access_model(user_id=user_id) for user_id in user_model.objects.values_list("id", flat=True)],
        ignore_conflicts=True,
    )


class Migration(migrations.Migration):
    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("lessons", "0004_providercredential"),
    ]

    operations = [
        migrations.CreateModel(
            name="UserApiAccess",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("ai_api_enabled", models.BooleanField(default=False, verbose_name="AI API access")),
                ("updated_at", models.DateTimeField(auto_now=True)),
                (
                    "user",
                    models.OneToOneField(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="api_access",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "verbose_name": "API access",
                "verbose_name_plural": "API access",
                "ordering": ["user__username"],
            },
        ),
        migrations.RunPython(create_existing_user_access, migrations.RunPython.noop),
    ]
