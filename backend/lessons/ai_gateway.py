import base64
import time
from dataclasses import dataclass

import requests
from django.conf import settings

from .provider_credentials import CredentialConfigurationError, get_provider_api_key


TEXT_MODELS = {"gpt-5.4-nano", "gpt-5.4-mini", "gpt-5.4", "gpt-5.5"}
SPEECH_MODELS = {"gpt-4o-mini-transcribe", "gpt-4o-transcribe", "whisper-1", "gpt-4o-transcribe-diarize"}
TTS_MODELS = {"gpt-4o-mini-tts", "tts-1", "tts-1-hd"}
IMAGE_MODELS = {"gpt-4o-mini", "gpt-4.1-mini", "gpt-4.1", "gpt-4o", "gpt-5.4-mini", "gpt-5.4"}
IMAGE_GENERATION_MODELS = {"gpt-image-2", "gpt-image-1"}
GOOGLE_IMAGE_MODELS = {"gemini-3.1-flash-lite-image", "gemini-3.1-flash-image"}
GOOGLE_VIDEO_MODELS = {"veo-3.1-lite-generate-preview", "veo-3.1-fast-generate-preview"}
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


def _google_headers() -> dict:
    try:
        api_key = get_provider_api_key("google")
    except CredentialConfigurationError as exc:
        raise ProviderError(str(exc)) from exc
    if not api_key:
        raise ProviderError("Google AI Studio is not configured on the server.")
    return {"x-goog-api-key": api_key, "Content-Type": "application/json"}


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


def _extract_usage(payload: dict) -> dict[str, int]:
    usage = payload.get("usage") if isinstance(payload, dict) else None
    usage = usage if isinstance(usage, dict) else {}
    input_tokens = int(usage.get("input_tokens") or 0)
    output_tokens = int(usage.get("output_tokens") or 0)
    return {
        "input_tokens": input_tokens,
        "output_tokens": output_tokens,
        "total_tokens": int(usage.get("total_tokens") or input_tokens + output_tokens),
    }


def run_text(model: str, prompt: str) -> tuple[str, str]:
    output, selected_model, _ = run_text_with_usage(model, prompt)
    return output, selected_model


def run_text_with_usage(model: str, prompt: str) -> tuple[str, str, dict[str, int]]:
    if model.strip().startswith("gemini-"):
        return run_google_text_with_usage(model, prompt)
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
        payload = response.json()
        output = _extract_response_text(payload)
        usage = _extract_usage(payload)
    except (requests.RequestException, ValueError) as exc:
        raise ProviderError("The AI provider is unavailable.") from exc
    if not output:
        raise ProviderError("The AI provider returned an empty response.")
    return output, selected_model, usage


def _extract_google_usage(payload: dict) -> dict[str, int]:
    usage = payload.get("usageMetadata") or payload.get("usage_metadata") or payload.get("usage") or {}
    input_tokens = int(usage.get("promptTokenCount") or usage.get("input_tokens") or 0)
    output_tokens = int(usage.get("candidatesTokenCount") or usage.get("output_tokens") or 0)
    return {"input_tokens": input_tokens, "output_tokens": output_tokens, "total_tokens": int(usage.get("totalTokenCount") or usage.get("total_tokens") or input_tokens + output_tokens)}


def run_google_text_with_usage(model: str, prompt: str) -> tuple[str, str, dict[str, int]]:
    selected_model = model.strip() or "gemini-3.1-flash-lite"
    if not selected_model.startswith("gemini-"):
        raise ProviderError("The selected Google text model is not supported.")
    if not prompt.strip() or len(prompt) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Text is empty or exceeds the server limit.")
    try:
        response = requests.post(
            f"{settings.GOOGLE_AI_BASE_URL}/models/{selected_model}:generateContent",
            headers=_google_headers(), json={"contents": [{"role": "user", "parts": [{"text": prompt}]}]}, timeout=90,
        )
        _safe_response(response)
        payload = response.json()
        output = "".join(str(part.get("text") or "") for candidate in payload.get("candidates", []) for part in (candidate.get("content", {}).get("parts", []) if isinstance(candidate, dict) else []) if isinstance(part, dict)).strip()
    except (requests.RequestException, ValueError) as exc:
        raise ProviderError("Google AI Studio is unavailable.") from exc
    if not output:
        raise ProviderError("Google AI Studio returned an empty response.")
    return output, selected_model, _extract_google_usage(payload)


