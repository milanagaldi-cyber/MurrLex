import uuid

from django.conf import settings
from django.db import models


from .storage_backend import PrivateStudioStorage
from .movie_timeline import MOVIE_TIMELINE_SCHEMA_VERSION, default_movie_timeline

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
    default_image_model = models.ForeignKey(
        "AiModelProfile",
        on_delete=models.SET_NULL,
        related_name="default_for_workspaces",
        null=True,
        blank=True,
    )
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
    editor_wallpaper = models.ImageField(
        upload_to="studio/editor-wallpapers/",
        blank=True,
    )
    project_type = models.CharField(max_length=24, choices=Type.choices)
    title = models.CharField(max_length=240)
    description = models.TextField(blank=True)
    concept = models.TextField(blank=True)
    original_language = models.CharField(max_length=16, blank=True)
    translation_languages = models.JSONField(default=list, blank=True)
    prompt_template = models.TextField(blank=True)
    documentation_language = models.CharField(max_length=16, default="EN")
    dialogue_language = models.CharField(max_length=16, default="EN")
    prompt_language = models.CharField(max_length=16, default="EN")
    default_translation_model = models.ForeignKey(
        "StudioTextModel",
        on_delete=models.SET_NULL,
        related_name="default_for_projects",
        null=True,
        blank=True,
    )
    hidden_sections = models.JSONField(default=list, blank=True)
    rights_holder = models.TextField(blank=True)
    publication_info = models.TextField(blank=True)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.DRAFT)
    status_comment = models.TextField(blank=True)
    purged_at = models.DateTimeField(null=True, blank=True)
    purged_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="purged_studio_projects", null=True, blank=True)

    class Meta:
        ordering = ["title", "id"]
        constraints = [models.UniqueConstraint(fields=["workspace", "title"], name="studio_unique_project_title")]

    def __str__(self):
        return self.title


class RecommendedTrack(SoftDeleteModel):
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="recommended_tracks")
    is_primary = models.BooleanField(default=False)
    platform = models.CharField(max_length=80, blank=True)
    artist = models.CharField(max_length=180)
    title = models.CharField(max_length=240)
    url = models.URLField(max_length=500, blank=True)
    position = models.PositiveIntegerField(default=0)

    class Meta:
        ordering = ["position", "id"]

    def __str__(self):
        return f"{self.artist} - {self.title}"


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
    name_prompt = models.CharField(max_length=180, blank=True)
    name_dialogue = models.CharField(max_length=180, blank=True)
    description = models.TextField(blank=True)
    description_prompt = models.TextField(blank=True)
    description_dialogue = models.TextField(blank=True)
    visual_description = models.TextField(blank=True)
    visual_description_prompt = models.TextField(blank=True)
    visual_description_dialogue = models.TextField(blank=True)
    position = models.PositiveIntegerField(default=0)
    avatar_asset = models.ForeignKey("Asset", on_delete=models.SET_NULL, related_name="character_avatar_for", null=True, blank=True)
    reference_assets = models.ManyToManyField("Asset", related_name="referenced_by_characters", blank=True)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["project", "name"], name="studio_unique_character_name")]


class Episode(SoftDeleteModel):
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="episodes")
    avatar_asset = models.ForeignKey(
        "Asset", on_delete=models.SET_NULL, related_name="episode_avatar_for", null=True, blank=True,
    )
    cover_assets = models.ManyToManyField("Asset", related_name="cover_for_episodes", blank=True)
    number = models.PositiveIntegerField(default=1)
    title = models.CharField(max_length=240)
    summary = models.TextField(blank=True)
    position = models.PositiveIntegerField(default=0)
    language = models.CharField(max_length=16, default="EN")

    class Meta:
        ordering = ["position", "number", "id"]
        constraints = [models.UniqueConstraint(fields=["project", "number"], name="studio_unique_episode_number")]


