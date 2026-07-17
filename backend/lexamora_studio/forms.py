import re

from django import forms
from django.contrib.auth import get_user_model
from django.core.validators import validate_email
from django.db.models import Q

from lessons.ai_gateway import TEXT_MODELS

from .ai_catalog import PROMPT_LANGUAGES, active_text_models, default_prompt_template
from .models import AdditionalGeneration, AiModelProfile, Asset, Character, DialogueLine, Episode, Project, ProjectMembership, Prompt, PromptBlock, RecommendedTrack, Scene, StudioTextModel, Workspace, WorkspaceMembership


class WorkspaceForm(forms.ModelForm):
    avatar = forms.ImageField(required=False, label="Avatar")

    class Meta:
        model = Workspace
        fields = [
            "name", "description", "documentation_language", "dialogue_language", "prompt_language",
            "image_prompt_template", "video_prompt_template", "audio_prompt_template",
            "text_prompt_template",
            "default_image_model",
        ]
        widgets = {
            "description": forms.Textarea(attrs={"rows": 4}),
            "documentation_language": forms.Select(choices=PROMPT_LANGUAGES),
            "dialogue_language": forms.Select(choices=PROMPT_LANGUAGES),
            "prompt_language": forms.Select(choices=PROMPT_LANGUAGES),
            "image_prompt_template": forms.Textarea(attrs={"rows": 3}),
            "video_prompt_template": forms.Textarea(attrs={"rows": 3}),
            "audio_prompt_template": forms.Textarea(attrs={"rows": 3}),
            "text_prompt_template": forms.Textarea(attrs={"rows": 3}),
        }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["default_image_model"].queryset = AiModelProfile.objects.filter(
            is_active=True,
            media_type=AiModelProfile.MediaType.IMAGE,
            provider__iexact="OpenAI",
        )
        self.fields["default_image_model"].required = False
        self.fields["default_image_model"].empty_label = "Choose OpenAI image model"
        for name in ("documentation_language", "dialogue_language", "prompt_language"):
            self.fields[name].required = False

    def clean(self):
        cleaned = super().clean()
        for name in ("documentation_language", "dialogue_language", "prompt_language"):
            cleaned[name] = (cleaned.get(name) or getattr(self.instance, name, "") or "EN").upper()
        return cleaned


class ProjectForm(forms.ModelForm):
    class Meta:
        model = Project
        fields = ["project_type", "title", "description", "concept", "rights_holder", "publication_info", "status", "status_comment"]
        widgets = {
            "description": forms.Textarea(attrs={"rows": 3}),
            "concept": forms.Textarea(attrs={"rows": 4}),
        }


class ProjectSettingsForm(forms.ModelForm):
    SECTION_CHOICES = (
        ("service", "Service information"),
        ("music", "Recommended music"),
        ("legal", "Legal information"),
        ("images", "Images"),
        ("characters", "Characters"),
        ("episodes", "Episodes"),
    )
    original_language = forms.CharField(widget=forms.Select(choices=PROMPT_LANGUAGES), initial="EN")
    translation_languages = forms.CharField(required=False, help_text="Comma-separated language codes, for example: en, pl, de")
    confirm_language_propagation = forms.BooleanField(required=False, widget=forms.HiddenInput)
    hidden_sections = forms.MultipleChoiceField(
        choices=SECTION_CHOICES,
        required=False,
        widget=forms.CheckboxSelectMultiple,
        label="Do not show these blocks",
    )

    class Meta:
        model = Project
        fields = ["original_language", "documentation_language", "dialogue_language", "prompt_language", "translation_languages", "default_translation_model", "prompt_template", "hidden_sections"]
        widgets = {
            "documentation_language": forms.Select(choices=PROMPT_LANGUAGES),
            "dialogue_language": forms.Select(choices=PROMPT_LANGUAGES),
            "prompt_language": forms.Select(choices=PROMPT_LANGUAGES),
            "prompt_template": forms.Textarea(attrs={"rows": 4, "placeholder": "Text appended to every new prompt in this project"}),
        }
        labels = {
            "default_translation_model": "Default translation model",
            "prompt_template": "Project Prompt Template",
        }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["default_translation_model"].queryset = active_text_models()
        self.fields["default_translation_model"].required = False
        self.fields["default_translation_model"].empty_label = "Use Studio default"
        for name in ("documentation_language", "dialogue_language", "prompt_language"):
            self.fields[name].required = False
        if self.instance and not self.instance._state.adding:
            self.initial["original_language"] = (self.instance.original_language or "EN").upper()
            self.fields["original_language"].widget.attrs["data-initial-language"] = self.initial["original_language"]
            self.initial["translation_languages"] = ", ".join(self.instance.translation_languages or [])
            prompt_count = Prompt.objects.filter(
                scene__episode__project=self.instance,
                source_prompt__isnull=True,
            ).count()
            dialogue_count = DialogueLine.objects.filter(scene__episode__project=self.instance).count()
            self.language_change_summary = (
                f"The new language will be applied to {prompt_count} original prompts and "
                f"{dialogue_count} dialogue lines. Saved translations remain translations."
            )

    def clean_original_language(self):
        value = self.cleaned_data["original_language"].strip().upper()
        if value not in dict(PROMPT_LANGUAGES):
            raise forms.ValidationError("Choose a supported project language.")
        return value

    def clean_translation_languages(self):
        value = self.cleaned_data["translation_languages"]
        return list(dict.fromkeys(part.strip().lower() for part in value.split(",") if part.strip()))

    def clean(self):
        cleaned = super().clean()
        for name in ("documentation_language", "dialogue_language", "prompt_language"):
            cleaned[name] = (cleaned.get(name) or getattr(self.instance, name, "") or self.initial.get(name) or "EN").upper()
        if self.instance and not self.instance._state.adding:
            old_language = (self.instance.original_language or "EN").strip().upper()
            new_language = (cleaned.get("original_language") or "").strip().upper()
            if new_language and new_language != old_language and not cleaned.get("confirm_language_propagation"):
                self.add_error(
                    "confirm_language_propagation",
                    "Confirm that the new language will update every original prompt and dialogue line in this project.",
                )
        return cleaned