def run_multimodal_text_with_usage(
    model: str,
    prompt: str,
    reference_images: list[tuple[str, bytes, str]] | None = None,
) -> tuple[str, str, dict[str, int]]:
    if model.strip().startswith("gemini-"):
        return run_google_multimodal_text_with_usage(model, prompt, reference_images)
    selected_model = require_model(model, TEXT_MODELS, "gpt-5.4-mini")
    clean_prompt = prompt.strip()
    if not clean_prompt or len(clean_prompt) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Text is empty or exceeds the server limit.")

    references = list(reference_images or [])
    max_references = int(getattr(settings, "AI_MAX_COMIC_REFERENCE_IMAGES", 20))
    if len(references) > max_references:
        raise ProviderError(f"A maximum of {max_references} reference images is supported.")

    content = [{"type": "input_text", "text": clean_prompt}]
    for filename, image_bytes, content_type in references:
        if not image_bytes or len(image_bytes) > settings.AI_MAX_IMAGE_BYTES:
            raise ProviderError(f"Reference image {filename} is empty or exceeds the server limit.")
        safe_content_type = content_type if content_type.startswith("image/") else "image/jpeg"
        image_data = base64.b64encode(image_bytes).decode("ascii")
        content.append({
            "type": "input_image",
            "image_url": f"data:{safe_content_type};base64,{image_data}",
        })

    try:
        response = requests.post(
            f"{settings.OPENAI_BASE_URL}/responses",
            headers={**_openai_headers(), "Content-Type": "application/json"},
            json={"model": selected_model, "input": [{"role": "user", "content": content}]},
            timeout=180,
        )
        _safe_response(response)
        payload = response.json()
        output = _extract_response_text(payload)
        usage = _extract_usage(payload)
    except (requests.RequestException, ValueError) as exc:
        raise ProviderError("The multimodal AI provider is unavailable.") from exc
    if not output:
        raise ProviderError("The multimodal AI provider returned an empty response.")
    return output, selected_model, usage


def run_google_multimodal_text_with_usage(
    model: str,
    prompt: str,
    reference_images: list[tuple[str, bytes, str]] | None = None,
) -> tuple[str, str, dict[str, int]]:
    selected_model = model.strip() or "gemini-3.1-flash-lite"
    if not selected_model.startswith("gemini-"):
        raise ProviderError("The selected Google multimodal model is not supported.")
    clean_prompt = prompt.strip()
    if not clean_prompt or len(clean_prompt) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Text is empty or exceeds the server limit.")
    references = list(reference_images or [])
    max_references = int(getattr(settings, "AI_MAX_COMIC_REFERENCE_IMAGES", 20))
    if len(references) > max_references:
        raise ProviderError(f"A maximum of {max_references} reference images is supported.")
    parts = [{"text": clean_prompt}]
    for filename, image_bytes, content_type in references:
        if not image_bytes or len(image_bytes) > settings.AI_MAX_IMAGE_BYTES:
            raise ProviderError(f"Reference image {filename} is empty or exceeds the server limit.")
        parts.append({
            "inlineData": {
                "mimeType": content_type if content_type.startswith("image/") else "image/jpeg",
                "data": base64.b64encode(image_bytes).decode("ascii"),
            },
        })
    try:
        response = requests.post(
            f"{settings.GOOGLE_AI_BASE_URL}/models/{selected_model}:generateContent",
            headers=_google_headers(),
            json={"contents": [{"role": "user", "parts": parts}]},
            timeout=180,
        )
        _safe_response(response)
        payload = response.json()
        output = "".join(
            str(part.get("text") or "")
            for candidate in payload.get("candidates", [])
            for part in candidate.get("content", {}).get("parts", [])
            if isinstance(part, dict)
        ).strip()
    except (requests.RequestException, ValueError) as exc:
        raise ProviderError("Google multimodal generation is unavailable.") from exc
    if not output:
        raise ProviderError("Google AI Studio returned an empty response.")
    return output, selected_model, _extract_google_usage(payload)


def generate_image(prompt: str) -> tuple[bytes, str]:
    image_bytes, model, _ = generate_image_with_usage(prompt)
    return image_bytes, model