class EpisodeCover(SoftDeleteModel):
    class Platform(models.TextChoices):
        TIKTOK = "TIKTOK", "TikTok"
        YOUTUBE = "YOUTUBE", "YouTube"
        INSTAGRAM = "INSTAGRAM", "Insta"
        FACEBOOK = "FACEBOOK", "Facebook"
        OTHER = "OTHER", "Others"

    episode = models.ForeignKey(Episode, on_delete=models.CASCADE, related_name="cover_entries")
    asset = models.ForeignKey("Asset", on_delete=models.CASCADE, related_name="episode_cover_entries")
    language_code = models.CharField(max_length=16, default="EN")
    platform = models.CharField(max_length=16, choices=Platform.choices, default=Platform.OTHER)
    custom_platform = models.CharField(max_length=120, blank=True)

    class Meta:
        ordering = ["created_at", "id"]
        constraints = [
            models.UniqueConstraint(fields=["episode", "asset"], name="studio_unique_episode_cover_asset"),
        ]


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
    title_prompt = models.CharField(max_length=240, blank=True)
    title_dialogue = models.CharField(max_length=240, blank=True)
    hook = models.TextField(blank=True)
    hook_prompt = models.TextField(blank=True)
    hook_dialogue = models.TextField(blank=True)
    description = models.TextField(blank=True)
    description_prompt = models.TextField(blank=True)
    description_dialogue = models.TextField(blank=True)
    location = models.TextField(blank=True)
    location_prompt = models.TextField(blank=True)
    location_dialogue = models.TextField(blank=True)
    actions = models.TextField(blank=True)
    actions_prompt = models.TextField(blank=True)
    actions_dialogue = models.TextField(blank=True)
    performance_notes = models.TextField(blank=True)
    performance_notes_prompt = models.TextField(blank=True)
    performance_notes_dialogue = models.TextField(blank=True)
    scene_type = models.CharField(max_length=24, choices=Type.choices, default=Type.ORIGINAL)
    position = models.PositiveIntegerField(default=0)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.DRAFT)
    status_comment = models.TextField(blank=True)
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

    def __str__(self):
        return f"{self.name} ({self.model_id})"


class StudioTextModel(models.Model):
    class Provider(models.TextChoices):
        OPENAI = "openai", "OpenAI"
        GOOGLE = "google", "Google AI Studio"

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

class StudioUserPreference(models.Model):
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="studio_preferences",
    )
    speech_language = models.CharField(max_length=16, default="en-US")
    translation_language = models.CharField(max_length=16, default="EN")
    speech_continuous = models.BooleanField(default=False)
    speech_interim = models.BooleanField(default=True)
    ai_enabled = models.BooleanField(default=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"Studio preferences for {self.user}"



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
    speaker_documentation = models.CharField(max_length=180, blank=True)
    speaker_prompt = models.CharField(max_length=180, blank=True)
    text = models.TextField()
    text_documentation = models.TextField(blank=True)
    text_prompt = models.TextField(blank=True)
    language = models.CharField(max_length=16, blank=True)
    delivery = models.TextField(blank=True)
    delivery_documentation = models.TextField(blank=True)
    delivery_prompt = models.TextField(blank=True)
    position = models.PositiveIntegerField(default=0)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.DRAFT)
    status_comment = models.TextField(blank=True)

    class Meta:
        ordering = ["position", "id"]
        constraints = [models.UniqueConstraint(fields=["scene", "position"], name="studio_unique_dialogue_position")]


class Prompt(SoftDeleteModel):
    class Language(models.TextChoices):
        BY = "BY", "BY"
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
    status_comment = models.TextField(blank=True)
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

    @property
    def generation_reference_count(self):
        return self.reference_assets.filter(content_type__startswith="image/").exclude(
            kind=Asset.Kind.GENERATION_OUTPUT,
        ).count()

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


def studio_proxy_path(instance, filename):
    suffix = filename.rsplit(".", 1)[-1].lower() if "." in filename else "bin"
    return f"studio/{instance.workspace_id}/{instance.id}/proxy.{suffix}"


def studio_waveform_path(instance, filename):
    return f"studio/{instance.workspace_id}/{instance.id}/waveform.png"


