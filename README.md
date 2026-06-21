# Make Mistake

Make Mistake is a language-learning product built around mistake cards.

This repository is intentionally structured as a monorepo so the Android app, future backend, shared JSON formats, and documentation can evolve together.

## Structure

```text
MakeMistake/
  backend/    Django backend for lesson import and Lab UI
  connector/  Python connector scripts/modules for syncing lessons
  mobile/
    android/   Android app, Kotlin + Jetpack Compose
  docs/        Product and technical documentation
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

## Backend

The `backend/` folder contains the Django backend. It currently uses SQLite by default for local development and reads settings from environment variables.

The settings are prepared for a later PostgreSQL migration through `DATABASE_URL`, while keeping the first local iteration simple.

Backend quick check:

```powershell
cd C:\CodexProjects\MakeMistake\backend
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py migrate
.\.venv\Scripts\python.exe manage.py runserver
```

## Connector

The `connector/` folder contains Python scripts/modules that send lesson JSON into the Django backend. The first script is:

```text
connector/send_lesson.py
```

It reads a lesson JSON file and sends it to `DJANGO_IMPORT_URL` with `INTERNAL_IMPORT_TOKEN`.

The reusable connector function for future MCP-style wrapping is:

```python
from connector.client import save_lesson_to_make_mistakes
```

## Environment

Copy `.env.example` to `.env` for local backend/connector development when Step 3 begins. Do not commit `.env`.

## Git Workflow

Use short-lived branches for small iterations. Keep `main` stable and merge verified work through pull requests when useful.
