from django import forms

from .models import Character, DialogueLine, Episode, Project, Prompt, PromptBlock, Scene


class ProjectForm(forms.ModelForm):
    translation_languages = forms.CharField(required=False, help_text="Comma-separated language codes, for example: en, pl, de")

    class Meta:
        model = Project
        fields = ["project_type", "title", "concept", "original_language", "translation_languages", "rights_holder", "publication_info", "status"]
        widgets = {"concept": forms.Textarea(attrs={"rows": 4})}

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


class PromptBlockForm(forms.ModelForm):
    class Meta:
        model = PromptBlock
        fields = ["block_type", "content"]
