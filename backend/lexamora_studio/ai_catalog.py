from django.conf import settings
from django.core.exceptions import ValidationError

from lessons.ai_gateway import TEXT_MODELS

from .models import PromptTemplate, StudioTextModel


PROMPT_LANGUAGES = (
    ("BY", "BY"),
    ("DE", "DE"),
    ("EN", "EN"),
    ("ES", "ES"),
    ("PL", "PL"),
    ("PT", "PT"),
    ("RU", "RU"),
    ("UA", "UA"),
)
PROMPT_LANGUAGE_NAMES = {
    "BY": "Belarusian",
    "DE": "German",
    "EN": "English",
    "ES": "Spanish",
    "PL": "Polish",
    "PT": "Portuguese",
    "RU": "Russian",
    "UA": "Ukrainian",
}


def active_text_models():
    return StudioTextModel.objects.filter(
        provider=StudioTextModel.Provider.OPENAI,
        is_active=True,
        model_id__in=TEXT_MODELS,
    )


def default_text_model_id():
    configured = active_text_models().filter(is_default=True).first()
    if configured:
        return configured.model_id
    preferred = settings.OPENAI_TRANSLATION_MODEL
    if active_text_models().filter(model_id=preferred).exists():
        return preferred
    fallback = active_text_models().first()
    return fallback.model_id if fallback else "gpt-5.4-mini"


def project_text_model_id(project):
    profile = getattr(project, "default_translation_model", None)
    if profile and active_text_models().filter(pk=profile.pk).exists():
        return profile.model_id
    return default_text_model_id()


def selected_text_model(model_id):
    clean = (model_id or "").strip() or default_text_model_id()
    profile = active_text_models().filter(model_id=clean).first()
    if profile is None:
        raise ValidationError("Choose an active OpenAI text model from Studio settings.")
    return profile.model_id


def active_prompt_templates(prompt_type=None):
    queryset = PromptTemplate.objects.filter(is_active=True)
    if prompt_type in {"IMAGE", "VIDEO", "AUDIO", "TEXT"}:
        queryset = queryset.filter(scope__in=[PromptTemplate.Scope.ALL, prompt_type])
    return queryset


def default_prompt_template(prompt_type=None):
    queryset = active_prompt_templates(prompt_type)
    template = queryset.filter(is_default=True).first() or queryset.first()
    if template is None:
        raise ValidationError("Create and activate a prompt template in Studio settings first.")
    return template


def selected_prompt_template(template_id, prompt_type=None):
    if template_id:
        template = active_prompt_templates(prompt_type).filter(id=template_id).first()
        if template is not None:
            return template
    return default_prompt_template(prompt_type)
