import argparse
import json
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError

try:
    from connector.client import ConnectorConfigError, read_json_file, save_lesson_to_make_mistakes
except ModuleNotFoundError:
    from client import ConnectorConfigError, read_json_file, save_lesson_to_make_mistakes


def main() -> int:
    parser = argparse.ArgumentParser(description="Send one Make Mistake lesson JSON file to Django.")
    parser.add_argument("json_file", help="Path to a lesson JSON file.")
    args = parser.parse_args()

    try:
        payload = read_json_file(Path(args.json_file))
        response = save_lesson_to_make_mistakes(payload)
    except ConnectorConfigError as exc:
        print(str(exc), file=sys.stderr)
        return 2
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


def print_json_or_text(value: str, stream) -> None:
    try:
        parsed = json.loads(value)
    except json.JSONDecodeError:
        print(value, file=stream)
    else:
        print(json.dumps(parsed, ensure_ascii=False, indent=2), file=stream)


if __name__ == "__main__":
    raise SystemExit(main())