def studio_filmstrip_path(instance, filename):
    return f"studio/{instance.workspace_id}/{instance.id}/filmstrip.jpg"


class Asset(SoftDeleteModel):
    class Kind(models.TextChoices):
        CHARACTER_REFERENCE = "CHARACTER_REFERENCE", "Character reference"
        SCENE_IMAGE = "SCENE_IMAGE", "Scene image"
        GENERATION_OUTPUT = "GENERATION_OUTPUT", "Generation output"
        MONTAGE_SCREENSHOT = "MONTAGE_SCREENSHOT", "Montage screenshot"
        SOURCE_DOCUMENT = "SOURCE_DOCUMENT", "Source document"
        EXPORT = "EXPORT", "Export"
        OTHER = "OTHER", "Other"

    class ProcessingStatus(models.TextChoices):
        NOT_REQUIRED = "NOT_REQUIRED", "Not required"
        QUEUED = "QUEUED", "Queued"
        PROCESSING = "PROCESSING", "Processing"
        READY = "READY", "Ready"
        FAILED = "FAILED", "Failed"

    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="assets")
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="assets", null=True, blank=True)
    projects = models.ManyToManyField(Project, related_name="media_assets", blank=True)
    scene = models.ForeignKey(Scene, on_delete=models.PROTECT, related_name="assets", null=True, blank=True)
    character = models.ForeignKey(Character, on_delete=models.PROTECT, related_name="assets", null=True, blank=True)
    prompt = models.ForeignKey(Prompt, on_delete=models.PROTECT, related_name="assets", null=True, blank=True)
    kind = models.CharField(max_length=32, choices=Kind.choices)
    file = models.FileField(storage=private_storage, upload_to=studio_asset_path, max_length=500)
    thumbnail = models.FileField(storage=private_storage, upload_to=studio_thumbnail_path, max_length=500, blank=True)
    proxy_file = models.FileField(storage=private_storage, upload_to=studio_proxy_path, max_length=500, blank=True)
    waveform_file = models.FileField(storage=private_storage, upload_to=studio_waveform_path, max_length=500, blank=True)
    filmstrip_file = models.FileField(storage=private_storage, upload_to=studio_filmstrip_path, max_length=500, blank=True)
    original_filename = models.CharField(max_length=255)
    content_type = models.CharField(max_length=120)
    size_bytes = models.PositiveBigIntegerField()
    checksum_sha256 = models.CharField(max_length=64)
    width = models.PositiveIntegerField(null=True, blank=True)
    height = models.PositiveIntegerField(null=True, blank=True)
    duration_ms = models.PositiveBigIntegerField(null=True, blank=True)
    media_metadata = models.JSONField(default=dict, blank=True)
    processing_status = models.CharField(
        max_length=20, choices=ProcessingStatus.choices, default=ProcessingStatus.NOT_REQUIRED,
    )
    processing_error = models.TextField(blank=True)
    processing_attempts = models.PositiveSmallIntegerField(default=0)
    processing_started_at = models.DateTimeField(null=True, blank=True)
    processing_finished_at = models.DateTimeField(null=True, blank=True)
    ai_metadata = models.JSONField(default=dict, blank=True)
    is_starred = models.BooleanField(default=False)
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
        indexes = [models.Index(fields=["processing_status", "created_at"], name="studio_media_queue")]


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
    status_comment = models.TextField(blank=True)

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
    prompt = models.ForeignKey(Prompt, on_delete=models.PROTECT, related_name="ai_usage", null=True, blank=True)
    action = models.CharField(max_length=40)
    model = models.CharField(max_length=80)
    status = models.CharField(max_length=20)
    input_chars = models.PositiveIntegerField(default=0)
    output_chars = models.PositiveIntegerField(default=0)
    input_tokens = models.PositiveIntegerField(default=0)
    output_tokens = models.PositiveIntegerField(default=0)
    total_tokens = models.PositiveIntegerField(default=0)
    error_code = models.CharField(max_length=80, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at", "id"]


class UserTokenQuota(models.Model):
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="studio_token_quota",
    )
    allowance = models.PositiveBigIntegerField(default=1_000_000)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"{self.user}: {self.allowance}"


