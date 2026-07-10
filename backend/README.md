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
- mobile AI gateway with account login, rotating refresh sessions, and protected text, speech, transcription, and image-text endpoints;
- provider keys are entered by a superuser in the server cabinet, encrypted before being stored in the database, and never returned to a browser or mobile client.

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
- mobile registration, refresh-token rotation, and authenticated AI requests.

## Mobile AI Gateway

The Android app never sends OpenAI or ElevenLabs API keys. It stores only its MurrLex account session and the model choices selected by the learner.

A superuser configures provider keys at `/account/provider-keys/` (or through Django admin). The server encrypts every key using `CREDENTIAL_ENCRYPTION_KEY`; the saved key is never displayed again. Do not add provider keys to `.env`.

Required server environment values:

```text
OPENAI_BASE_URL=https://api.openai.com/v1
ELEVENLABS_BASE_URL=https://api.elevenlabs.io/v1
CREDENTIAL_ENCRYPTION_KEY=fernet-key-generated-on-the-server
JWT_SIGNING_KEY=a-long-random-server-secret
JWT_ACCESS_MINUTES=15
JWT_REFRESH_DAYS=30
```

Mobile endpoints:

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
POST /api/ai/text
POST /api/ai/transcribe
POST /api/ai/speech
POST /api/ai/image-text
```

All `/api/ai/*` endpoints require `Authorization: Bearer <access token>`. Access tokens are short-lived. Refresh tokens are opaque, stored as hashes in the database, rotated on refresh, and can be revoked by logout.

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
