import base64
from dataclasses import dataclass

import requests
from django.conf import settings

from .provider_credentials import CredentialConfigurationError, get_provider_api_key


TEXT_MODELS = {"gpt-5.4-nano", "gpt-5.4-mini", "gpt-5.4", "gpt-5.5"}
SPEECH_MODELS = {"gpt-4o-mini-transcribe", "gpt-4o-transcribe", "whisper-1", "gpt-4o-transcribe-diarize"}
TTS_MODELS = {"gpt-4o-mini-tts", "tts-1", "tts-1-hd"}
IMAGE_MODELS = {"gpt-4o-mini", "gpt-4.1-mini", "gpt-4.1", "gpt-4o", "gpt-5.4-mini", "gpt-5.4"}
TTS_VOICES = {"alloy", "ash", "ballad", "coral", "echo", "fable", "nova", "onyx", "sage", "shimmer", "verse", "marin", "cedar"}


class ProviderError(Exception):
    pass


def require_model(model: str, allowed: set[str], default: str) -> str:
    selected = model.strip() or default
    if selected not in allowed:
        raise ProviderError("The selected model is not supported by the server.")
    return selected


def _openai_headers() -> dict:
    try:
        api_key = get_provider_api_key("openai")
    except CredentialConfigurationError as exc:
        raise ProviderError(str(exc)) from exc
    if not api_key:
        raise ProviderError("OpenAI is not configured on the server.")
    return {"Authorization": f"Bearer {api_key}"}


def _safe_response(response: requests.Response) -> None:
    if not response.ok:
        if response.status_code in {401, 403}:
            raise ProviderError("The AI provider rejected the server credentials.")
        if response.status_code == 429:
            raise ProviderError("The AI provider is busy. Please try again shortly.")
        raise ProviderError("The AI provider request failed.")


def _extract_response_text(payload: dict) -> str:
    output_text = payload.get("output_text")
    if isinstance(output_text, str) and output_text.strip():
        return output_text.strip()
    parts = []
    for item in payload.get("output", []):
        if not isinstance(item, dict):
            continue
        for content in item.get("content", []):
            if isinstance(content, dict) and isinstance(content.get("text"), str):
                parts.append(content["text"])
    return "".join(parts).strip()


def run_text(model: str, prompt: str) -> tuple[str, str]:
    selected_model = require_model(model, TEXT_MODELS, "gpt-5.4-mini")
    if not prompt.strip() or len(prompt) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Text is empty or exceeds the server limit.")
    try:
        response = requests.post(
            f"{settings.OPENAI_BASE_URL}/responses",
            headers={**_openai_headers(), "Content-Type": "application/json"},
            json={"model": selected_model, "input": prompt},
            timeout=60,
        )
        _safe_response(response)
        output = _extract_response_text(response.json())
    except (requests.RequestException, ValueError) as exc:
        raise ProviderError("The AI provider is unavailable.") from exc
    if not output:
        raise ProviderError("The AI provider returned an empty response.")
    return output, selected_model


def generate_image(prompt: str) -> tuple[bytes, str]:
    clean_prompt = prompt.strip()
    if not clean_prompt or len(clean_prompt) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Image prompt is empty or exceeds the server limit.")
    model = "gpt-image-1"
    try:
        response = requests.post(
            f"{settings.OPENAI_BASE_URL}/images/generations",
            headers={**_openai_headers(), "Content-Type": "application/json"},
            json={"model": model, "prompt": clean_prompt, "size": "1024x1024", "quality": "low"},
            timeout=180,
        )
        _safe_response(response)
        payload = response.json()
        encoded = str((payload.get("data") or [{}])[0].get("b64_json") or "")
        image_bytes = base64.b64decode(encoded, validate=True)
    except (requests.RequestException, ValueError, KeyError, IndexError) as exc:
        raise ProviderError("Image generation is unavailable.") from exc
    if not image_bytes:
        raise ProviderError("The AI provider returned an empty image.")
    return image_bytes, model


