import uuid

from django.conf import settings
from django.db import models


from .storage_backend import PrivateStudioStorage

private_storage = PrivateStudioStorage()


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
    avatar_asset = models.ForeignKey("Asset", on_delete=models.SET_NULL, related_name="workspace_avatar_for", null=True, blank=True)
    documentation_language = models.CharField(max_length=16, default="EN")
    dialogue_language = models.CharField(max_length=16, default="EN")
    prompt_language = models.CharField(max_length=16, default="EN")
    image_prompt_template = models.TextField(blank=True)
    video_prompt_template = models.TextField(blank=True)
    audio_prompt_template = models.TextField(blank=True)
    text_prompt_template = models.TextField(blank=True)
    purged_at = models.DateTimeField(null=True, blank=True)
    purged_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="purged_studio_workspaces", null=True, blank=True)

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


class EmailDeliveryLog(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.CASCADE, related_name="email_delivery_logs")
    recipient = models.EmailField()
    success = models.BooleanField(default=False)
    detail = models.TextField(blank=True)
    created_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_email_tests")
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]


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
    cover_asset = models.ForeignKey(
        "Asset",
        on_delete=models.SET_NULL,
        related_name="project_cover_for",
        null=True,
        blank=True,
    )
    project_type = models.CharField(max_length=24, choices=Type.choices)
    title = models.CharField(max_length=240)
    concept = models.TextField(blank=True)
    original_language = models.CharField(max_length=16, blank=True)
    translation_languages = models.JSONField(default=list, blank=True)
    prompt_template = models.TextField(blank=True)
    documentation_language = models.CharField(max_length=16, default="EN")
    dialogue_language = models.CharField(max_length=16, default="EN")
    prompt_language = models.CharField(max_length=16, default="EN")
    rights_holder = models.TextField(blank=True)
    publication_info = models.TextField(blank=True)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.DRAFT)
    purged_at = models.DateTimeField(null=True, blank=True)
    purged_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="purged_studio_projects", null=True, blank=True)

    class Meta:
        ordering = ["title", "id"]
        constraints = [models.UniqueConstraint(fields=["workspace", "title"], name="studio_unique_project_title")]

    def __str__(self):
        return self.title


class ProjectMembership(models.Model):
    class Role(models.TextChoices):
        VIEWER = "VIEWER", "View"
        EDITOR = "EDITOR", "Edit"
        CONTROLLER = "CONTROLLER", "Full control"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    project = models.ForeignKey(Project, on_delete=models.CASCADE, related_name="memberships")
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="studio_project_memberships")
    role = models.CharField(max_length=16, choices=Role.choices, default=Role.VIEWER)
    is_active = models.BooleanField(default=True)
    invited_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_project_invitations")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["user__email", "user__username", "id"]
        constraints = [
            models.UniqueConstraint(fields=["project", "user"], name="studio_unique_project_member"),
        ]

    def __str__(self):
        return f"{self.project}: {self.user} ({self.get_role_display()})"


class ProjectAccessExclusion(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    project = models.ForeignKey(Project, on_delete=models.CASCADE, related_name="access_exclusions")
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="studio_project_access_exclusions")
    revoked_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="revoked_studio_project_access")
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=["project", "user"], name="studio_unique_project_access_exclusion")]


class Character(SoftDeleteModel):
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="characters")
    name = models.CharField(max_length=180)
    description = models.TextField(blank=True)
    visual_description = models.TextField(blank=True)
    position = models.PositiveIntegerField(default=0)
    avatar_asset = models.ForeignKey("Asset", on_delete=models.SET_NULL, related_name="character_avatar_for", null=True, blank=True)
    reference_assets = models.ManyToManyField("Asset", related_name="referenced_by_characters", blank=True)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["project", "name"], name="studio_unique_character_name")]


class Episode(SoftDeleteModel):
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="episodes")
    number = models.PositiveIntegerField(default=1)
    title = models.CharField(max_length=240)
    summary = models.TextField(blank=True)
    position = models.PositiveIntegerField(default=0)
    language = models.CharField(max_length=16, default="EN")

    class Meta:
        ordering = ["position", "number", "id"]
        constraints = [models.UniqueConstraint(fields=["project", "number"], name="studio_unique_episode_number")]