class ProjectAssistantContext(models.Model):
    project = models.OneToOneField(Project, on_delete=models.CASCADE, related_name="assistant_context")
    content = models.TextField(blank=True)
    asset = models.ForeignKey(
        Asset, on_delete=models.SET_NULL, related_name="assistant_context_for", null=True, blank=True,
    )
    updated_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.SET_NULL,
        related_name="updated_studio_assistant_contexts", null=True, blank=True,
    )
    updated_at = models.DateTimeField(auto_now=True)


class EpisodeComic(models.Model):
    class Status(models.TextChoices):
        RUNNING = "RUNNING", "Running"
        SUCCESS = "SUCCESS", "Success"
        ERROR = "ERROR", "Error"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    episode = models.ForeignKey(Episode, on_delete=models.CASCADE, related_name="comics")
    asset = models.ForeignKey(Asset, on_delete=models.SET_NULL, related_name="episode_comics", null=True, blank=True)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.RUNNING)
    model = models.CharField(max_length=160, blank=True)
    error_message = models.TextField(blank=True)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_episode_comics",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-created_at", "id"]


class EpisodeConsistencyReview(models.Model):
    class Status(models.TextChoices):
        SUCCESS = "SUCCESS", "Completed"
        ERROR = "ERROR", "Failed"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    episode = models.ForeignKey(Episode, on_delete=models.CASCADE, related_name="consistency_reviews")
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.SUCCESS)
    model = models.CharField(max_length=160, blank=True)
    content = models.TextField(blank=True)
    error_message = models.TextField(blank=True)
    image_count = models.PositiveIntegerField(default=0)
    score = models.PositiveSmallIntegerField(null=True, blank=True)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_episode_consistency_reviews",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at", "id"]


class ImageGenerationJob(models.Model):
    class Status(models.TextChoices):
        QUEUED = "QUEUED", "Queued"
        RUNNING = "RUNNING", "Running"
        SUCCESS = "SUCCESS", "Success"
        ERROR = "ERROR", "Error"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.PROTECT, related_name="image_generation_jobs")
    project = models.ForeignKey(
        Project, on_delete=models.PROTECT, related_name="image_generation_jobs",
        null=True, blank=True,
    )
    prompt = models.ForeignKey(
        Prompt, on_delete=models.PROTECT, related_name="image_generation_jobs",
        null=True, blank=True,
    )
    requested_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="studio_image_generation_jobs",
    )
    model_profile = models.ForeignKey(AiModelProfile, on_delete=models.PROTECT, related_name="image_generation_jobs")
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.QUEUED)
    request_prompt = models.TextField()
    options = models.JSONField(default=dict)
    reference_asset_ids = models.JSONField(default=list)
    result_asset = models.ForeignKey(
        Asset, on_delete=models.PROTECT, related_name="generation_jobs", null=True, blank=True,
    )
    provider_model = models.CharField(max_length=160, blank=True)
    error_message = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    started_at = models.DateTimeField(null=True, blank=True)
    finished_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-created_at", "id"]
        indexes = [models.Index(fields=["status", "created_at"], name="studio_img_job_queue")]


class MovieTimeline(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    workspace = models.ForeignKey(Workspace, on_delete=models.CASCADE, related_name="movie_timelines")
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="owned_movie_timelines")
    projects = models.ManyToManyField(Project, related_name="movie_timelines", blank=True)
    title = models.CharField(max_length=200, default="Main edit")
    aspect_ratio = models.CharField(max_length=12, default="9:16")
    resolution = models.CharField(max_length=20, default="1080x1920")
    fps = models.PositiveSmallIntegerField(default=25)
    schema_version = models.PositiveSmallIntegerField(default=MOVIE_TIMELINE_SCHEMA_VERSION)
    timeline = models.JSONField(default=default_movie_timeline, blank=True)
    editor_wallpaper = models.ImageField(
        upload_to="studio/movie-edit-wallpapers/",
        blank=True,
    )
    is_archived = models.BooleanField(default=False)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="created_movie_timelines",
    )
    updated_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="updated_movie_timelines",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["workspace_id", "title", "id"]

    def save(self, *args, **kwargs):
        if not self.workspace_id and self.project_id:
            self.workspace_id = self.project.workspace_id
        super().save(*args, **kwargs)