class RecommendedTrackForm(forms.ModelForm):
    class Meta:
        model = RecommendedTrack
        fields = ["is_primary", "platform", "artist", "title", "url", "position"]
        widgets = {"position": forms.HiddenInput()}
        labels = {"is_primary": "Main"}


class ProjectMembershipForm(forms.Form):
    email = forms.EmailField(label="User email")
    role = forms.ChoiceField(choices=ProjectMembership.Role.choices)

    def __init__(self, *args, project, **kwargs):
        self.project = project
        super().__init__(*args, **kwargs)

    def clean_email(self):
        from django.contrib.auth import get_user_model

        email = self.cleaned_data["email"].strip().lower()
        matches = list(get_user_model().objects.filter(email__iexact=email)[:2])
        if len(matches) != 1:
            raise forms.ValidationError("A single registered user with this email is required.")
        if matches[0] == self.project.workspace.owner:
            raise forms.ValidationError("The workspace owner already has full access.")
        self.user = matches[0]
        return email


class ProjectBulkMembershipForm(forms.Form):
    emails = forms.CharField(
        label="User emails",
        widget=forms.Textarea(attrs={
            "rows": 5,
            "placeholder": "one@example.com\ntwo@example.com",
        }),
        help_text="Up to 100 registered users. Separate addresses with commas, semicolons, spaces, or new lines.",
    )
    role = forms.ChoiceField(choices=ProjectMembership.Role.choices)

    def __init__(self, *args, project, **kwargs):
        self.project = project
        self.users = []
        self.missing_emails = []
        super().__init__(*args, **kwargs)

    def clean_emails(self):
        from django.contrib.auth import get_user_model

        values = list(dict.fromkeys(
            value.strip().lower()
            for value in re.split(r"[\s,;]+", self.cleaned_data["emails"])
            if value.strip()
        ))
        if not values:
            raise forms.ValidationError("Enter at least one email address.")
        if len(values) > 100:
            raise forms.ValidationError("Add no more than 100 users at once.")
        for value in values:
            validate_email(value)
        query = Q(pk__in=[])
        for value in values:
            query |= Q(email__iexact=value)
        matches = list(get_user_model().objects.filter(query))
        by_email = {user.email.strip().lower(): user for user in matches if user.email}
        owner_email = (self.project.workspace.owner.email or "").strip().lower()
        self.users = [by_email[value] for value in values if value in by_email and value != owner_email]
        self.missing_emails = [value for value in values if value not in by_email]
        if not self.users:
            raise forms.ValidationError("No registered project users were found in this list.")
        return "\n".join(values)


class ProjectUserSelectionForm(forms.Form):
    users = forms.ModelMultipleChoiceField(
        label="Users",
        queryset=get_user_model().objects.none(),
        widget=forms.CheckboxSelectMultiple,
    )
    role = forms.ChoiceField(choices=ProjectMembership.Role.choices)

    def __init__(self, *args, project, **kwargs):
        self.project = project
        super().__init__(*args, **kwargs)
        self.fields["users"].queryset = (
            get_user_model().objects.filter(is_active=True)
            .exclude(pk=project.workspace.owner_id)
            .order_by("email", "username")
        )

    def clean_users(self):
        users = list(self.cleaned_data["users"])
        if len(users) > 100:
            raise forms.ValidationError("Select no more than 100 users at once.")
        return users