class Scene(SoftDeleteModel):
    class Type(models.TextChoices):
        ORIGINAL = "ORIGINAL", "Original"
        ALTERNATIVE = "ALTERNATIVE", "Alternative"
        ADDITIONAL_GENERATION = "ADDITIONAL_GENERATION", "Additional generation"

    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        IN_REVIEW = "IN_REVIEW", "In review"
        APPROVED = "APPROVED", "Approved"
        NEEDS_CHANGES = "NEEDS_CHANGES", "Needs changes"
        PRODUCTION = "PRODUCTION", "Production"

    episode = models.ForeignKey(Episode, on_delete=models.PROTECT, related_name="scenes")
    number = models.PositiveIntegerField(default=1)
    title = models.CharField(max_length=240)
    hook = models.TextField(blank=True)
    description = models.TextField(blank=True)
    location = models.TextField(blank=True)
    actions = models.TextField(blank=True)
    performance_notes = models.TextField(blank=True)
    scene_type = models.CharField(max_length=24, choices=Type.choices, default=Type.ORIGINAL)
    position = models.PositiveIntegerField(default=0)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.DRAFT)
    reference_assets = models.ManyToManyField("Asset", related_name="referenced_by_scenes", blank=True)

    class Meta:
        ordering = ["position", "number", "id"]
        constraints = [models.UniqueConstraint(fields=["episode", "number"], name="studio_unique_scene_number")]


class AiModelProfile(models.Model):
    class MediaType(models.TextChoices):
        IMAGE = "IMAGE", "Photo"
        VIDEO = "VIDEO", "Video"
        AUDIO = "AUDIO", "Audio"
        TEXT = "TEXT", "Text"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=120, unique=True)
    provider = models.CharField(max_length=80)
    model_id = models.CharField(max_length=160)
    media_type = models.CharField(max_length=16, choices=MediaType.choices)
    is_active = models.BooleanField(default=True)
    defaults = models.JSONField(default=dict, blank=True)

    class Meta:
        ordering = ["media_type", "name"]


class StudioTextModel(models.Model):
    class Provider(models.TextChoices):
        OPENAI = "openai", "OpenAI"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=120)
    provider = models.CharField(max_length=32, choices=Provider.choices, default=Provider.OPENAI)
    model_id = models.CharField(max_length=160, unique=True)
    is_active = models.BooleanField(default=True)
    is_default = models.BooleanField(default=False)
    updated_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        related_name="updated_studio_text_models",
        null=True,
        blank=True,
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["name", "model_id"]

    def __str__(self):
        return self.name


