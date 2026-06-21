import json

from django.conf import settings
from django.contrib import messages
from django.http import JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods

from .models import ImportLog, Lesson
from .services import LessonImportError, import_lesson_payload, log_failed_import


@require_http_methods(["GET", "POST"])
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
    lessons = Lesson.objects.prefetch_related("cards").all()
    return render(request, "lessons/lesson_list.html", {"lessons": lessons})


def lesson_detail(request, lesson_id: int):
    lesson = get_object_or_404(Lesson.objects.prefetch_related("cards"), id=lesson_id)
    return render(request, "lessons/lesson_detail.html", {"lesson": lesson})


def import_log_list(request):
    import_logs = ImportLog.objects.all()[:100]
    return render(request, "lessons/import_log_list.html", {"import_logs": import_logs})


@require_http_methods(["GET"])
def api_health(request):
    return JsonResponse({"status": "ok", "service": "make-mistake-backend"})


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
