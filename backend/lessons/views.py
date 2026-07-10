import json
import urllib.error
import urllib.request

from django.conf import settings
from django.contrib import messages
from django.contrib.auth import login
from django.contrib.auth import get_user_model
from django.contrib.auth.decorators import login_required
from django.http import HttpResponse, JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods

from .forms import AccountSettingsForm, PublicRegistrationForm
from .models import ImportLog, Lesson
from .services import LessonImportError, import_lesson_payload, log_failed_import
from .ai_gateway import ProviderError, recognize_image, run_text, synthesize_elevenlabs, synthesize_openai, transcribe
from .api_auth import (
    authenticate_login,
    authenticate_mobile_request,
    create_session,
    json_error,
    revoke_refresh_session,
    rotate_refresh_session,
    token_payload,
)


@require_http_methods(["GET"])
def home(request):
    site_links = [
        {
            "label": "Health check",
            "href": "/api/health",
            "access": "Public",
            "description": "Small JSON endpoint for checking that the backend is alive.",
        },
        {
            "label": "Login",
            "href": "/login/",
            "access": "Public",
            "description": "Sign in to a public user account.",
        },
        {
            "label": "Register",
            "href": "/register/",
            "access": "Public",
            "description": "Create a public MurrLex account.",
        },
        {
            "label": "User account",
            "href": "/account/",
            "access": "Login",
            "description": "A first version of the personal cabinet for signed-in users.",
        },
        {
            "label": "Account settings",
            "href": "/account/settings/",
            "access": "Login",
            "description": "Update profile basics and find password tools.",
        },
        {
            "label": "Premium",
            "href": "/premium/",
            "access": "Preview",
            "description": "Placeholder for future paid features.",
        },
        {
            "label": "Django admin",
            "href": "/admin/",
            "access": "Password",
            "description": "Admin area for managing users, lessons, cards, and import logs.",
        },
        {
            "label": "Import JSON",
            "href": "/lab/import-json/",
            "access": "Password",
            "description": "Lab tool for importing lesson JSON by hand.",
        },
        {
            "label": "Lessons",
            "href": "/lab/lessons/",
            "access": "Password",
            "description": "Review imported lessons and their cards.",
        },
        {
            "label": "Import logs",
            "href": "/lab/imports/",
            "access": "Password",
            "description": "Review recent import attempts and errors.",
        },
        {
            "label": "Internal lesson import API",
            "href": "/api/internal/import-lesson",
            "access": "Bearer token",
            "description": "Machine endpoint used by connector scripts to send lesson payloads.",
        },
        {
            "label": "Translation API",
            "href": "/api/translate",
            "access": "Bearer token",
            "description": "Machine endpoint for text translation requests.",
        },
    ]
    return render(request, "lessons/home.html", {"site_links": site_links})


@require_http_methods(["GET", "POST"])
def register(request):
    if request.user.is_authenticated:
        return redirect("account")

    if request.method == "POST":
        form = PublicRegistrationForm(request.POST)
        if form.is_valid():
            user = form.save()
            login(request, user, backend="django.contrib.auth.backends.ModelBackend")
            messages.success(request, "Your account has been created.")
            return redirect("account")
    else:
        form = PublicRegistrationForm()

    return render(request, "registration/register.html", {"form": form})


@login_required
@require_http_methods(["GET"])
def account(request):
    return render(request, "registration/account.html")


@login_required
@require_http_methods(["GET", "POST"])
def account_settings(request):
    if request.method == "POST":
        form = AccountSettingsForm(request.POST, instance=request.user)
        if form.is_valid():
            form.save()
            messages.success(request, "Account settings updated.")
            return redirect("account")
    else:
        form = AccountSettingsForm(instance=request.user)

    return render(request, "registration/account_settings.html", {"form": form})


@require_http_methods(["GET"])
def premium(request):
    return render(request, "registration/premium.html")


@require_http_methods(["GET", "POST"])
@login_required
def import_json(request):
    json_text = ""

    if request.method == "POST":
        json_text = request.POST.get("json_payload", "").strip()
        try:
            payload = json.loads(json_text)
            result = import_lesson_payload(payload, source="lab")
        except json.JSONDecodeError as exc:
            log_failed_import(
                source="lab",
                raw_payload={"raw": json_text},
                error_message=f"Invalid JSON: {exc}",
            )
            messages.error(request, f"Invalid JSON: {exc}")
        except LessonImportError as exc:
            external_lesson_id = ""
            try:
                parsed_payload = json.loads(json_text)
                if isinstance(parsed_payload, dict):
                    external_lesson_id = str(parsed_payload.get("id", ""))
            except json.JSONDecodeError:
                parsed_payload = {"raw": json_text}
            log_failed_import(
                source="lab",
                raw_payload=parsed_payload,
                error_message=str(exc),
                external_lesson_id=external_lesson_id,
            )
            messages.error(request, str(exc))
        else:
            messages.success(
                request,
                f"Imported '{result.lesson.title}' with {result.cards_imported} cards.",
            )
            return redirect("lessons:lesson_detail", lesson_id=result.lesson.id)

    return render(request, "lessons/import_json.html", {"json_text": json_text})