class PromptTemplate(models.Model):
    class Scope(models.TextChoices):
        ALL = "ALL", "All prompt types"
        IMAGE = "IMAGE", "Photo"
        VIDEO = "VIDEO", "Video"
        AUDIO = "AUDIO", "Audio"
        TEXT = "TEXT", "Text"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=160, unique=True)
    content = models.TextField(blank=True)
    scope = models.CharField(max_length=16, choices=Scope.choices, default=Scope.ALL)
    is_active = models.BooleanField(default=True)
    is_default = models.BooleanField(default=False)
    updated_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        related_name="updated_prompt_templates",
        null=True,
        blank=True,
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["name", "id"]

    def __str__(self):
        return self.name


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
    class Language(models.TextChoices):
        BL = "BL", "BL"
        DE = "DE", "DE"
        EN = "EN", "EN"
        ES = "ES", "ES"
        PL = "PL", "PL"
        PT = "PT", "PT"
        RU = "RU", "RU"
        UA = "UA", "UA"

    class TranslationScope(models.TextChoices):
        ORIGINAL = "ORIGINAL", "Original"
        FULL = "FULL", "Full text"
        DIALOGUE = "DIALOGUE", "Dialogue only"
        SELECTED = "SELECTED", "Selected text"

    class Type(models.TextChoices):
        IMAGE = "IMAGE", "Photo"
        VIDEO = "VIDEO", "Video"
        AUDIO = "AUDIO", "Audio"
        TEXT = "TEXT", "Text"

    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        IN_REVIEW = "IN_REVIEW", "In review"
        APPROVED = "APPROVED", "Approved"
        NEEDS_CHANGES = "NEEDS_CHANGES", "Needs changes"
        FINAL = "FINAL", "Final"

    scene = models.ForeignKey(Scene, on_delete=models.PROTECT, related_name="prompts")
    ai_model = models.ForeignKey(AiModelProfile, on_delete=models.PROTECT, related_name="prompts")
    template = models.ForeignKey(PromptTemplate, on_delete=models.PROTECT, related_name="prompts")
    source_prompt = models.ForeignKey(
        "self", on_delete=models.PROTECT, related_name="translations", null=True, blank=True,
    )
    original_language = models.CharField(max_length=2, choices=Language.choices, default=Language.EN)
    language = models.CharField(max_length=2, choices=Language.choices, default=Language.EN)
    translation_scope = models.CharField(
        max_length=16, choices=TranslationScope.choices, default=TranslationScope.ORIGINAL,
    )
    content = models.TextField(blank=True)
    prompt_type = models.CharField(max_length=16, choices=Type.choices)
    title = models.CharField(max_length=180, blank=True)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.DRAFT)
    position = models.PositiveIntegerField(default=0)
    needs_review = models.BooleanField(default=False)
    reference_assets = models.ManyToManyField("Asset", related_name="referenced_by_prompts", blank=True)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["scene", "ai_model", "prompt_type", "position"], name="studio_unique_prompt_position")]

    def save(self, *args, **kwargs):
        if not self.template_id:
            from .ai_catalog import default_prompt_template

            self.template = default_prompt_template(self.prompt_type)
        if self._state.adding:
            if self.source_prompt_id:
                self.original_language = self.source_prompt.original_language
            else:
                project_language = (
                    self.scene.episode.project.prompt_language
                    or self.scene.episode.language
                    or self.scene.episode.project.original_language
                    or ""
                ).strip().upper()
                if project_language in self.Language.values:
                    self.original_language = project_language
                self.language = self.original_language
                project = self.scene.episode.project
                workspace = project.workspace
                typed_templates = {
                    self.Type.IMAGE: workspace.image_prompt_template,
                    self.Type.VIDEO: workspace.video_prompt_template,
                    self.Type.AUDIO: workspace.audio_prompt_template,
                    self.Type.TEXT: workspace.text_prompt_template,
                }
                project_template = (project.prompt_template or typed_templates.get(self.prompt_type, "") or "").strip()
                if project_template and not self.content.rstrip().endswith(project_template):
                    self.content = f"{self.content.rstrip()}\n\n{project_template}".strip()
        return super().save(*args, **kwargs)

    @property
    def editor_content(self):
        if self.content:
            return self.content
        return "\n\n".join(self.blocks.values_list("content", flat=True))

    def language_versions(self):
        root_id = self.source_prompt_id or self.id
        return Prompt.objects.filter(models.Q(id=root_id) | models.Q(source_prompt_id=root_id)).order_by("language", "created_at")


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
    translated_content = models.TextField(blank=True)
    translation_language = models.CharField(max_length=16, blank=True)
    translation_model = models.CharField(max_length=160, blank=True)
    position = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["prompt", "position"], name="studio_unique_prompt_block_position")]

def studio_asset_path(instance, filename):
    suffix = filename.rsplit(".", 1)[-1].lower() if "." in filename else "bin"
    return f"studio/{instance.workspace_id}/{instance.id}/original.{suffix}"


def studio_thumbnail_path(instance, filename):
    return f"studio/{instance.workspace_id}/{instance.id}/thumbnail.jpg"


class Asset(SoftDeleteModel):
    class Kind(models.TextChoices):
        CHARACTER_REFERENCE = "CHARACTER_REFERENCE", "Character reference"
        SCENE_IMAGE = "SCENE_IMAGE", "Scene image"
        GENERATION_OUTPUT = "GENERATION_OUTPUT", "Generation output"
        MONTAGE_SCREENSHOT = "MONTAGE_SCREENSHOT", "Montage screenshot"
        SOURCE_DOCUMENT = "SOURCE_DOCUMENT", "Source document"
        EXPORT = "EXPORT", "Export"
        OTHER = "OTHER", "Other"

    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="assets")
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="assets", null=True, blank=True)
    projects = models.ManyToManyField(Project, related_name="media_assets", blank=True)
    scene = models.ForeignKey(Scene, on_delete=models.PROTECT, related_name="assets", null=True, blank=True)
    character = models.ForeignKey(Character, on_delete=models.PROTECT, related_name="assets", null=True, blank=True)
    prompt = models.ForeignKey(Prompt, on_delete=models.PROTECT, related_name="assets", null=True, blank=True)
    kind = models.CharField(max_length=32, choices=Kind.choices)
    file = models.FileField(storage=private_storage, upload_to=studio_asset_path, max_length=500)
    thumbnail = models.FileField(storage=private_storage, upload_to=studio_thumbnail_path, max_length=500, blank=True)
    original_filename = models.CharField(max_length=255)
    content_type = models.CharField(max_length=120)
    size_bytes = models.PositiveBigIntegerField()
    checksum_sha256 = models.CharField(max_length=64)
    width = models.PositiveIntegerField(null=True, blank=True)
    height = models.PositiveIntegerField(null=True, blank=True)
    purged_at = models.DateTimeField(null=True, blank=True)
    purged_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="purged_studio_assets",
        null=True,
        blank=True,
    )

    class Meta:
        ordering = ["-created_at", "id"]


