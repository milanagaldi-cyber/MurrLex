import base64
import json
import os
import uuid

from django.conf import settings
from django.contrib import messages
from django.contrib.auth import login
from django.contrib.auth.decorators import login_required
from django.http import HttpResponse, HttpResponseForbidden, JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods

from .forms import (
    AccountSettingsForm,
    CardForm,
    ImageTextForm,
    LessonForm,
    ProviderCredentialForm,
    PublicRegistrationForm,
    SpeechForm,
    TranscriptionForm,
    TranslationForm,
)
from .models import Card, ImportLog, Lesson, ProviderCredential, UserApiAccess
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
from .provider_credentials import user_has_ai_access
from .social_auth import GoogleIdentityError, user_for_google_claims, verify_google_id_token
from .sync_service import (
    mark_card_updated,
    mark_lesson_updated,
    merge_mobile_lessons,
    user_lessons_payload,
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
    if not settings.PUBLIC_SIGNUP_ENABLED:
        site_links = [link for link in site_links if link["href"] != "/register/"]
    return render(request, "lessons/home.html", {"site_links": site_links})


@require_http_methods(["GET"])
def admin_help(request):
    return render(request, "admin/help.html")


@require_http_methods(["GET", "POST"])
def register(request):
    if request.user.is_authenticated:
        return redirect("account")

    if not settings.PUBLIC_SIGNUP_ENABLED:
        return render(request, "registration/register.html", {"signup_closed": True}, status=403)

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
    api_access, _ = UserApiAccess.objects.get_or_create(user=request.user)
    return render(request, "registration/account.html", {
        "api_access": api_access,
        "ai_access": user_has_ai_access(request.user),
    })


def _web_ai_denied(request):
    return None if user_has_ai_access(request.user) else HttpResponseForbidden("AI application access is required.")


@login_required
@require_http_methods(["GET"])
def apps_dashboard(request):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    return render(request, "apps/dashboard.html")


@login_required
@require_http_methods(["GET"])
def web_lessons(request):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    query = request.GET.get("q", "").strip()
    lessons = Lesson.objects.filter(owner=request.user).prefetch_related("cards")
    if query:
        lessons = lessons.filter(title__icontains=query)
    return render(request, "apps/lessons.html", {"lessons": lessons, "query": query})


@login_required
@require_http_methods(["GET", "POST"])
def web_lesson_edit(request, lesson_id=None):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    lesson = get_object_or_404(Lesson, owner=request.user, id=lesson_id) if lesson_id else None
    form = LessonForm(request.POST or None, instance=lesson)
    if request.method == "POST" and form.is_valid():
        saved = form.save(commit=False)
        saved.owner = request.user
        saved.external_id = saved.external_id or uuid.uuid4().hex
        saved.save()
        mark_lesson_updated(saved)
        messages.success(request, "Lesson saved.")
        return redirect("web_lesson_detail", lesson_id=saved.id)
    return render(request, "apps/form.html", {"form": form, "title": "Edit lesson" if lesson else "Create lesson"})


@login_required
@require_http_methods(["GET"])
def web_lesson_detail(request, lesson_id):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    lesson = get_object_or_404(Lesson.objects.prefetch_related("cards"), owner=request.user, id=lesson_id)
    return render(request, "apps/lesson_detail.html", {"lesson": lesson})


@login_required
@require_http_methods(["GET", "POST"])
def web_study(request, lesson_id):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    lesson = get_object_or_404(Lesson.objects.prefetch_related("cards"), owner=request.user, id=lesson_id)
    cards = list(lesson.cards.all())
    if request.method == "POST":
        card = get_object_or_404(Card, lesson=lesson, id=request.POST.get("card_id"))
        rating = max(0, min(3, int(request.POST.get("rating", 0))))
        card.stars = (0, 1, 3, 7)[rating]
        mark_card_updated(card)
        messages.success(request, "Progress saved.")
    index = max(0, min(len(cards) - 1, int(request.GET.get("index", 0)))) if cards else 0
    return render(
        request,
        "apps/study.html",
        {"lesson": lesson, "card": cards[index] if cards else None, "index": index, "total": len(cards)},
    )


@login_required
@require_http_methods(["GET", "POST"])
def web_card_edit(request, lesson_id, card_id=None):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    lesson = get_object_or_404(Lesson, owner=request.user, id=lesson_id)
    card = get_object_or_404(Card, lesson=lesson, id=card_id) if card_id else None
    form = CardForm(request.POST or None, instance=card)
    if request.method == "POST" and form.is_valid():
        saved = form.save(commit=False)
        saved.lesson = lesson
        saved.external_card_id = saved.external_card_id or str((lesson.cards.count() or 0) + 1)
        saved.save()
        mark_card_updated(saved)
        messages.success(request, "Card saved.")
        return redirect("web_lesson_detail", lesson_id=lesson.id)
    return render(request, "apps/form.html", {"form": form, "title": "Edit card" if card else "Create card"})


@login_required
@require_http_methods(["POST"])
def web_delete(request, object_type, object_id):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    if object_type == "lesson":
        get_object_or_404(Lesson, owner=request.user, id=object_id).delete()
        return redirect("web_lessons")
    card = get_object_or_404(Card, lesson__owner=request.user, id=object_id)
    lesson_id = card.lesson_id
    card.delete()
    return redirect("web_lesson_detail", lesson_id=lesson_id)


@login_required
@require_http_methods(["GET", "POST"])
def web_translate(request):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    output = ""
    form = TranslationForm(request.POST or None)
    if request.method == "POST" and form.is_valid():
        prompt = (
            f"Translate from {form.cleaned_data['source_language']} to {form.cleaned_data['target_language']}. "
            f"Return only the translation.\n\n{form.cleaned_data['text']}"
        )
        try:
            output, _ = run_text(settings.OPENAI_TRANSLATION_MODEL, prompt)
        except ProviderError as exc:
            messages.error(request, str(exc))
    return render(request, "apps/tool.html", {"form": form, "title": "Translator", "output": output})


@login_required
@require_http_methods(["GET", "POST"])
def web_speech(request):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    audio_data = ""
    form = SpeechForm(request.POST or None)
    if request.method == "POST" and form.is_valid():
        try:
            audio, _, content_type = synthesize_openai(
                form.cleaned_data["model"], form.cleaned_data["voice"], form.cleaned_data["text"], 1.0
            )
            audio_data = f"data:{content_type};base64,{base64.b64encode(audio).decode('ascii')}"
        except ProviderError as exc:
            messages.error(request, str(exc))
    return render(request, "apps/tool.html", {"form": form, "title": "Text to speech", "audio_data": audio_data})


@login_required
@require_http_methods(["GET", "POST"])
def web_transcribe(request):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    output = ""
    form = TranscriptionForm(request.POST or None, request.FILES or None)
    if request.method == "POST" and form.is_valid():
        try:
            output, _ = transcribe(form.cleaned_data["model"], form.cleaned_data["language"], form.cleaned_data["audio"])
        except ProviderError as exc:
            messages.error(request, str(exc))
    return render(request, "apps/tool.html", {"form": form, "title": "Speech to text", "output": output, "multipart": True})


@login_required
@require_http_methods(["GET", "POST"])
def web_image_text(request):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    output = ""
    form = ImageTextForm(request.POST or None, request.FILES or None)
    if request.method == "POST" and form.is_valid():
        try:
            output, _ = recognize_image(form.cleaned_data["model"], "Extract all readable text.", form.cleaned_data["image"])
        except ProviderError as exc:
            messages.error(request, str(exc))
    return render(request, "apps/tool.html", {"form": form, "title": "Image text", "output": output, "multipart": True})


@login_required
@require_http_methods(["GET", "POST"])
def web_sync(request):
    denied = _web_ai_denied(request)
    if denied:
        return denied
    if request.method == "POST":
        scope = request.POST.get("scope", "all")
        lesson_id = request.POST.get("lesson_id", "")
        card_id = request.POST.get("card_id", "")
        if scope == "card" and card_id:
            mark_card_updated(get_object_or_404(Card, id=card_id, lesson__owner=request.user))
        elif scope == "lesson" and lesson_id:
            mark_lesson_updated(get_object_or_404(Lesson, id=lesson_id, owner=request.user))
        else:
            for lesson in Lesson.objects.filter(owner=request.user):
                mark_lesson_updated(lesson)
        messages.success(request, "Sync checkpoint updated. The phone will receive it on the next synchronization.")
        return redirect("web_sync")
    return render(request, "apps/sync.html", {"lessons": Lesson.objects.filter(owner=request.user).prefetch_related("cards")})


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


@login_required
@require_http_methods(["GET", "POST"])
def provider_credentials(request):
    if not request.user.is_superuser:
        return HttpResponseForbidden("Administrator access is required.")

    if request.method == "POST":
        form = ProviderCredentialForm(request.POST)
        if form.is_valid():
            credential, _ = ProviderCredential.objects.get_or_create(provider=form.cleaned_data["provider"])
            if form.cleaned_data["clear_key"]:
                credential.clear_api_key()
            else:
                credential.set_api_key(form.cleaned_data["api_key"])
            credential.updated_by = request.user
            credential.save()
            messages.success(request, "Provider key settings saved.")
            return redirect("provider_credentials")
    else:
        form = ProviderCredentialForm()

    credentials = {item.provider: item for item in ProviderCredential.objects.all()}
    provider_rows = [
        {
            "value": value,
            "label": label,
            "configured": credentials.get(value).is_configured if value in credentials else False,
            "updated_at": credentials.get(value).updated_at if value in credentials else None,
        }
        for value, label in ProviderCredential.Provider.choices
    ]
    return render(request, "registration/provider_credentials.html", {"form": form, "provider_rows": provider_rows})


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

@require_http_methods(["GET"])
def api_ready(request):
    from django.db import connection

    checks = {"database": False, "private_storage": False}
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
            checks["database"] = cursor.fetchone()[0] == 1
    except Exception:
        pass
    try:
        root = settings.STUDIO_PRIVATE_MEDIA_ROOT
        root.mkdir(parents=True, exist_ok=True)
        checks["private_storage"] = root.is_dir() and os.access(root, os.R_OK | os.W_OK)
    except OSError:
        pass
    ready = all(checks.values())
    return JsonResponse(
        {"status": "ready" if ready else "unavailable", "checks": checks},
        status=200 if ready else 503,
    )

def _api_payload(request):
    try:
        payload = json.loads(request.body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        return None
    return payload if isinstance(payload, dict) else None


def _mobile_user_or_error(request, require_ai=False):
    user = authenticate_mobile_request(request)
    if user is None:
        return json_error("Login is required.", 401)
    if require_ai and not user_has_ai_access(user):
        return json_error("AI access is not enabled for this account.", 403)
    return user


@csrf_exempt
@require_http_methods(["POST"])
def api_register(request):
    if not settings.PUBLIC_SIGNUP_ENABLED:
        return json_error("Registration is closed. Ask an administrator for access.", 403)
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
def api_google_login(request):
    if not settings.GOOGLE_OAUTH_ENABLED:
        return JsonResponse({"detail": "Google sign-in is disabled."}, status=403)
    payload = _api_payload(request)
    if payload is None:
        return JsonResponse({"detail": "Invalid JSON."}, status=400)
    try:
        claims = verify_google_id_token(str(payload.get("id_token", "")))
        user = user_for_google_claims(claims)
    except GoogleIdentityError as exc:
        return JsonResponse({"detail": str(exc)}, status=401)
    except PermissionError as exc:
        return JsonResponse({"detail": str(exc)}, status=403)
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
    user_or_error = _mobile_user_or_error(request, require_ai=True)
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
    user_or_error = _mobile_user_or_error(request, require_ai=True)
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
    user_or_error = _mobile_user_or_error(request, require_ai=True)
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
    user_or_error = _mobile_user_or_error(request, require_ai=True)
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
@require_http_methods(["GET", "POST"])
def api_sync(request):
    user_or_error = _mobile_user_or_error(request, require_ai=True)
    if isinstance(user_or_error, JsonResponse):
        return user_or_error
    scope = request.GET.get("scope", "all") if request.method == "GET" else "all"
    lesson_id = request.GET.get("lessonId", "") if request.method == "GET" else ""
    card_id = request.GET.get("cardId", "") if request.method == "GET" else ""
    if request.method == "POST":
        payload = _api_payload(request)
        if payload is None:
            return json_error("Invalid JSON.")
        scope = str(payload.get("scope", "all"))
        lesson_id = str(payload.get("lessonId", ""))
        card_id = str(payload.get("cardId", ""))
        lessons = payload.get("lessons", [])
        if not isinstance(lessons, list):
            return json_error("lessons must be a list.")
        merge_mobile_lessons(user_or_error, lessons, scope, lesson_id, card_id)
    if scope not in {"all", "lesson", "card"}:
        return json_error("Unknown sync scope.")
    response_lessons = user_lessons_payload(
        user_or_error,
        lesson_id=lesson_id if scope in {"lesson", "card"} else "",
        card_id=card_id if scope == "card" else "",
    )
    return JsonResponse({"status": "ok", "scope": scope, "lessons": response_lessons})


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

    try:
        payload = json.loads(request.body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        return JsonResponse({"status": "error", "error": f"Invalid JSON: {exc}"}, status=400)

    text = str(payload.get("text", "")).strip()
    source_language = str(payload.get("sourceLanguage", "")).strip() or "source language"
    target_language = str(payload.get("targetLanguage", "")).strip() or "target language"
    if not text:
        return JsonResponse({"status": "error", "error": "Text is required."}, status=400)

    try:
        translation, _ = run_text(
            settings.OPENAI_TRANSLATION_MODEL,
            "Translate the text from "
            f"{source_language} to {target_language}. Return only the translation, without comments or alternatives.\n\n"
            f"Text: {text}",
        )
    except ProviderError as exc:
        return JsonResponse({"status": "error", "error": str(exc)}, status=503)
    return JsonResponse({"status": "ok", "translation": translation})
