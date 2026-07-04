# MurrLex Backend

Django backend prototype for MurrLex.

Current state:

- minimal Django project is created;
- `lessons` app is created;
- `Lesson`, `Card`, and `ImportLog` models are created and registered in Django Admin;
- simple Lab UI pages are available under `/lab/`;
- internal lesson import API is available at `/api/internal/import-lesson`;
- SQLite is used by default for local development;
- `DATABASE_URL` can later switch the project to PostgreSQL settings;
- optional `/api/translate` endpoint can call OpenAI when `OPENAI_API_KEY` is configured.

## Local Setup

From the repository:

```powershell
cd C:\CodexProjects\murrlex
Copy-Item .env.example .env
cd C:\CodexProjects\murrlex\backend
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

The project reads environment variables from:

```text
C:\CodexProjects\murrlex\.env
C:\CodexProjects\murrlex\backend\.env
```

For local development, copy the root `.env.example` to `.env`.

## Database

Default local database:

```text
backend/db.sqlite3
```

If `DATABASE_URL` is empty, Django uses SQLite.

Later PostgreSQL example:

```text
DATABASE_URL=postgresql://murrlex_user:password@localhost:5432/murrlex
```

## Verification

```powershell
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py migrate
.\.venv\Scripts\python.exe manage.py showmigrations lessons
.\.venv\Scripts\python.exe manage.py shell -c "from django.contrib import admin; from lessons.models import Lesson, Card, ImportLog; print(Lesson in admin.site._registry, Card in admin.site._registry, ImportLog in admin.site._registry)"
.\.venv\Scripts\python.exe manage.py test
.\.venv\Scripts\python.exe manage.py runserver
```

The current test suite covers:

- successful internal API import;
- missing Bearer token rejection;
- invalid JSON rejection;
- lesson without cards rejection;
- imported cards linked to their lesson;
- duplicate lesson import updating the existing lesson safely.

Lab UI pages:

```text
http://127.0.0.1:8000/lab/import-json/
http://127.0.0.1:8000/lab/lessons/
http://127.0.0.1:8000/lab/lessons/<id>/
http://127.0.0.1:8000/lab/imports/
```

The Lab UI requires Django login. For temporary external testing, create a local user and share that login instead of your admin account.

Internal API:

```text
POST http://127.0.0.1:8000/api/internal/import-lesson
Authorization: Bearer <INTERNAL_IMPORT_TOKEN>
Content-Type: application/json
```

PowerShell example:

```powershell
cd C:\CodexProjects\murrlex
$headers = @{ Authorization = "Bearer change-me-import-token" }
$json = Get-Content -Raw .\connector\sample_lesson.json
Invoke-RestMethod `
  -Uri "http://127.0.0.1:8000/api/internal/import-lesson" `
  -Method Post `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $json
```

Expected success response:

```json
{
  "status": "ok",
  "lessonId": "lesson-id",
  "cardsImported": 30
}
```

ChatGPT Action setup:

```text
C:\CodexProjects\murrlex\docs\chatgpt-action-setup.md
C:\CodexProjects\murrlex\docs\chatgpt-action-openapi.yaml
```

Server handoff:

```text
C:\CodexProjects\murrlex\docs\SERVER_HANDOFF.md
```
