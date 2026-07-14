from django import forms

from lessons.ai_gateway import TEXT_MODELS

from .ai_catalog import PROMPT_LANGUAGES, default_prompt_template
from .models import AdditionalGeneration, AiModelProfile, Asset, Character, DialogueLine, Episode, Project, ProjectMembership, Prompt, PromptBlock, Scene, StudioTextModel, Workspace, WorkspaceMembership


class WorkspaceForm(forms.ModelForm):
    avatar = forms.ImageField(required=False, label="Avatar")

    class Meta:
        model = Workspace
        fields = [
            "name", "description", "documentation_language", "dialogue_language", "prompt_language",
            "image_prompt_template", "video_prompt_template", "audio_prompt_template",
            "text_prompt_template",
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
        for name in ("documentation_language", "dialogue_language", "prompt_language"):
            self.fields[name].required = False

    def clean(self):
        cleaned = super().clean()
        for name in ("documentation_language", "dialogue_language", "prompt_language"):
            cleaned[name] = (cleaned.get(name) or getattr(self.instance, name, "") or "EN").upper()
        return cleaned


class ProjectForm(forms.ModelForm):
    original_language = forms.CharField(widget=forms.Select(choices=PROMPT_LANGUAGES), initial="EN")
    translation_languages = forms.CharField(required=False, help_text="Comma-separated language codes, for example: en, pl, de")
    confirm_language_propagation = forms.BooleanField(required=False, widget=forms.HiddenInput)

    class Meta:
        model = Project
        fields = ["project_type", "title", "concept", "original_language", "documentation_language", "dialogue_language", "prompt_language", "translation_languages", "prompt_template", "rights_holder", "publication_info", "status"]
        widgets = {
            "concept": forms.Textarea(attrs={"rows": 4}),
            "documentation_language": forms.Select(choices=PROMPT_LANGUAGES),
            "dialogue_language": forms.Select(choices=PROMPT_LANGUAGES),
            "prompt_language": forms.Select(choices=PROMPT_LANGUAGES),
            "prompt_template": forms.Textarea(attrs={"rows": 4, "placeholder": "Text appended to every new prompt in this project"}),
        }
        labels = {"prompt_template": "Project prompt template"}

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
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
        else:
            self.fields.pop("confirm_language_propagation")

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


class CharacterForm(forms.ModelForm):
    class Meta:
        model = Character
        fields = ["name", "description", "visual_description"]


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
    class Meta:
        model = Scene
        fields = ["title", "hook", "description", "location", "actions", "performance_notes", "status"]
        widgets = {field: forms.Textarea(attrs={"rows": 2}) for field in ("hook", "description", "location", "actions", "performance_notes")}


class DialogueLineForm(forms.ModelForm):
    class Meta:
        model = DialogueLine
        fields = ["speaker", "text", "language", "delivery", "status"]


class PromptForm(forms.ModelForm):
    class Meta:
        model = Prompt
        fields = ["ai_model", "prompt_type", "title", "content", "status"]
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
    file = forms.FileField(help_text="JPG, PNG or WEBP. The original is stored privately.")

    def clean_file(self):
        uploaded = self.cleaned_data["file"]
        if (getattr(uploaded, "content_type", "") or "").lower() not in {"image/jpeg", "image/png", "image/webp"}:
            raise forms.ValidationError("Choose a JPG, PNG or WEBP image.")
        return uploaded


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
        fields = ["reason", "source_asset", "prompt", "status"]
        widgets = {
            "reason": forms.Textarea(attrs={"rows": 2}),
            "prompt": forms.Textarea(attrs={"rows": 5}),
        }

    def __init__(self, *args, project=None, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["source_asset"].required = False
        self.fields["source_asset"].queryset = (
            Asset.objects.filter(project=project, content_type__startswith="image/")
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
