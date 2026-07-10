import uuid

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("lessons", "0002_lesson_lesson_info"),
    ]

    operations = [
        migrations.CreateModel(
            name="ApiSession",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("public_id", models.UUIDField(default=uuid.uuid4, editable=False, unique=True)),
                ("refresh_token_hash", models.CharField(max_length=64, unique=True)),
                ("device_name", models.CharField(blank=True, max_length=160)),
                ("expires_at", models.DateTimeField()),
                ("revoked_at", models.DateTimeField(blank=True, null=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("last_used_at", models.DateTimeField(auto_now=True)),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="api_sessions", to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ["-last_used_at", "-id"]},
        ),
    ]
