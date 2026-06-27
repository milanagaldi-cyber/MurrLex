# Architecture

## High-Level Shape

### Confirmed From Code

MurrLex is currently a monorepo:

```text
MakeMistake/
  mobile/android/  Android app
  backend/         Django backend prototype
  connector/       Python connector
  docs/            Shared docs and ChatGPT Action notes
```

The Android app is local-first. Lessons and study progress are handled inside the app. The backend is currently a lab/prototype service for importing and reviewing lesson JSON, not a required runtime dependency for the Android app.

## Android Architecture

### Confirmed From Code

Important Android files:

- `mobile/android/app/src/main/java/com/lexaprograms/polishcards/MainActivity.kt`
  Main Jetpack Compose UI. This is currently very large and contains most screens, dialogs, translator UI, study UI, Settings UI, onboarding, and helper composables.

- `mobile/android/app/src/main/java/com/lexaprograms/polishcards/MainViewModel.kt`
  Main state holder and behavior coordinator for lessons, sessions, navigation, editing, translation, speech, and app settings.

- `mobile/android/app/src/main/java/com/lexaprograms/polishcards/CardRepository.kt`
  Local persistence and JSON loading/import/export logic.

- `mobile/android/app/src/main/java/com/lexaprograms/polishcards/Flashcard.kt`
  Card data model and compatibility helpers for older JSON fields.

- `mobile/android/app/src/main/java/com/lexaprograms/polishcards/Lesson.kt`
  Lesson data model.

- `mobile/android/app/src/main/java/com/lexaprograms/polishcards/StudyMode.kt`
  Study ordering modes: `ORIGINAL`, `ALPHABETICAL`, `RANDOM`.

- `mobile/android/app/src/main/java/com/lexaprograms/polishcards/StudySession.kt`
  Persisted study-session state.

- `mobile/android/app/src/main/java/com/lexaprograms/polishcards/CardNotificationWorker.kt`
  Periodic notification worker.

- `mobile/android/app/src/main/java/com/lexaprograms/polishcards/QuickVoiceWidgetProvider.kt`
  Home-screen quick voice widget entry point.

### Inferred From Behavior

The Android architecture grew through rapid UX iteration. `MainActivity.kt` and `MainViewModel.kt` should be split in v0.02 before adding many new product features.

### Uncertain / Needs User Confirmation

- Whether to keep local JSON as primary storage, move to Room/SQLite on Android, or prioritize backend sync.
- Whether current local SharedPreferences/file persistence is acceptable for v0.02.

## Backend Architecture

### Confirmed From Code

Backend files:

- `backend/manage.py`
- `backend/make_mistake_backend/`
- `backend/lessons/models.py`
- `backend/lessons/services.py`
- `backend/lessons/views.py`
- `backend/lessons/templates/`

Models:

- `Lesson`
- `Card`
- `ImportLog`

Endpoints/UI:

- `/lab/import-json/`
- `/lab/lessons/`
- `/lab/lessons/<id>/`
- `/lab/imports/`
- `/api/health/`
- `/api/internal/import-lesson`
- translation endpoint in `lessons/views.py`

SQLite is the default database. Environment settings are designed so PostgreSQL can be introduced later through `DATABASE_URL`.

### Inferred From Behavior

The backend is a learning/prototype layer for future ChatGPT integration and content pipeline testing.

### Uncertain / Needs User Confirmation

- Whether external demo access via Cloudflare Tunnel should remain part of v0.02.
- Whether user accounts/roles should be added soon or postponed.

## Data Contracts

### Confirmed From Code

The current card/lesson fields include:

- Lesson: `id`, `title`, `lessonInfo`, `sourceLanguage`, `targetLanguage`, `cards`, `timesCompleted`, `editable`, `hidden`.
- Card: `id`, `nativeValue`, `correctValue`, `wrongAnswers`, `hint`, `madeAt`, `where`, `log`, `mistake`, `value`, `pl`, `ru`, `type`, `cardKind`, `sourceLanguage`, `targetLanguage`, `stars`.

Backward compatibility exists for old `pl`/`ru` and `mistake`/`value` fields.

### Inferred From Behavior

`cardKind = "MK"` means mistake card, and `cardKind = "LN"` means lesson card.

### Uncertain / Needs User Confirmation

- Final JSON schema for MurrLex v0.02.
- Whether stars are per-card or should become per-side/per-direction.

