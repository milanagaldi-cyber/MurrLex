# Development Setup

## Prerequisites

### Confirmed From Code

- Windows PowerShell is used in the current local workflow.
- Android Studio is installed.
- Android Gradle wrapper exists in `mobile/android/`.
- Backend virtual environment exists at `backend/.venv/`.
- Backend dependency file: `backend/requirements.txt`.
- Root `.env.example` exists.

## Android

Open this folder in Android Studio:

```text
C:\CodexProjects\MakeMistake\mobile\android
```

Build debug APK from PowerShell:

```powershell
cd C:\CodexProjects\MakeMistake\mobile\android
.\gradlew.bat assembleDebug
```

Debug APK output:

```text
C:\CodexProjects\MakeMistake\mobile\android\app\build\outputs\apk\debug\app-debug.apk
```

Run lint:

```powershell
cd C:\CodexProjects\MakeMistake\mobile\android
.\gradlew.bat lintDebug
```

## Backend

Create local environment if needed:

```powershell
cd C:\CodexProjects\MakeMistake\backend
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Copy local env file if needed:

```powershell
cd C:\CodexProjects\MakeMistake
Copy-Item .env.example .env
```

Run checks:

```powershell
cd C:\CodexProjects\MakeMistake\backend
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py migrate
.\.venv\Scripts\python.exe manage.py test
```

Run local server:

```powershell
cd C:\CodexProjects\MakeMistake\backend
.\.venv\Scripts\python.exe manage.py runserver
```

Lab UI:

```text
http://127.0.0.1:8000/lab/import-json/
http://127.0.0.1:8000/lab/lessons/
http://127.0.0.1:8000/lab/imports/
```

## Connector

Run connector tests:

```powershell
cd C:\CodexProjects\MakeMistake
python -m unittest connector.test_client
```

Send sample lesson to a running backend:

```powershell
cd C:\CodexProjects\MakeMistake
$env:DJANGO_IMPORT_URL = "http://127.0.0.1:8000/api/internal/import-lesson"
$env:INTERNAL_IMPORT_TOKEN = "change-me-import-token"
python connector\send_lesson.py connector\sample_lesson.json
```

## Validation Expectations

### Confirmed From Code

The project has backend and connector tests. Android has a Gradle build and lint task.

### Inferred From Behavior

There are no dedicated Android unit/UI tests yet.

### Uncertain / Needs User Confirmation

- Whether CI should be added in v0.02.
- Whether emulator/device visual tests should become mandatory before each APK handoff.

