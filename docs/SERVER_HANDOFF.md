# MurrLex Server Handoff

Use this document to give the backend work to another developer through GitHub.

## Branch

- Repository: `https://github.com/milanagaldi-cyber/MurrLex`
- Server branch: `Server_Main`
- Backend path: `backend/`
- Connector path: `connector/`
- Shared server/API docs: `docs/`

Keep Android work out of this branch unless the task explicitly requires mobile integration.

## Current Server Shape

The backend is a Django prototype with:

- Django Admin for `Lesson`, `Card`, and `ImportLog`.
- Lab UI under `/lab/`.
- Health endpoint: `GET /api/health`.
- Internal import endpoint: `POST /api/internal/import-lesson`.
- Translation endpoint: `POST /api/translate`.
- SQLite for local development.
- Optional PostgreSQL support through `DATABASE_URL`.
- Connector script for sending lesson JSON into the import endpoint.

The backend is not yet the Android app's runtime data source. Treat it as the server/content pipeline branch.

## Local Setup On A New Machine

Windows PowerShell:

```powershell
git clone https://github.com/milanagaldi-cyber/MurrLex.git
cd MurrLex
git switch Server_Main
Copy-Item .env.example .env
cd backend
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe manage.py migrate
.\.venv\Scripts\python.exe manage.py createsuperuser
.\.venv\Scripts\python.exe manage.py test
.\.venv\Scripts\python.exe manage.py runserver
```

Linux/macOS shell:

```bash
git clone https://github.com/milanagaldi-cyber/MurrLex.git
cd MurrLex
git switch Server_Main
cp .env.example .env
cd backend
python3 -m venv .venv
./.venv/bin/python -m pip install --upgrade pip
./.venv/bin/python -m pip install -r requirements.txt
./.venv/bin/python manage.py migrate
./.venv/bin/python manage.py createsuperuser
./.venv/bin/python manage.py test
./.venv/bin/python manage.py runserver 0.0.0.0:8000
```

Open:

```text
http://127.0.0.1:8000/api/health
http://127.0.0.1:8000/lab/lessons/
http://127.0.0.1:8000/lab/imports/
```

## Required Environment Variables

Copy `.env.example` to `.env`, then change secrets before exposing the backend:

```text
SECRET_KEY=change-this
DEBUG=false
ALLOWED_HOSTS=your-domain.example,127.0.0.1,localhost
CSRF_TRUSTED_ORIGINS=https://your-domain.example
DATABASE_URL=
INTERNAL_IMPORT_TOKEN=change-this-too
DJANGO_IMPORT_URL=https://your-domain.example/api/internal/import-lesson
CREDENTIAL_ENCRYPTION_KEY=fernet-key-generated-on-the-server
OPENAI_TRANSLATION_MODEL=gpt-4o-mini
```

For local SQLite, leave `DATABASE_URL` empty. For PostgreSQL:

```text
DATABASE_URL=postgresql://murrlex_user:password@localhost:5432/murrlex
```

## Minimal Server Deployment Checklist

1. Clone the repo on the server and switch to `Server_Main`.
2. Create `.env` with production values.
3. Create a Python virtual environment.
4. Install `backend/requirements.txt`.
5. Run `manage.py check --deploy` and review warnings.
6. Run `manage.py migrate`.
7. Run `manage.py collectstatic`.
8. Start Django through Gunicorn, for example:

```bash
cd /opt/MurrLex/backend
./.venv/bin/gunicorn make_mistake_backend.wsgi:application --bind 127.0.0.1:8000
```

9. Put Nginx/Caddy/Apache in front with HTTPS.
10. Verify `/api/health`, `/login/`, and `/lab/lessons/`.

## Access Control Operations

Read `docs/ACCESS_CONTROL.md` before changing staff, MFA, SSH, or deployment access.

After migrations, synchronize the standard Django groups:

```bash
python manage.py bootstrap_roles
```

Generate a read-only staff access review:

```bash
python manage.py review_access
```

The repository contains a manual GitHub Actions workflow plus restricted deploy/SSHD examples. They do not alter the VPS until an owner explicitly creates and tests the separate Linux accounts and self-hosted runner.

## Connector Test

With Django running:

```powershell
cd C:\path\to\MurrLex
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

## Suggested Next Backend Tasks

1. Decide deployment target and database: SQLite for demo or PostgreSQL for shared work.
2. Add real production settings for static files, logging, and allowed hosts.
3. Add `/api/translate` to the OpenAPI schema if ChatGPT should call it.
4. Clean legacy Make Mistake names when it is safe to rename Django modules.
5. Fix mojibake in backend test fixtures without changing import behavior.
6. Add CI for backend tests and connector tests.

## Mobile AI Gateway

Android online AI work is now served only through MurrLex. The app chooses the model and keeps an encrypted MurrLex login session. A server superuser enters OpenAI and ElevenLabs keys at `/account/provider-keys/`; keys are encrypted in the database and never returned to the browser or mobile client.

Personal browser applications live under `/apps/`. Access requires authentication, a configured OpenAI key, and an enabled `AiAccessGrant` (superusers are allowed automatically). `/api/sync` provides owner-isolated all/lesson/card synchronization with Android.

Add these production values to `.env` and never commit them:

```text
OPENAI_BASE_URL=https://api.openai.com/v1
ELEVENLABS_BASE_URL=https://api.elevenlabs.io/v1
CREDENTIAL_ENCRYPTION_KEY=fernet-key-generated-on-the-server
JWT_SIGNING_KEY=a-long-random-secret-different-from-SECRET_KEY
JWT_ACCESS_MINUTES=15
JWT_REFRESH_DAYS=30
```

Mobile authentication routes:

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

Authenticated AI routes:

```text
POST /api/ai/text
POST /api/ai/transcribe
POST /api/ai/speech
POST /api/ai/image-text
```

All AI routes require `Authorization: Bearer <access token>`. Access tokens expire quickly; refresh tokens are opaque, stored as SHA-256 hashes in `ApiSession`, rotate on refresh, and are revoked by logout. Before public release, rate-limit login and AI endpoints at the reverse proxy and use PostgreSQL rather than SQLite.