def transcribe(model: str, language: str, audio_file) -> tuple[str, str]:
    selected_model = require_model(model, SPEECH_MODELS, "gpt-4o-mini-transcribe")
    if audio_file.size > settings.AI_MAX_AUDIO_BYTES:
        raise ProviderError("Audio exceeds the server limit.")
    try:
        response = requests.post(
            f"{settings.OPENAI_BASE_URL}/audio/transcriptions",
            headers=_openai_headers(),
            data={"model": selected_model, "language": language.strip()[:12], "response_format": "json"},
            files={"file": (audio_file.name or "speech.m4a", audio_file.read(), audio_file.content_type or "audio/mp4")},
            timeout=90,
        )
        _safe_response(response)
        text = str(response.json().get("text", "")).strip()
    except (requests.RequestException, ValueError) as exc:
        raise ProviderError("Speech recognition is unavailable.") from exc
    if not text:
        raise ProviderError("Speech recognition returned no text.")
    return text, selected_model


def synthesize_openai(model: str, voice: str, text: str, speed: float) -> tuple[bytes, str, str]:
    selected_model = require_model(model, TTS_MODELS, "gpt-4o-mini-tts")
    selected_voice = voice.strip() or "coral"
    if selected_voice not in TTS_VOICES:
        raise ProviderError("The selected voice is not supported by the server.")
    if not text.strip() or len(text) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Text is empty or exceeds the server limit.")
    try:
        response = requests.post(
            f"{settings.OPENAI_BASE_URL}/audio/speech",
            headers={**_openai_headers(), "Content-Type": "application/json"},
            json={"model": selected_model, "voice": selected_voice, "input": text, "response_format": "mp3", "speed": max(0.5, min(float(speed), 2.5))},
            timeout=90,
        )
        _safe_response(response)
    except (requests.RequestException, ValueError) as exc:
        raise ProviderError("Speech generation is unavailable.") from exc
    return response.content, selected_model, selected_voice


def synthesize_elevenlabs(model: str, voice_id: str, text: str) -> tuple[bytes, str, str]:
    try:
        api_key = get_provider_api_key("elevenlabs")
    except CredentialConfigurationError as exc:
        raise ProviderError(str(exc)) from exc
    if not api_key:
        raise ProviderError("ElevenLabs is not configured on the server.")
    clean_voice_id = voice_id.strip()
    if not clean_voice_id:
        raise ProviderError("Choose an ElevenLabs voice first.")
    if not text.strip() or len(text) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Text is empty or exceeds the server limit.")
    try:
        response = requests.post(
            f"{settings.ELEVENLABS_BASE_URL}/text-to-speech/{clean_voice_id}",
            params={"output_format": "mp3_44100_128"},
            headers={"xi-api-key": api_key, "Content-Type": "application/json"},
            json={"text": text, "model_id": model.strip() or "eleven_v3"},
            timeout=90,
        )
        _safe_response(response)
    except requests.RequestException as exc:
        raise ProviderError("ElevenLabs speech generation is unavailable.") from exc
    return response.content, model.strip() or "eleven_v3", clean_voice_id


def recognize_image(model: str, prompt: str, image_file) -> tuple[str, str]:
    selected_model = require_model(model, IMAGE_MODELS, "gpt-4.1-mini")
    if image_file.size > settings.AI_MAX_IMAGE_BYTES:
        raise ProviderError("Image exceeds the server limit.")
    image_bytes = image_file.read()
    image_data = base64.b64encode(image_bytes).decode("ascii")
    content_type = image_file.content_type or "image/jpeg"
    instruction = prompt.strip() or "Read all meaningful text from this image. Return only the recognized text."
    if len(instruction) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Prompt exceeds the server limit.")
    try:
        response = requests.post(
            f"{settings.OPENAI_BASE_URL}/responses",
            headers={**_openai_headers(), "Content-Type": "application/json"},
            json={
                "model": selected_model,
                "input": [{"role": "user", "content": [
                    {"type": "input_text", "text": instruction},
                    {"type": "input_image", "image_url": f"data:{content_type};base64,{image_data}"},
                ]}],
            },
            timeout=90,
        )
        _safe_response(response)
        output = _extract_response_text(response.json())
    except (requests.RequestException, ValueError) as exc:
        raise ProviderError("Image text recognition is unavailable.") from exc
    if not output:
        raise ProviderError("Image text recognition returned no text.")
    return output, selected_model