class AdditionalGeneration(SoftDeleteModel):
    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        IN_REVIEW = "IN_REVIEW", "In review"
        APPROVED = "APPROVED", "Approved"
        NEEDS_CHANGES = "NEEDS_CHANGES", "Needs changes"
        FINAL = "FINAL", "Final"

    scene = models.ForeignKey(Scene, on_delete=models.PROTECT, related_name="additional_generations")
    reason = models.TextField()
    source_asset = models.ForeignKey(Asset, on_delete=models.PROTECT, related_name="source_generations", null=True, blank=True)
    prompt = models.TextField()
    position = models.PositiveIntegerField(default=0)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.DRAFT)

    class Meta:
        ordering = ["position", "id"]


class GenerationOutput(SoftDeleteModel):
    generation = models.ForeignKey(AdditionalGeneration, on_delete=models.PROTECT, related_name="outputs")
    asset = models.ForeignKey(Asset, on_delete=models.PROTECT, related_name="generation_outputs")
    model_metadata = models.JSONField(default=dict, blank=True)
    position = models.PositiveIntegerField(default=0)
    is_final = models.BooleanField(default=False)

    class Meta:
        ordering = ["position", "id"]
        constraints = [
            models.UniqueConstraint(fields=["generation"], condition=models.Q(is_final=True), name="studio_one_final_generation_output")
        ]


class Revision(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="revisions")
    entity_type = models.CharField(max_length=120)
    entity_id = models.UUIDField()
    sequence = models.PositiveIntegerField()
    operation = models.CharField(max_length=32)
    snapshot = models.JSONField(default=dict)
    changed_fields = models.JSONField(default=list)
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_revisions")
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at", "-sequence"]
        constraints = [models.UniqueConstraint(fields=["entity_type", "entity_id", "sequence"], name="studio_unique_revision_sequence")]

    def save(self, *args, **kwargs):
        if self.pk and Revision.objects.filter(pk=self.pk).exists():
            raise ValueError("Revisions are append-only.")
        return super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        raise ValueError("Revisions are append-only.")


class AuditEvent(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="audit_events")
    actor = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_audit_events")
    action = models.CharField(max_length=80)
    entity_type = models.CharField(max_length=120, blank=True)
    entity_id = models.UUIDField(null=True, blank=True)
    request_id = models.CharField(max_length=80, blank=True)
    metadata = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at", "id"]

    def save(self, *args, **kwargs):
        if self.pk and AuditEvent.objects.filter(pk=self.pk).exists():
            raise ValueError("Audit events are append-only.")
        return super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        raise ValueError("Audit events are append-only.")


class AccessEvent(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="access_events")
    actor = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_access_events")
    asset = models.ForeignKey(Asset, on_delete=models.PROTECT, related_name="access_events")
    action = models.CharField(max_length=24)
    request_id = models.CharField(max_length=80, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at", "id"]


class AiSuggestion(models.Model):
    class Status(models.TextChoices):
        PENDING = "PENDING", "Pending"
        ACCEPTED = "ACCEPTED", "Accepted"
        REJECTED = "REJECTED", "Rejected"
        UNDONE = "UNDONE", "Undone"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="ai_suggestions")
    prompt = models.ForeignKey(Prompt, on_delete=models.PROTECT, related_name="ai_suggestions")
    source_revision = models.ForeignKey(Revision, on_delete=models.PROTECT, related_name="ai_suggestions")
    mode = models.CharField(max_length=40)
    selected_block_ids = models.JSONField(default=list)
    original_blocks = models.JSONField(default=list)
    suggested_blocks = models.JSONField(default=list)
    raw_response = models.TextField(blank=True)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.PENDING)
    provider = models.CharField(max_length=40, default="openai")
    model = models.CharField(max_length=80)
    created_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_ai_suggestions")
    created_at = models.DateTimeField(auto_now_add=True)
    decided_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_ai_decisions", null=True, blank=True)
    decided_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at", "id"]


