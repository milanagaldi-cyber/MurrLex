# Project State: MurrLex v0.01 Checkpoint

This document captures the state at the end of the current Codex development phase, before migration to a new clean thread and branch `murrlex-0.02`.

## What This App Is

### Confirmed From Code

MurrLex is a language-learning product centered on cards. The Android app stores lessons locally, lets the user practice cards, edit lesson/card content, use speech input, and work with translate/test/card modes.

The repository is a monorepo with:

- Android client in `mobile/android/`.
- Django backend prototype in `backend/`.
- Python connector in `connector/`.
- Shared documentation and ChatGPT Action/OpenAPI notes in `docs/`.

The Android application id and namespace are still:

```text
com.lexaprograms.polishcards
```

The app display name was renamed to MurrLex in Android string resources.

### Inferred From Behavior

The product direction is moving from "mistake cards" toward a broader personal language lab: cards, tests, translation, split-screen translation, quick vocabulary capture, and later ChatGPT/server sync.

### Uncertain / Needs User Confirmation

- Whether the package id should remain `com.lexaprograms.polishcards` or eventually become a MurrLex-specific package.
- Whether Android `versionName = "1.04"` should be reset/aligned with the product checkpoint `v0.01`.
- Whether the product name should be styled exactly `MurrLex` everywhere.

## Current Tech Stack

### Confirmed From Code

Android:

- Kotlin.
- Jetpack Compose.
- Material 3.
- Kotlin serialization.
- WorkManager.
- Google ML Kit Translate dependency.
- Android SpeechRecognizer / speech features in app code.
- minSdk 34, targetSdk 35, compileSdk 35.
- Java/Kotlin target 17.

Backend:

- Python.
- Django 5.x.
- SQLite by default.
- `DATABASE_URL` support intended for later PostgreSQL.

Connector:

- Python standard library HTTP client code.
- `unittest` tests.

### Inferred From Behavior

The Android app is currently local-first. Backend/connector work is a prototype bridge for future ChatGPT/server workflows, not yet the core mobile sync engine.

### Uncertain / Needs User Confirmation

- Whether Google ML Kit offline translation remains the chosen translation path.
- Whether OpenAI translation endpoint in backend should be used by Android later.
- Whether the backend should become the source of truth in v0.02 or remain a lab prototype.

## What Already Works

### Confirmed From Code

Android app:

- Lesson model and card model exist.
- Built-in lesson assets exist under `mobile/android/app/src/main/assets/lessons/`.
- Lesson import/edit/local storage code exists in `CardRepository.kt`.
- Study session persistence exists through `StudySession`.
- Study modes include `ORIGINAL`, `ALPHABETICAL`, and `RANDOM`.
- Notifications are implemented through `CardNotificationWorker.kt`.
- Quick voice widget provider exists.
- Translate, split, card, and test mode UI code exists in `MainActivity.kt`.
- Version log exists in Settings.

Backend:

- Django models: `Lesson`, `Card`, `ImportLog`.
- Lab UI pages for importing JSON, listing lessons, viewing lesson details, and import logs.
- Internal token-protected import endpoint.
- Translation endpoint using OpenAI Responses API when `OPENAI_API_KEY` is configured.

Connector:

- CLI and reusable client function for posting lesson JSON to the backend import endpoint.
- Connector unit tests exist.

### Inferred From Behavior

- The user has successfully installed and run many Android debug APKs.
- The local Django lab UI worked in browser after login.
- Cloudflare/Porkbun DNS/tunnel work was being explored for external demo access.

### Uncertain / Needs User Confirmation

- Which Android features are stable enough to keep as MVP.
- Whether quick vocabulary and translator flows should be merged or separated.
- Which UI language should be the default after the encoding hotfix temporarily routes damaged localized UI labels to English.

## Partially Implemented

### Confirmed From Code

- Localization exists in several places, but it is not cleanly centralized.
- Android app strings/resources are renamed to MurrLex, but package names and folder names still reference PolishCards/polishcards.
- Google Translate attribution and ML Kit dependency are present.
- Backend translation endpoint exists, but Android integration with backend translation is not confirmed.
- Offline speech recognition model download UI/code exists from recent work, but needs device validation.

### Inferred From Behavior

- Translate mode UI and split translate UI have been iterated heavily and may still need layout polish.
- Quick microphone capture likely works in some paths but has had recent timing, language, and placement issues.
- Lesson editor UI has been repeatedly flagged as uncomfortable on small screens.

### Uncertain / Needs User Confirmation

- Whether first-launch onboarding should remain in v0.02.
- Whether the professor-cat splash direction is final.
- Whether to keep all four work modes (`Cards`, `Tests`, `Translate`, `Split`) visible on the lesson catalog.

## Current Emergency Fix

### Confirmed From Code

The latest local changes include an emergency encoding hotfix:

- `uiTextFor(...)` returns clean English UI labels.
- the Settings helper `st(...)` currently returns English text.
- Android `versionCode` is `104`.
- Android `versionName` is `1.04`.

### Inferred From Behavior

This was done to stop mojibake such as `Ãƒ...` appearing in the main app UI after corrupted localized literals entered the source.

### Uncertain / Needs User Confirmation

- Whether stored local lesson data also contains corrupted text and needs a data repair/reset path.

