from django import forms

from lessons.ai_gateway import TEXT_MODELS

from .ai_catalog import default_prompt_template
from .models import AdditionalGeneration, Asset, Character, DialogueLine, Episode, Project, Prompt, PromptBlock, Scene, StudioTextModel


class ProjectForm(forms.ModelForm):
    translation_languages = forms.CharField(required=False, help_text="Comma-separated language codes, for example: en, pl, de")

    class Meta:
        model = Project
        fields = ["project_type", "title", "concept", "original_language", "translation_languages", "rights_holder", "publication_info", "status"]
        widgets = {"concept": forms.Textarea(attrs={"rows": 4})}

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        if self.instance and self.instance.pk:
            self.initial["translation_languages"] = ", ".join(self.instance.translation_languages or [])

    def clean_translation_languages(self):
        value = self.cleaned_data["translation_languages"]
        return list(dict.fromkeys(part.strip().lower() for part in value.split(",") if part.strip()))


class CharacterForm(forms.ModelForm):
    class Meta:
        model = Character
        fields = ["name", "description", "visual_description"]


class EpisodeForm(forms.ModelForm):
    class Meta:
        model = Episode
        fields = ["number", "title", "summary"]


class SceneForm(forms.ModelForm):
    class Meta:
        model = Scene
        fields = ["number", "title", "hook", "description", "location", "actions", "performance_notes", "status"]


class DialogueLineForm(forms.ModelForm):
    class Meta:
        model = DialogueLine
        fields = ["speaker", "text", "language", "delivery", "status"]


class PromptForm(forms.ModelForm):
    class Meta:
        model = Prompt
        fields = ["ai_model", "prompt_type", "title", "status"]

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