class AiUsageLog(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="ai_usage")
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_ai_usage")
    prompt = models.ForeignKey(Prompt, on_delete=models.PROTECT, related_name="ai_usage")
    action = models.CharField(max_length=40)
    model = models.CharField(max_length=80)
    status = models.CharField(max_length=20)
    input_chars = models.PositiveIntegerField(default=0)
    output_chars = models.PositiveIntegerField(default=0)
    error_code = models.CharField(max_length=80, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at", "id"]


class SubtitleTrack(SoftDeleteModel):
    class Kind(models.TextChoices):
        WORKING = "WORKING", "Working"
        FINAL = "FINAL", "Final"

    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        IN_REVIEW = "IN_REVIEW", "In review"
        APPROVED = "APPROVED", "Approved"
        FINAL = "FINAL", "Final"

    episode = models.ForeignKey(Episode, on_delete=models.PROTECT, related_name="subtitle_tracks")
    language = models.CharField(max_length=16)
    kind = models.CharField(max_length=16, choices=Kind.choices, default=Kind.WORKING)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.DRAFT)

    class Meta:
        ordering = ["episode", "language", "kind"]
        constraints = [models.UniqueConstraint(fields=["episode", "language", "kind"], name="studio_unique_subtitle_track")]


class SubtitleLine(SoftDeleteModel):
    track = models.ForeignKey(SubtitleTrack, on_delete=models.PROTECT, related_name="lines")
    position = models.PositiveIntegerField(default=0)
    text = models.TextField()
    start_ms = models.PositiveIntegerField(null=True, blank=True)
    end_ms = models.PositiveIntegerField(null=True, blank=True)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["track", "position"], name="studio_unique_subtitle_position")]


class TranslationUnit(SoftDeleteModel):
    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        IN_REVIEW = "IN_REVIEW", "In review"
        APPROVED = "APPROVED", "Approved"
        STALE = "STALE", "Stale"

    dialogue_line = models.ForeignKey(DialogueLine, on_delete=models.PROTECT, related_name="translations")
    source_revision = models.ForeignKey(Revision, on_delete=models.PROTECT, related_name="translations", null=True, blank=True)
    target_language = models.CharField(max_length=16)
    source_text = models.TextField()
    translated_text = models.TextField(blank=True)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.DRAFT)

    class Meta:
        ordering = ["dialogue_line__scene__position", "dialogue_line__position", "target_language"]
        constraints = [models.UniqueConstraint(fields=["dialogue_line", "target_language"], name="studio_unique_dialogue_translation")]


class ExportJob(models.Model):
    class Status(models.TextChoices):
        PENDING = "PENDING", "Pending"
        RUNNING = "RUNNING", "Running"
        SUCCESS = "SUCCESS", "Success"
        ERROR = "ERROR", "Error"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="export_jobs")
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="export_jobs")
    episode = models.ForeignKey(Episode, on_delete=models.PROTECT, related_name="export_jobs", null=True, blank=True)
    sections = models.JSONField(default=list)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.PENDING)
    output_asset = models.ForeignKey(Asset, on_delete=models.PROTECT, related_name="export_jobs", null=True, blank=True)
    error_message = models.TextField(blank=True)
    requested_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_export_jobs")
    created_at = models.DateTimeField(auto_now_add=True)
    started_at = models.DateTimeField(null=True, blank=True)
    completed_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at", "id"]

class DocxImport(models.Model):
    class Status(models.TextChoices):
        PREVIEW = "PREVIEW", "Preview"
        ACCEPTED = "ACCEPTED", "Accepted"
        ERROR = "ERROR", "Error"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="docx_imports")
    source_asset = models.ForeignKey(Asset, on_delete=models.PROTECT, related_name="docx_imports")
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="source_imports", null=True, blank=True)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.PREVIEW)
    parsed_data = models.JSONField(default=dict)
    warnings = models.JSONField(default=list)
    requested_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_docx_imports")
    accepted_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_accepted_docx_imports", null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    accepted_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        ordering = ["-created_at", "id"]