class WorkspaceMembershipForm(forms.Form):
    email = forms.EmailField(label="User email")
    role = forms.ChoiceField(choices=[choice for choice in WorkspaceMembership.Role.choices if choice[0] != WorkspaceMembership.Role.OWNER])
    can_use_ai = forms.BooleanField(required=False, initial=True, label="AI access")

    def __init__(self, *args, workspace, **kwargs):
        self.workspace = workspace
        super().__init__(*args, **kwargs)

    def clean_email(self):
        from django.contrib.auth import get_user_model

        email = self.cleaned_data["email"].strip().lower()
        matches = list(get_user_model().objects.filter(email__iexact=email)[:2])
        if len(matches) != 1:
            raise forms.ValidationError("A single registered user with this email is required.")
        if matches[0] == self.workspace.owner:
            raise forms.ValidationError("The workspace owner already has full access.")
        self.user = matches[0]
        return email


class WorkspaceUserSelectionForm(forms.Form):
    users = forms.ModelMultipleChoiceField(
        label="Users",
        queryset=get_user_model().objects.none(),
        widget=forms.CheckboxSelectMultiple,
    )
    role = forms.ChoiceField(
        choices=[choice for choice in WorkspaceMembership.Role.choices if choice[0] != WorkspaceMembership.Role.OWNER]
    )
    can_use_ai = forms.BooleanField(required=False, initial=True, label="AI access")

    def __init__(self, *args, workspace, **kwargs):
        self.workspace = workspace
        super().__init__(*args, **kwargs)
        self.fields["users"].queryset = (
            get_user_model().objects.filter(is_active=True)
            .exclude(pk=workspace.owner_id)
            .order_by("email", "username")
        )

    def clean_users(self):
        users = list(self.cleaned_data["users"])
        if len(users) > 100:
            raise forms.ValidationError("Select no more than 100 users at once.")
        return users


class CharacterForm(forms.ModelForm):
    class Meta:
        model = Character
        fields = [
            "name", "name_prompt", "name_dialogue",
            "description", "description_prompt", "description_dialogue",
            "visual_description", "visual_description_prompt", "visual_description_dialogue",
        ]


class CharacterCreateForm(CharacterForm):
    avatar_file = forms.ImageField(
        required=False,
        label="Avatar",
        widget=forms.ClearableFileInput(attrs={"accept": "image/jpeg,image/png,image/webp"}),
    )


class EpisodeForm(forms.ModelForm):
    class Meta:
        model = Episode
        fields = ["number", "title", "summary", "language"]
        widgets = {"language": forms.Select(choices=PROMPT_LANGUAGES)}

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["language"].required = False

    def clean_language(self):
        return (self.cleaned_data.get("language") or getattr(self.instance, "language", "") or self.initial.get("language") or "EN").upper()


class SceneForm(forms.ModelForm):
    scene_type = forms.ChoiceField(
        choices=Scene.Type.choices,
        required=False,
        initial=Scene.Type.ORIGINAL,
    )

    class Meta:
        model = Scene
        fields = [
            "title", "title_prompt", "title_dialogue", "scene_type",
            "hook", "hook_prompt", "hook_dialogue",
            "description", "description_prompt", "description_dialogue",
            "location", "location_prompt", "location_dialogue",
            "actions", "actions_prompt", "actions_dialogue",
            "performance_notes", "performance_notes_prompt", "performance_notes_dialogue",
            "status", "status_comment",
        ]
        widgets = {field: forms.Textarea(attrs={"rows": 2}) for field in (
            "hook", "hook_prompt", "hook_dialogue", "description", "description_prompt", "description_dialogue",
            "location", "location_prompt", "location_dialogue", "actions", "actions_prompt", "actions_dialogue",
            "performance_notes", "performance_notes_prompt", "performance_notes_dialogue", "status_comment",
        )}

    def clean_scene_type(self):
        return self.cleaned_data.get("scene_type") or self.instance.scene_type or Scene.Type.ORIGINAL


