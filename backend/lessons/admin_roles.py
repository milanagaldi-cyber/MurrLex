from django.contrib.auth.models import Group, Permission
from django.db.models import Q, QuerySet


DAILY_ADMIN_GROUP = "Daily Admin"
ROLE_NAMES = (
    DAILY_ADMIN_GROUP,
    "Support",
    "Billing",
    "Content",
    "ReadOnly",
    "Developer",
    "Superadmin",
)


DAILY_ADMIN_PERMISSIONS = {
    "auth.user": {"add", "change", "view"},
    "lessons.apisession": {"change", "delete", "view"},
    "lessons.card": {"add", "change", "delete", "view"},
    "lessons.googleoauthalloweduser": {"add", "change", "delete", "view"},
    "lessons.importlog": {"view"},
    "lessons.lesson": {"add", "change", "delete", "view"},
    "lessons.userapiaccess": {"change", "view"},
}

SUPPORT_PERMISSIONS = {
    "auth.user": {"add", "change", "view"},
    "lessons.apisession": {"change", "delete", "view"},
    "lessons.googleoauthalloweduser": {"add", "change", "delete", "view"},
    "lessons.userapiaccess": {"change", "view"},
}

BILLING_PERMISSIONS = {
    "auth.user": {"view"},
    "lessons.userapiaccess": {"change", "view"},
    "lexamora_studio.aiusagelog": {"view"},
}

CONTENT_PERMISSIONS = {
    "lessons.card": {"add", "change", "delete", "view"},
    "lessons.importlog": {"view"},
    "lessons.lesson": {"add", "change", "delete", "view"},
}

CONTENT_STUDIO_MODELS = {
    "asset",
    "character",
    "dialogueline",
    "docximport",
    "episode",
    "exportjob",
    "generationoutput",
    "project",
    "prompt",
    "promptblock",
    "prompttemplate",
    "scene",
    "subtitleline",
    "subtitletrack",
    "translationunit",
    "workspace",
}


def _explicit_permissions(permission_map: dict[str, set[str]]) -> QuerySet[Permission]:
    query = Q(pk__in=[])
    for model_label, actions in permission_map.items():
        app_label, model = model_label.split(".", 1)
        query |= Q(
            content_type__app_label=app_label,
            content_type__model=model,
            codename__in=[f"{action}_{model}" for action in actions],
        )
    return Permission.objects.filter(query)


def permissions_for_role(role_name: str) -> QuerySet[Permission]:
    if role_name == DAILY_ADMIN_GROUP:
        return _explicit_permissions(DAILY_ADMIN_PERMISSIONS)
    if role_name == "Support":
        return _explicit_permissions(SUPPORT_PERMISSIONS)
    if role_name == "Billing":
        return _explicit_permissions(BILLING_PERMISSIONS)
    if role_name == "Content":
        lesson_permissions = _explicit_permissions(CONTENT_PERMISSIONS)
        studio_permissions = Permission.objects.filter(
            content_type__app_label="lexamora_studio",
            content_type__model__in=CONTENT_STUDIO_MODELS,
        )
        return Permission.objects.filter(
            Q(pk__in=lesson_permissions.values("pk"))
            | Q(pk__in=studio_permissions.values("pk"))
        )
    if role_name == "ReadOnly":
        return Permission.objects.filter(
            content_type__app_label__in=("auth", "lessons", "lexamora_studio"),
            codename__startswith="view_",
        ).exclude(
            Q(content_type__app_label="auth", content_type__model="group")
            | Q(content_type__app_label="lessons", content_type__model="providercredential")
        )
    if role_name == "Developer":
        return Permission.objects.filter(
            content_type__app_label__in=("auth", "lessons", "lexamora_studio"),
            codename__startswith="view_",
        ).exclude(content_type__app_label="auth", content_type__model="group")
    if role_name == "Superadmin":
        return Permission.objects.all()
    return Permission.objects.none()


def sync_admin_groups() -> dict[str, Group]:
    groups = {}
    for role_name in ROLE_NAMES:
        group, _ = Group.objects.get_or_create(name=role_name)
        group.permissions.set(permissions_for_role(role_name))
        groups[role_name] = group
    return groups


def sync_daily_admin_group() -> Group:
    return sync_admin_groups()[DAILY_ADMIN_GROUP]
