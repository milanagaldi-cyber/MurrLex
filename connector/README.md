# Connector

Python connector code for sending lesson JSON into the Make Mistake Django backend.

Current script:

```text
send_lesson.py
```

It:

- reads lesson JSON from a file path argument;
- reads `DJANGO_IMPORT_URL` and `INTERNAL_IMPORT_TOKEN` from environment variables;
- also loads `.env` from the repository root if present;
- sends `POST` to the Django internal import endpoint;
- prints the JSON response.

## Usage

Start Django first:

```powershell
cd C:\CodexProjects\MakeMistake\backend
.\.venv\Scripts\python.exe manage.py runserver
```

In another PowerShell:

```powershell
cd C:\CodexProjects\MakeMistake
$env:DJANGO_IMPORT_URL = "http://127.0.0.1:8000/api/internal/import-lesson"
$env:INTERNAL_IMPORT_TOKEN = "change-me-import-token"
python connector\send_lesson.py connector\sample_lesson.json
```

Expected response:

```json
{
  "status": "ok",
  "lessonId": "connector-sample-lesson",
  "cardsImported": 2
}
```

Later this connector logic will be wrapped in an MCP-style tool.
