import json
import os
from pathlib import Path
from urllib.request import Request, urlopen


DEFAULT_TIMEOUT_SECONDS = 20


class ConnectorConfigError(ValueError):
    pass


def save_lesson_to_make_mistakes(
    lesson: dict,
    *,
    import_url: str | None = None,
    import_token: str | None = None,
    timeout_seconds: int = DEFAULT_TIMEOUT_SECONDS,
) -> dict:
    load_default_env_files()
    resolved_import_url = (import_url or os.environ.get("DJANGO_IMPORT_URL", "")).strip()
    resolved_import_token = (import_token or os.environ.get("INTERNAL_IMPORT_TOKEN", "")).strip()

    if not resolved_import_url:
        raise ConnectorConfigError("DJANGO_IMPORT_URL is required.")
    if not resolved_import_token:
        raise ConnectorConfigError("INTERNAL_IMPORT_TOKEN is required.")
    if not isinstance(lesson, dict):
        raise ValueError("Lesson must be a dict.")

    return send_lesson(
        import_url=resolved_import_url,
        import_token=resolved_import_token,
        payload=lesson,
        timeout_seconds=timeout_seconds,
    )


def send_lesson(
    *,
    import_url: str,
    import_token: str,
    payload: dict,
    timeout_seconds: int = DEFAULT_TIMEOUT_SECONDS,
) -> dict:
    body = json.dumps(payload).encode("utf-8")
    request = Request(
        import_url,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {import_token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )

    with urlopen(request, timeout=timeout_seconds) as response:
        response_body = response.read().decode("utf-8")
        return json.loads(response_body)


def read_json_file(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as file:
        payload = json.load(file)
    if not isinstance(payload, dict):
        raise ValueError("Lesson JSON must be an object.")
    return payload


def load_default_env_files() -> None:
    load_env_file(project_root() / ".env")
    load_env_file(Path(__file__).resolve().parent / ".env")


def load_env_file(path: Path) -> None:
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def project_root() -> Path:
    return Path(__file__).resolve().parents[1]
