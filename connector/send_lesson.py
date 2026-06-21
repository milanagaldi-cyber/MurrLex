import argparse
import json
import os
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


DEFAULT_TIMEOUT_SECONDS = 20


def main() -> int:
    parser = argparse.ArgumentParser(description="Send one Make Mistake lesson JSON file to Django.")
    parser.add_argument("json_file", help="Path to a lesson JSON file.")
    args = parser.parse_args()

    load_env_file(project_root() / ".env")
    load_env_file(Path(__file__).resolve().parent / ".env")

    import_url = os.environ.get("DJANGO_IMPORT_URL", "").strip()
    import_token = os.environ.get("INTERNAL_IMPORT_TOKEN", "").strip()

    if not import_url:
        print("DJANGO_IMPORT_URL is required.", file=sys.stderr)
        return 2
    if not import_token:
        print("INTERNAL_IMPORT_TOKEN is required.", file=sys.stderr)
        return 2

    try:
        payload = read_json_file(Path(args.json_file))
        response = send_lesson(import_url=import_url, import_token=import_token, payload=payload)
    except FileNotFoundError as exc:
        print(f"File not found: {exc.filename}", file=sys.stderr)
        return 2
    except json.JSONDecodeError as exc:
        print(f"Invalid JSON file: {exc}", file=sys.stderr)
        return 2
    except HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        print_json_or_text(body, stream=sys.stderr)
        return 1
    except URLError as exc:
        print(f"Could not connect to Django import endpoint: {exc.reason}", file=sys.stderr)
        return 1

    print(json.dumps(response, ensure_ascii=False, indent=2))
    return 0


def send_lesson(import_url: str, import_token: str, payload: dict) -> dict:
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

    with urlopen(request, timeout=DEFAULT_TIMEOUT_SECONDS) as response:
        response_body = response.read().decode("utf-8")
        return json.loads(response_body)


def read_json_file(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as file:
        payload = json.load(file)
    if not isinstance(payload, dict):
        raise ValueError("Lesson JSON must be an object.")
    return payload


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


def print_json_or_text(value: str, stream) -> None:
    try:
        parsed = json.loads(value)
    except json.JSONDecodeError:
        print(value, file=stream)
    else:
        print(json.dumps(parsed, ensure_ascii=False, indent=2), file=stream)


if __name__ == "__main__":
    raise SystemExit(main())