def generate_image_with_usage(
    prompt: str,
    *,
    model: str = "gpt-image-1",
    reference_images: list[tuple[str, bytes, str]] | None = None,
    size: str = "1024x1024",
    quality: str = "low",
    output_format: str = "png",
    output_compression: int = 100,
    background: str = "auto",
    moderation: str = "auto",
) -> tuple[bytes, str, dict[str, int]]:
    clean_prompt = prompt.strip()
    if not clean_prompt or len(clean_prompt) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Image prompt is empty or exceeds the server limit.")
    selected_model = require_model(model, IMAGE_GENERATION_MODELS, "gpt-image-1")
    references = list(reference_images or [])[:3]
    allowed_sizes = {
        "auto", "1024x1024", "1536x1024", "1024x1536",
        "2048x2048", "2048x1152", "3840x2160", "2160x3840",
    }
    if size not in allowed_sizes:
        raise ProviderError("The selected image size is not supported.")
    if size in {"2048x2048", "2048x1152", "3840x2160", "2160x3840"} and selected_model != "gpt-image-2":
        raise ProviderError("The selected 2K or 4K size requires gpt-image-2.")
    if quality not in {"auto", "low", "medium", "high"}:
        raise ProviderError("The selected image quality is not supported.")
    if output_format not in {"png", "jpeg", "webp"}:
        raise ProviderError("The selected image format is not supported.")
    if background not in {"auto", "opaque", "transparent"}:
        raise ProviderError("The selected background is not supported.")
    if selected_model == "gpt-image-2" and background == "transparent":
        raise ProviderError("Transparent backgrounds are not supported by gpt-image-2.")
    if moderation not in {"auto", "low"}:
        raise ProviderError("The selected image moderation level is not supported.")
    compression = max(0, min(int(output_compression), 100))
    options = {
        "model": selected_model,
        "prompt": clean_prompt,
        "size": size,
        "quality": quality,
        "output_format": output_format,
        "background": background,
        "moderation": moderation,
    }
    if output_format in {"jpeg", "webp"}:
        options["output_compression"] = compression
    try:
        if references:
            response = requests.post(
                f"{settings.OPENAI_BASE_URL}/images/edits",
                headers=_openai_headers(),
                data=options,
                files=[("image[]", (filename, content, content_type)) for filename, content, content_type in references],
                timeout=180,
            )
        else:
            response = requests.post(
                f"{settings.OPENAI_BASE_URL}/images/generations",
                headers={**_openai_headers(), "Content-Type": "application/json"},
                json=options,
                timeout=180,
            )
        _safe_response(response)
        payload = response.json()
        usage = _extract_usage(payload)
        result = (payload.get("data") or [{}])[0]
        encoded = str(result.get("b64_json") or "")
        if encoded:
            image_bytes = base64.b64decode(encoded, validate=True)
        elif result.get("url"):
            download = requests.get(str(result["url"]), timeout=60)
            _safe_response(download)
            image_bytes = download.content
        else:
            image_bytes = b""
    except (requests.RequestException, ValueError, KeyError, IndexError) as exc:
        raise ProviderError("Image generation is unavailable.") from exc
    if not image_bytes:
        raise ProviderError("The AI provider returned an empty image.")
    return image_bytes, selected_model, usage


def _find_inline_media(value):
    if isinstance(value, dict):
        for key in ("inlineData", "inline_data", "output_image", "image"):
            item = value.get(key)
            if isinstance(item, dict) and (item.get("data") or item.get("bytesBase64Encoded")):
                return item
        for child in value.values():
            found = _find_inline_media(child)
            if found:
                return found
    elif isinstance(value, list):
        for child in value:
            found = _find_inline_media(child)
            if found:
                return found
    return None


