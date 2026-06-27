# MurrLex Agent Handoff

This repository is the MurrLex monorepo. Treat this file as the first stop for a new Codex thread.

## Current Checkpoint

- Product checkpoint: `MurrLex v0.01`.
- Next development branch requested by the user: `murrlex-0.02`.
- Current Git branch at handoff time: `0.75`.
- Do not assume the Android internal `versionName` matches the product checkpoint. At handoff time the Android debug build was at `versionName = "1.04"` after an emergency encoding hotfix.

## Repository Shape

- `mobile/android/`: Android app built with Kotlin and Jetpack Compose.
- `backend/`: Django backend prototype with SQLite by default and settings designed for later PostgreSQL.
- `connector/`: Python connector for sending lesson JSON into the backend.
- `docs/`: project documentation, ChatGPT Action/OpenAPI notes, and this handoff documentation.

## Operating Rules For The Next Thread

- Start by reading `docs/PROJECT_STATE.md`, `docs/KNOWN_ISSUES.md`, and `docs/NEXT_TASKS.md`.
- Run `git status --short --branch` before editing.
- Keep changes small and verified. The project has accumulated many UX experiments in one large Compose file.
- Do not silently overwrite user data or local `.env` files.
- Prefer documented JSON contracts over guessing from UI behavior.
- Be careful with encoding. Recent corruption produced `Ãƒ...` mojibake in UI strings and sample JSON text.
- Commit only after explicit user approval.

## Useful Commands

Android build:

```powershell
cd C:\CodexProjects\MakeMistake\mobile\android
.\gradlew.bat assembleDebug
```

Android lint:

```powershell
cd C:\CodexProjects\MakeMistake\mobile\android
.\gradlew.bat lintDebug
```

Backend checks and tests:

```powershell
cd C:\CodexProjects\MakeMistake\backend
.\.venv\Scripts\python.exe manage.py check
.\.venv\Scripts\python.exe manage.py test
```

Connector tests:

```powershell
cd C:\CodexProjects\MakeMistake
python -m unittest connector.test_client
```

