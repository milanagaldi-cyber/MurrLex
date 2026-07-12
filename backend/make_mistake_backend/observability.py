import json
import logging
import re
import time
import uuid
from contextvars import ContextVar
from datetime import datetime, timezone

REQUEST_ID_RE = re.compile(r"^[A-Za-z0-9._-]{8,80}$")
request_logger = logging.getLogger("murrlex.request")
_current_request_id = ContextVar("request_id", default="")


def current_request_id():
    return _current_request_id.get()


class RequestIdMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        supplied = request.headers.get("X-Request-ID", "")
        request.request_id = supplied if REQUEST_ID_RE.fullmatch(supplied) else uuid.uuid4().hex
        token = _current_request_id.set(request.request_id)
        started = time.monotonic()
        try:
            response = self.get_response(request)
            response["X-Request-ID"] = request.request_id
            request_logger.info(
                "request_complete",
                extra={
                    "request_id": request.request_id,
                    "method": request.method,
                    "path": request.path,
                    "status_code": response.status_code,
                    "duration_ms": round((time.monotonic() - started) * 1000, 2),
                },
            )
            return response
        finally:
            _current_request_id.reset(token)


class JsonFormatter(logging.Formatter):
    def format(self, record):
        payload = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }
        request_id = getattr(record, "request_id", None)
        if not request_id and getattr(record, "request", None):
            request_id = getattr(record.request, "request_id", None)
        if request_id:
            payload["request_id"] = request_id
        for field in ("method", "path", "status_code", "duration_ms"):
            value = getattr(record, field, None)
            if value is not None:
                payload[field] = value
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=True)