def generate_google_image_with_usage(prompt: str, *, model: str, reference_images=None, aspect_ratio="1:1", size="1K", **_options):
    selected_model = require_model(model, GOOGLE_IMAGE_MODELS, "gemini-3.1-flash-lite-image")
    clean_prompt = prompt.strip()
    if not clean_prompt or len(clean_prompt) > settings.AI_MAX_TEXT_CHARS:
        raise ProviderError("Image prompt is empty or exceeds the server limit.")
    content = [{"text": clean_prompt}]
    for filename, image_bytes, content_type in list(reference_images or [])[:14]:
        if not image_bytes or len(image_bytes) > settings.AI_MAX_IMAGE_BYTES:
            raise ProviderError(f"Reference image {filename} is empty or exceeds the server limit.")
        content.append({"inlineData": {"mimeType": content_type if content_type.startswith("image/") else "image/jpeg", "data": base64.b64encode(image_bytes).decode("ascii")}})
    try:
        response = requests.post(
            f"{settings.GOOGLE_AI_BASE_URL}/models/{selected_model}:generateContent", headers=_google_headers(),
            json={
                "contents": [{"role": "user", "parts": content}],
                "generationConfig": {
                    "responseModalities": ["TEXT", "IMAGE"],
                    "imageConfig": {"aspectRatio": aspect_ratio, "imageSize": size},
                },
            },
            timeout=300,
        )
        _safe_response(response)
        payload = response.json()
        media_item = _find_inline_media(payload) or {}
        encoded = media_item.get("data") or media_item.get("bytesBase64Encoded")
        media = base64.b64decode(encoded, validate=True) if encoded else b""
    except (requests.RequestException, ValueError) as exc:
        raise ProviderError("Google image generation is unavailable.") from exc
    if not media:
        raise ProviderError("Google AI Studio returned an empty image.")
    return media, selected_model, _extract_google_usage(payload), "image/png", "png"


def generate_google_video_with_usage(
    prompt: str, *, model: str, reference_images=None, first_frame_image=None,
    last_frame_image=None, reference_mode="FRAMES", aspect_ratio="16:9",
    size="720p", duration_seconds=8, **_options,
):
    selected_model = require_model(model, GOOGLE_VIDEO_MODELS, "veo-3.1-lite-generate-preview")
    instance = {"prompt": prompt.strip()}
    references = list(reference_images or [])[:3]
    use_frames = str(reference_mode).upper() == "FRAMES"
    if use_frames and (first_frame_image or references):
        _, image_bytes, content_type = first_frame_image or references[0]
        instance["image"] = {"bytesBase64Encoded": base64.b64encode(image_bytes).decode("ascii"), "mimeType": content_type if content_type.startswith("image/") else "image/jpeg"}
    if use_frames and last_frame_image:
        _, image_bytes, content_type = last_frame_image
        instance["lastFrame"] = {"bytesBase64Encoded": base64.b64encode(image_bytes).decode("ascii"), "mimeType": content_type if content_type.startswith("image/") else "image/jpeg"}
    parameters = {
        "aspectRatio": aspect_ratio,
        "resolution": size,
        "sampleCount": 1,
        "durationSeconds": max(4, min(int(duration_seconds), 8)),
    }
    if references and not use_frames:
        parameters["referenceImages"] = [{"image": {"bytesBase64Encoded": base64.b64encode(data).decode("ascii"), "mimeType": mime if mime.startswith("image/") else "image/jpeg"}, "referenceType": "asset"} for _, data, mime in references]
    try:
        response = requests.post(
            f"{settings.GOOGLE_AI_BASE_URL}/models/{selected_model}:predictLongRunning",
            headers=_google_headers(), json={"instances": [instance], "parameters": parameters}, timeout=90,
        )
        _safe_response(response)
        operation_name = str(response.json().get("name") or "")
        if not operation_name:
            raise ProviderError("Google video generation did not return an operation ID.")
        deadline = time.monotonic() + 900
        while time.monotonic() < deadline:
            poll = requests.get(f"{settings.GOOGLE_AI_BASE_URL}/{operation_name}", headers=_google_headers(), timeout=60)
            _safe_response(poll)
            payload = poll.json()
            if payload.get("done"):
                if payload.get("error"):
                    raise ProviderError(str(payload["error"].get("message") or "Google video generation failed."))
                samples = payload.get("response", {}).get("generateVideoResponse", {}).get("generatedSamples", [])
                uri = str((samples[0].get("video") or {}).get("uri") or "") if samples else ""
                if not uri:
                    raise ProviderError("Google AI Studio returned an empty video.")
                download = requests.get(uri, headers={"x-goog-api-key": _google_headers()["x-goog-api-key"]}, timeout=300)
                _safe_response(download)
                return download.content, selected_model, _extract_google_usage(payload), "video/mp4", "mp4"
            time.sleep(8)
    except ProviderError:
        raise
    except (requests.RequestException, ValueError, IndexError) as exc:
        raise ProviderError("Google video generation is unavailable.") from exc
    raise ProviderError("Google video generation timed out.")


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
