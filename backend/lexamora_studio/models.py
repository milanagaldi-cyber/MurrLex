import uuid

from django.conf import settings
from django.db import models


class ActiveManager(models.Manager):
    def get_queryset(self):
        return super().get_queryset().filter(deleted_at__isnull=True)


class SoftDeleteModel(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    created_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="%(app_label)s_%(class)s_created")
    updated_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="%(app_label)s_%(class)s_updated")
    deleted_at = models.DateTimeField(null=True, blank=True)
    deleted_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="%(app_label)s_%(class)s_deleted", null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    objects = ActiveManager()
    all_objects = models.Manager()

    class Meta:
        abstract = True


class Workspace(SoftDeleteModel):
    name = models.CharField(max_length=180)
    slug = models.SlugField(max_length=180, unique=True)
    description = models.TextField(blank=True)
    owner = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="owned_studio_workspaces")

    class Meta:
        ordering = ["name", "id"]

    def __str__(self):
        return self.name


class WorkspaceMembership(models.Model):
    class Role(models.TextChoices):
        OWNER = "OWNER", "Owner"
        ADMIN = "ADMIN", "Admin"
        EDITOR = "EDITOR", "Editor"
        TRANSLATOR = "TRANSLATOR", "Translator"
        VIEWER = "VIEWER", "Viewer"

    class Status(models.TextChoices):
        INVITED = "INVITED", "Invited"
        ACTIVE = "ACTIVE", "Active"
        SUSPENDED = "SUSPENDED", "Suspended"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.CASCADE, related_name="memberships")
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="studio_memberships")
    role = models.CharField(max_length=16, choices=Role.choices, default=Role.VIEWER)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.ACTIVE)
    can_use_ai = models.BooleanField(default=False)
    can_export = models.BooleanField(default=False)
    can_manage_members = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=["workspace", "user"], name="studio_unique_workspace_member")]


class Project(SoftDeleteModel):
    class Type(models.TextChoices):
        SERIES = "SERIES", "Series"
        STANDALONE_VIDEO = "STANDALONE_VIDEO", "Standalone video"

    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        IN_REVIEW = "IN_REVIEW", "In review"
        APPROVED = "APPROVED", "Approved"
        FINAL = "FINAL", "Final"

    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="projects")
    project_type = models.CharField(max_length=24, choices=Type.choices)
    title = models.CharField(max_length=240)
    concept = models.TextField(blank=True)
    original_language = models.CharField(max_length=16, blank=True)
    translation_languages = models.JSONField(default=list, blank=True)
    rights_holder = models.TextField(blank=True)
    publication_info = models.TextField(blank=True)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.DRAFT)

    class Meta:
        ordering = ["title", "id"]
        constraints = [models.UniqueConstraint(fields=["workspace", "title"], name="studio_unique_project_title")]

    def __str__(self):
        return self.title


class Character(SoftDeleteModel):
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="characters")
    name = models.CharField(max_length=180)
    description = models.TextField(blank=True)
    visual_description = models.TextField(blank=True)
    position = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["project", "name"], name="studio_unique_character_name")]


class Episode(SoftDeleteModel):
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="episodes")
    number = models.PositiveIntegerField(default=1)
    title = models.CharField(max_length=240)
    summary = models.TextField(blank=True)
    position = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["position", "number", "id"]
        constraints = [models.UniqueConstraint(fields=["project", "number"], name="studio_unique_episode_number")]


class Scene(SoftDeleteModel):
    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        IN_REVIEW = "IN_REVIEW", "In review"
        APPROVED = "APPROVED", "Approved"
        NEEDS_CHANGES = "NEEDS_CHANGES", "Needs changes"
        FINAL = "FINAL", "Final"

    episode = models.ForeignKey(Episode, on_delete=models.PROTECT, related_name="scenes")
    number = models.PositiveIntegerField(default=1)
    title = models.CharField(max_length=240)
    hook = models.TextField(blank=True)
    description = models.TextField(blank=True)
    location = models.TextField(blank=True)
    actions = models.TextField(blank=True)
    performance_notes = models.TextField(blank=True)
    position = models.PositiveIntegerField(default=0)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.DRAFT)

    class Meta:
        ordering = ["position", "number", "id"]
        constraints = [models.UniqueConstraint(fields=["episode", "number"], name="studio_unique_scene_number")]


class AiModelProfile(models.Model):
    class MediaType(models.TextChoices):
        IMAGE = "IMAGE", "Image"
        VIDEO = "VIDEO", "Video"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=120, unique=True)
    provider = models.CharField(max_length=80)
    model_id = models.CharField(max_length=160)
    media_type = models.CharField(max_length=16, choices=MediaType.choices)
    is_active = models.BooleanField(default=True)
    defaults = models.JSONField(default=dict, blank=True)

    class Meta:
        ordering = ["media_type", "name"]


class DialogueLine(SoftDeleteModel):
    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        IN_REVIEW = "IN_REVIEW", "In review"
        APPROVED = "APPROVED", "Approved"
        NEEDS_CHANGES = "NEEDS_CHANGES", "Needs changes"
        FINAL = "FINAL", "Final"

    scene = models.ForeignKey(Scene, on_delete=models.PROTECT, related_name="dialogue_lines")
    character = models.ForeignKey(Character, on_delete=models.SET_NULL, related_name="dialogue_lines", null=True, blank=True)
    speaker = models.CharField(max_length=180, blank=True)
    text = models.TextField()
    language = models.CharField(max_length=16, blank=True)
    delivery = models.TextField(blank=True)
    position = models.PositiveIntegerField(default=0)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.DRAFT)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["scene", "position"], name="studio_unique_dialogue_position")]


class Prompt(SoftDeleteModel):
    class Type(models.TextChoices):
        IMAGE = "IMAGE", "Image"
        VIDEO = "VIDEO", "Video"

    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        IN_REVIEW = "IN_REVIEW", "In review"
        APPROVED = "APPROVED", "Approved"
        NEEDS_CHANGES = "NEEDS_CHANGES", "Needs changes"
        FINAL = "FINAL", "Final"

    scene = models.ForeignKey(Scene, on_delete=models.PROTECT, related_name="prompts")
    ai_model = models.ForeignKey(AiModelProfile, on_delete=models.PROTECT, related_name="prompts")
    prompt_type = models.CharField(max_length=16, choices=Type.choices)
    title = models.CharField(max_length=180, blank=True)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.DRAFT)
    position = models.PositiveIntegerField(default=0)
    needs_review = models.BooleanField(default=False)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["scene", "ai_model", "prompt_type", "position"], name="studio_unique_prompt_position")]


class PromptBlock(SoftDeleteModel):
    class Type(models.TextChoices):
        NARRATIVE = "NARRATIVE", "Narrative"
        DIALOGUE_REFERENCE = "DIALOGUE_REFERENCE", "Dialogue reference"
        NEGATIVE = "NEGATIVE", "Negative"
        AUDIO = "AUDIO", "Audio"

    prompt = models.ForeignKey(Prompt, on_delete=models.PROTECT, related_name="blocks")
    block_type = models.CharField(max_length=24, choices=Type.choices)
    content = models.TextField()
    source_dialogue = models.ForeignKey(DialogueLine, on_delete=models.SET_NULL, related_name="prompt_blocks", null=True, blank=True)
    position = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["prompt", "position"], name="studio_unique_prompt_block_position")]
