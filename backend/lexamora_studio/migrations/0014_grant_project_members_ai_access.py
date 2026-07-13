from django.db import migrations


def grant_existing_members(apps, schema_editor):
    ProjectMembership = apps.get_model("lexamora_studio", "ProjectMembership")
    UserApiAccess = apps.get_model("lessons", "UserApiAccess")
    user_ids = ProjectMembership.objects.filter(is_active=True).values_list("user_id", flat=True).distinct()
    for user_id in user_ids.iterator():
        UserApiAccess.objects.update_or_create(
            user_id=user_id,
            defaults={"ai_api_enabled": True},
        )


class Migration(migrations.Migration):
    dependencies = [
        ("lessons", "0008_googleoauthalloweduser_google_oauth_unique_allowed_email_and_more"),
        ("lexamora_studio", "0013_projectmembership"),
    ]

    operations = [migrations.RunPython(grant_existing_members, migrations.RunPython.noop)]
