# Make Mistake

Make Mistake is a language-learning product built around mistake cards.

The current technical goal is a small sync prototype:

```text
AI / ChatGPT
-> Python connector
-> Django backend
-> SQLite database
-> Lab UI for checking imported lessons
```

This repository is intentionally structured as a monorepo so the Android app, backend, connector, shared JSON formats, and documentation can evolve together.

## Structure

```text
MakeMistake/
  backend/    Django backend for lesson import and Lab UI
  connector/  Python connector scripts/modules for syncing lessons
  mobile/
    android/  Android app, Kotlin + Jetpack Compose
  docs/       Product and technical documentation
```

## Environment Variables

Copy `.env.example` to `.env` for local backend/connector development:

```powershell
cd C:\CodexProjects\MakeMistake
Copy-Item .env.example .env
```

Important variables:

```text
SECRET_KEY              Django local/dev secret key
DEBUG                   true for local development
ALLOWED_HOSTS           allowed local hosts
DATABASE_URL            empty means SQLite; PostgreSQL can be added later
INTERNAL_IMPORT_TOKEN   Bearer token for internal import API
DJANGO_IMPORT_URL       connector target URL
```

Never commit `.env`.

## Backend Setup

```powershell
cd C:\CodexProjects\MakeMistake\backend
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe manage.py migrate
.\.venv\Scripts\python.exe manage.py runserver
```

Default local database:

```text
backend/db.sqlite3
```

If `DATABASE_URL` is empty, Django uses SQLite. Later PostgreSQL can be enabled with a URL like:

```text
DATABASE_URL=postgresql://make_mistake_user:password@localhost:5432/make_mistake
```

## Lab UI

After `runserver`, open:

```text
http://127.0.0.1:8000/lab/import-json/
http://127.0.0.1:8000/lab/lessons/
http://127.0.0.1:8000/lab/imports/
```

The Lab UI is a small testing interface, not the final product UI.

## Internal API

Endpoint:

```text
POST http://127.0.0.1:8000/api/internal/import-lesson
Authorization: Bearer <INTERNAL_IMPORT_TOKEN>
Content-Type: application/json
```

Expected success response:

```json
{
  "status": "ok",
  "lessonId": "lesson-id",
  "cardsImported": 30
}
```

## ChatGPT Action Bridge

For local ChatGPT Action testing, Django must be exposed through a temporary public HTTPS tunnel.

Docs:

```text
docs/chatgpt-action-setup.md
docs/chatgpt-action-openapi.yaml
```

## Connector

CLI usage:

```powershell
cd C:\CodexProjects\MakeMistake
python connector\send_lesson.py connector\sample_lesson.json
```

Reusable connector function for future MCP-style wrapping:

```python
from connector.client import save_lesson_to_make_mistakes

result = save_lesson_to_make_mistakes(lesson_dict)
```

## Tests

Backend tests:

```powershell
cd C:\CodexProjects\MakeMistake\backend
.\.venv\Scripts\python.exe manage.py test
```

Connector tests:

```powershell
cd C:\CodexProjects\MakeMistake
python -m unittest connector.test_client
```

## Android App

Open this folder in Android Studio:

```text
C:\CodexProjects\MakeMistake\mobile\android
```

The current Android version is `0.47`.

Debug APK path after build:

```text
mobile/android/app/build/outputs/apk/debug/app-debug.apk
```

## Git Workflow

Recommended flow:

```powershell
git status
git switch -c codex/small-feature-name
# make a small verified change
git add .
git commit -m "Short clear message"
git push -u origin codex/small-feature-name
```

Keep `main` stable. Use small branches and commits for learning-friendly iterations.
