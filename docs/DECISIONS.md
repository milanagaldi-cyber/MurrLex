# Decisions

## Monorepo Kept

MurrLex remains a monorepo for now:

- `mobile/android` for Android.
- `backend` for the Django prototype.
- `connector` for Python sync/import experiments.
- `docs` for handoff and planning.

Reason: the product is still early and shared JSON/API decisions are evolving.

## SQLite / Backend Parked

The backend stays preserved but is not part of the current Android stabilization checkpoint.

Reason: the immediate goal is a stable mobile app and clean v0.02 thread.

## Android Version At Checkpoint

The visible Android version is `MurrLex v0.11`.

Reason: user requested every new Android build to increase the app version by `0.01`; v0.11 is the committed checkpoint for the working Translate/Split online/offline translator flow.

## Translator Online/Offline Direction

Translate/Split should use online translation by default through the current free Google Translate endpoint. Backend/OpenAI translation is intentionally parked for later.

Critical offline mode and loss of connectivity should use Android on-device speech recognition and ML Kit Google Translate with downloaded language models.

Reason: offline is a critical fallback, while normal quality should come from online translation until the future backend/OpenAI model is available.

## Safe Build Commands

The accepted validation commands are:

```powershell
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Reason: they are enough to prove the Android project compiles and lint blockers are absent.

## v0.02 Direction

The next phase should start with cleanup and stabilization, not new features.

Reason: the app has many experiments accumulated in large files; stable refactoring needs small commits and frequent builds.
