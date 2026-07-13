from django.db import migrations


def normalize_scene_numbers(apps, schema_editor):
    Episode = apps.get_model("lexamora_studio", "Episode")
    Scene = apps.get_model("lexamora_studio", "Scene")
    for episode_id in Episode.objects.values_list("id", flat=True).iterator():
        scene_ids = list(Scene.objects.filter(episode_id=episode_id, deleted_at__isnull=True).order_by("position", "number", "id").values_list("id", flat=True))
        for offset, scene_id in enumerate(scene_ids):
            Scene.objects.filter(id=scene_id).update(number=100000 + offset)
        for offset, scene_id in enumerate(scene_ids):
            Scene.objects.filter(id=scene_id).update(number=offset + 1, position=offset)


class Migration(migrations.Migration):
    dependencies = [("lexamora_studio", "0018_project_prompt_template")]
    operations = [migrations.RunPython(normalize_scene_numbers, migrations.RunPython.noop)]