def lesson_list(request):
    if not request.user.is_authenticated:
        return redirect(f"{settings.LOGIN_URL}?next={request.path}")
    lessons = Lesson.objects.prefetch_related("cards").all()
    return render(request, "lessons/lesson_list.html", {"lessons": lessons})


@login_required
def lesson_detail(request, lesson_id: int):
    lesson = get_object_or_404(Lesson.objects.prefetch_related("cards"), id=lesson_id)
    return render(request, "lessons/lesson_detail.html", {"lesson": lesson})


def import_log_list(request):
    if not request.user.is_authenticated:
        return redirect(f"{settings.LOGIN_URL}?next={request.path}")
    import_logs = ImportLog.objects.all()[:100]
    return render(request, "lessons/import_log_list.html", {"import_logs": import_logs})


@require_http_methods(["GET"])
def api_health(request):
    return JsonResponse({"status": "ok", "service": "murrlex-ai-gateway"})


def _api_payload(request):
    try:
        payload = json.loads(request.body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        return None
    return payload if isinstance(payload, dict) else None


def _mobile_user_or_error(request):
    user = authenticate_mobile_request(request)
    return user if user is not None else json_error("Login is required.", 401)


@csrf_exempt
@require_http_methods(["POST"])
def api_register(request):
    payload = _api_payload(request)
    if payload is None:
        return json_error("Invalid JSON.")
    form = PublicRegistrationForm(
        {
            "username": str(payload.get("username", "")).strip(),
            "email": str(payload.get("email", "")).strip(),
            "password1": str(payload.get("password", "")),
            "password2": str(payload.get("password", "")),
        }
    )
    if not form.is_valid():
        return JsonResponse(
            {"status": "error", "error": "Registration details are invalid.", "fields": form.errors.get_json_data()},
            status=400,
        )
    user = form.save()
    session, refresh_token = create_session(user, str(payload.get("deviceName", "")))
    return JsonResponse(token_payload(user, session, refresh_token), status=201)


@csrf_exempt
@require_http_methods(["POST"])
def api_login(request):
    payload = _api_payload(request)
    if payload is None:
        return json_error("Invalid JSON.")
    user = authenticate_login(str(payload.get("login", "")).strip(), str(payload.get("password", "")))
    if user is None:
        return json_error("Incorrect login or password.", 401)
    session, refresh_token = create_session(user, str(payload.get("deviceName", "")))
    return JsonResponse(token_payload(user, session, refresh_token))


@csrf_exempt
@require_http_methods(["POST"])
def api_refresh(request):
    payload = _api_payload(request)
    if payload is None:
        return json_error("Invalid JSON.")
    rotated = rotate_refresh_session(str(payload.get("refreshToken", "")))
    if rotated is None:
        return json_error("Session has expired. Please log in again.", 401)
    session, refresh_token = rotated
    return JsonResponse(token_payload(session.user, session, refresh_token))


@csrf_exempt
@require_http_methods(["POST"])
def api_logout(request):
    payload = _api_payload(request)
    if payload is not None:
        revoke_refresh_session(str(payload.get("refreshToken", "")))
    return JsonResponse({"status": "ok"})


@csrf_exempt
@require_http_methods(["POST"])
def api_text(request):
    user_or_error = _mobile_user_or_error(request)
    if isinstance(user_or_error, JsonResponse):
        return user_or_error
    payload = _api_payload(request)
    if payload is None:
        return json_error("Invalid JSON.")
    try:
        output, model = run_text(str(payload.get("model", "")), str(payload.get("prompt", "")))
    except ProviderError as exc:
        return json_error(str(exc), 503)
    return JsonResponse({"status": "ok", "output": output, "provider": "openai", "model": model})


@csrf_exempt
@require_http_methods(["POST"])
def api_transcribe(request):
    user_or_error = _mobile_user_or_error(request)
    if isinstance(user_or_error, JsonResponse):
        return user_or_error
    audio = request.FILES.get("audio") or request.FILES.get("file")
    if audio is None:
        return json_error("Audio file is required.")
    try:
        text, model = transcribe(request.POST.get("model", ""), request.POST.get("language", ""), audio)
    except ProviderError as exc:
        return json_error(str(exc), 503)
    return JsonResponse({"status": "ok", "text": text, "provider": "openai", "model": model})


@csrf_exempt
@require_http_methods(["POST"])
def api_speech(request):
    user_or_error = _mobile_user_or_error(request)
    if isinstance(user_or_error, JsonResponse):
        return user_or_error
    payload = _api_payload(request)
    if payload is None:
        return json_error("Invalid JSON.")
    provider = str(payload.get("provider", "openai")).strip().lower()
    try:
        if provider == "elevenlabs":
            audio, model, voice = synthesize_elevenlabs(
                str(payload.get("model", "eleven_v3")),
                str(payload.get("voiceId", "")),
                str(payload.get("text", "")),
            )
        else:
            audio, model, voice = synthesize_openai(
                str(payload.get("model", "")),
                str(payload.get("voice", "")),
                str(payload.get("text", "")),
                payload.get("speed", 1.0),
            )
            provider = "openai"
    except ProviderError as exc:
        return json_error(str(exc), 503)
    response = HttpResponse(audio, content_type="audio/mpeg")
    response["X-MurrLex-Provider"] = provider
    response["X-MurrLex-Model"] = model
    response["X-MurrLex-Voice"] = voice
    response["Cache-Control"] = "private, no-store"
    return response


@csrf_exempt
@require_http_methods(["POST"])
def api_image_text(request):
    user_or_error = _mobile_user_or_error(request)
    if isinstance(user_or_error, JsonResponse):
        return user_or_error
    image = request.FILES.get("image") or request.FILES.get("file")
    if image is None:
        return json_error("Image file is required.")
    try:
        text, model = recognize_image(request.POST.get("model", ""), request.POST.get("prompt", ""), image)
    except ProviderError as exc:
        return json_error(str(exc), 503)
    return JsonResponse({"status": "ok", "text": text, "provider": "openai", "model": model})


@csrf_exempt
@require_http_methods(["POST"])
def internal_import_lesson(request):
    expected_token = settings.INTERNAL_IMPORT_TOKEN
    if not expected_token:
        return JsonResponse(
            {"status": "error", "error": "INTERNAL_IMPORT_TOKEN is not configured."},
            status=500,
        )

    auth_header = request.headers.get("Authorization", "")
    if auth_header != f"Bearer {expected_token}":
        return JsonResponse({"status": "error", "error": "Unauthorized."}, status=401)

    try:
        payload = json.loads(request.body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        log_failed_import(
            source="api",
            raw_payload={"raw": request.body.decode("utf-8", errors="replace")},
            error_message=f"Invalid JSON: {exc}",
        )
        return JsonResponse({"status": "error", "error": f"Invalid JSON: {exc}"}, status=400)

    try:
        result = import_lesson_payload(payload, source="api")
    except LessonImportError as exc:
        external_lesson_id = str(payload.get("id", "")) if isinstance(payload, dict) else ""
        log_failed_import(
            source="api",
            raw_payload=payload,
            error_message=str(exc),
            external_lesson_id=external_lesson_id,
        )
        return JsonResponse({"status": "error", "error": str(exc)}, status=400)

    return JsonResponse(
        {
            "status": "ok",
            "lessonId": result.lesson.external_id,
            "cardsImported": result.cards_imported,
        }
    )

@csrf_exempt
@require_http_methods(["POST"])
def translate_text(request):
    expected_token = settings.INTERNAL_IMPORT_TOKEN
    if not expected_token:
        return JsonResponse(
            {"status": "error", "error": "INTERNAL_IMPORT_TOKEN is not configured."},
            status=500,
        )

    auth_header = request.headers.get("Authorization", "")
    if auth_header != f"Bearer {expected_token}":
        return JsonResponse({"status": "error", "error": "Unauthorized."}, status=401)

    if not settings.OPENAI_API_KEY:
        return JsonResponse({"status": "error", "error": "OPENAI_API_KEY is not configured."}, status=503)

    try:
        payload = json.loads(request.body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        return JsonResponse({"status": "error", "error": f"Invalid JSON: {exc}"}, status=400)

    text = str(payload.get("text", "")).strip()
    source_language = str(payload.get("sourceLanguage", "")).strip() or "source language"
    target_language = str(payload.get("targetLanguage", "")).strip() or "target language"
    if not text:
        return JsonResponse({"status": "error", "error": "Text is required."}, status=400)

    request_body = {
        "model": settings.OPENAI_TRANSLATION_MODEL,
        "input": (
            "Translate the text from "
            f"{source_language} to {target_language}. Return only the translation, "
            "without comments or alternatives.\n\n"
            f"Text: {text}"
        ),
    }
    request_data = json.dumps(request_body).encode("utf-8")
    openai_request = urllib.request.Request(
        "https://api.openai.com/v1/responses",
        data=request_data,
        method="POST",
        headers={
            "Authorization": f"Bearer {settings.OPENAI_API_KEY}",
            "Content-Type": "application/json",
        },
    )

    try:
        with urllib.request.urlopen(openai_request, timeout=30) as response:
            response_payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        error_text = exc.read().decode("utf-8", errors="replace")
        return JsonResponse({"status": "error", "error": error_text}, status=502)
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        return JsonResponse({"status": "error", "error": str(exc)}, status=502)

    translation = _extract_openai_text(response_payload).strip()
    if not translation:
        return JsonResponse({"status": "error", "error": "Translation response was empty."}, status=502)
    return JsonResponse({"status": "ok", "translation": translation})


def _extract_openai_text(payload):
    output_text = payload.get("output_text")
    if isinstance(output_text, str) and output_text.strip():
        return output_text

    parts = []
    for item in payload.get("output", []):
        if not isinstance(item, dict):
            continue
        for content in item.get("content", []):
            if not isinstance(content, dict):
                continue
            text = content.get("text")
            if isinstance(text, str):
                parts.append(text)
    return "".join(parts)
