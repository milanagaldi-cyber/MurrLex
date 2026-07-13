from django.contrib.auth.models import Group, Permission


DAILY_ADMIN_GROUP = "Daily Admin"

# Daily administrators can handle routine user and content work. Server secrets,
# role assignment, groups, and system configuration remain superuser-only.
DAILY_ADMIN_PERMISSIONS = {
    "auth.user": {"add", "change", "view"},
    "lessons.apisession": {"change", "delete", "view"},
    "lessons.card": {"add", "change", "delete", "view"},
    "lessons.googleoauthalloweduser": {"add", "change", "delete", "view"},
    "lessons.importlog": {"view"},
    "lessons.lesson": {"add", "change", "delete", "view"},
    "lessons.userapiaccess": {"change", "view"},
}


def sync_daily_admin_group() -> Group:
    group, _ = Group.objects.get_or_create(name=DAILY_ADMIN_GROUP)
    permission_ids = []

    for model_label, actions in DAILY_ADMIN_PERMISSIONS.items():
        app_label, model = model_label.split(".", 1)
        codenames = [f"{action}_{model}" for action in actions]
        permission_ids.extend(
            Permission.objects.filter(
                content_type__app_label=app_label,
                content_type__model=model,
                codename__in=codenames,
            ).values_list("pk", flat=True)
        )

    group.permissions.set(permission_ids)
    return group