class DialogueLineForm(forms.ModelForm):
    class Meta:
        model = DialogueLine
        fields = [
            "speaker_documentation", "speaker_prompt", "speaker",
            "text_documentation", "text_prompt", "text", "language",
            "delivery_documentation", "delivery_prompt", "delivery",
            "status", "status_comment",
        ]
        widgets = {field: forms.Textarea(attrs={"rows": 3}) for field in (
            "text_documentation", "text_prompt", "text", "delivery_documentation", "delivery_prompt", "delivery", "status_comment",
        )}

    def __init__(self, *args, project=None, **kwargs):
        super().__init__(*args, **kwargs)
        names = list(project.characters.order_by("position", "name").values_list("name", flat=True)) if project else []
        current = getattr(self.instance, "speaker_documentation", "")
        choices = [("", "Select character")]
        choices.extend((name, name) for name in dict.fromkeys([*names, *([current] if current else [])]))
        self.fields["speaker_documentation"].widget = forms.Select(choices=choices)
        self.fields["speaker_prompt"].widget = forms.TextInput(attrs={"readonly": True})
        self.fields["speaker"].widget = forms.TextInput(attrs={"readonly": True})


class PromptForm(forms.ModelForm):
    class Meta:
        model = Prompt
        fields = ["ai_model", "prompt_type", "title", "content", "status", "status_comment"]
        widgets = {"content": forms.Textarea(attrs={"rows": 8})}

    def save(self, commit=True):
        instance = super().save(commit=False)
        instance.template = default_prompt_template(instance.prompt_type)
        if commit:
            instance.save()
            self.save_m2m()
        return instance


class PromptBlockForm(forms.ModelForm):
    class Meta:
        model = PromptBlock
        fields = ["block_type", "content"]


class ImageUploadForm(forms.Form):
    file = forms.FileField(
        help_text="JPG, PNG or WEBP. The original is stored privately.",
        widget=forms.ClearableFileInput(attrs={"accept": "image/jpeg,image/png,image/webp"}),
    )

    def clean_file(self):
        uploaded = self.cleaned_data["file"]
        if (getattr(uploaded, "content_type", "") or "").lower() not in {"image/jpeg", "image/png", "image/webp"}:
            raise forms.ValidationError("Choose a JPG, PNG or WEBP image.")
        return uploaded


class MultipleFileInput(forms.ClearableFileInput):
    allow_multiple_selected = True


class MultipleImageField(forms.FileField):
    widget = MultipleFileInput

    def clean(self, data, initial=None):
        files = data if isinstance(data, (list, tuple)) else [data]
        return [super().clean(uploaded, initial) for uploaded in files]


class MultipleImageUploadForm(forms.Form):
    file = MultipleImageField(
        help_text="JPG, PNG or WEBP.",
        widget=MultipleFileInput(attrs={"accept": "image/jpeg,image/png,image/webp"}),
    )

    def __init__(self, *args, limit=10, **kwargs):
        super().__init__(*args, **kwargs)
        self.limit = limit
        self.fields["file"].widget.attrs["data-file-limit"] = str(limit)

    def clean_file(self):
        uploads = self.cleaned_data["file"]
        if not uploads or len(uploads) > self.limit:
            raise forms.ValidationError(f"Choose between 1 and {self.limit} images.")
        for uploaded in uploads:
            if (getattr(uploaded, "content_type", "") or "").lower() not in {"image/jpeg", "image/png", "image/webp"}:
                raise forms.ValidationError("Choose JPG, PNG or WEBP images.")
        return uploads


class DocxImportUploadForm(forms.Form):
    file = forms.FileField(help_text="DOCX up to the configured Studio upload limit.")

    def clean_file(self):
        uploaded = self.cleaned_data["file"]
        if not uploaded.name.lower().endswith(".docx"):
            raise forms.ValidationError("Choose a DOCX document.")
        return uploaded

class AdditionalGenerationForm(forms.ModelForm):
    class Meta:
        model = AdditionalGeneration
        fields = ["reason", "source_asset", "prompt", "status", "status_comment"]
        widgets = {
            "reason": forms.Textarea(attrs={"rows": 2}),
            "prompt": forms.Textarea(attrs={"rows": 5}),
            "status_comment": forms.Textarea(attrs={"rows": 2}),
        }

    def __init__(self, *args, project=None, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["source_asset"].required = False
        self.fields["source_asset"].queryset = (
            Asset.objects.filter(projects=project, content_type__startswith="image/").distinct()
            if project is not None else Asset.objects.none()
        )


class GenerationOutputUploadForm(ImageUploadForm):
    model_name = forms.CharField(
        required=False,
        max_length=160,
        help_text="Optional model or tool name used to create this result.",
    )

class AssetEditForm(forms.ModelForm):
    class Meta:
        model = Asset
        fields = ["original_filename", "kind"]


class StudioTextModelForm(forms.ModelForm):
    model_id = forms.ChoiceField(choices=sorted((value, value) for value in TEXT_MODELS))

    class Meta:
        model = StudioTextModel
        fields = ["name", "model_id", "is_active", "is_default"]


class AiModelProfileForm(forms.ModelForm):
    class Meta:
        model = AiModelProfile
        fields = ["name", "provider", "model_id", "media_type", "is_active"]