class MovieTimelineRevision(models.Model):
    class Reason(models.TextChoices):
        SAVE = "SAVE", "Save"
        RESTORE = "RESTORE", "Restore"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    timeline = models.ForeignKey(MovieTimeline, on_delete=models.CASCADE, related_name="revisions")
    title = models.CharField(max_length=200)
    aspect_ratio = models.CharField(max_length=12)
    resolution = models.CharField(max_length=20)
    fps = models.PositiveSmallIntegerField()
    schema_version = models.PositiveSmallIntegerField(default=MOVIE_TIMELINE_SCHEMA_VERSION)
    snapshot = models.JSONField(default=dict)
    reason = models.CharField(max_length=16, choices=Reason.choices, default=Reason.SAVE)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="movie_timeline_revisions",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at", "-id"]
        indexes = [models.Index(fields=["timeline", "-created_at"], name="studio_movie_rev_time")]


class MovieRenderJob(models.Model):
    class Status(models.TextChoices):
        QUEUED = "QUEUED", "Queued"
        RUNNING = "RUNNING", "Running"
        SUCCEEDED = "SUCCEEDED", "Succeeded"
        FAILED = "FAILED", "Failed"
        CANCELLED = "CANCELLED", "Cancelled"

    class Profile(models.TextChoices):
        DRAFT_720 = "DRAFT_720", "Draft 720p"
        REVIEW_1080 = "REVIEW_1080", "Review 1080p"

    class AudioProfile(models.TextChoices):
        ORIGINAL = "ORIGINAL", "Original mix"
        BALANCED = "BALANCED", "Balanced"
        CLEAN_SPEECH = "CLEAN_SPEECH", "Clean speech"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    timeline = models.ForeignKey(MovieTimeline, on_delete=models.PROTECT, related_name="render_jobs")
    project = models.ForeignKey(Project, on_delete=models.PROTECT, related_name="movie_render_jobs")
    title = models.CharField(max_length=200)
    profile = models.CharField(max_length=20, choices=Profile.choices, default=Profile.DRAFT_720)
    audio_profile = models.CharField(
        max_length=20, choices=AudioProfile.choices, default=AudioProfile.CLEAN_SPEECH,
    )
    target_lufs = models.SmallIntegerField(default=-16)
    aspect_ratio = models.CharField(max_length=12)
    width = models.PositiveIntegerField()
    height = models.PositiveIntegerField()
    fps = models.PositiveSmallIntegerField()
    duration_ms = models.PositiveBigIntegerField(default=0)
    schema_version = models.PositiveSmallIntegerField(default=MOVIE_TIMELINE_SCHEMA_VERSION)
    snapshot = models.JSONField(default=dict)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.QUEUED)
    progress = models.PositiveSmallIntegerField(default=0)
    cancel_requested = models.BooleanField(default=False)
    output_asset = models.ForeignKey(
        Asset, on_delete=models.PROTECT, related_name="movie_render_jobs", null=True, blank=True,
    )
    error_message = models.TextField(blank=True)
    requested_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name="movie_render_jobs",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    started_at = models.DateTimeField(null=True, blank=True)
    completed_at = models.DateTimeField(null=True, blank=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-created_at", "-id"]
        indexes = [models.Index(fields=["status", "created_at"], name="studio_render_queue")]


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
    archived_at = models.DateTimeField(null=True, blank=True)
    archived_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="archived_studio_docx_imports",
        null=True,
        blank=True,
    )

    class Meta:
        ordering = ["-created_at", "id"]
