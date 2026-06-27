# Known Issues And Limitations

## Encoding / Mojibake

### Confirmed From Code

Some source files contain corrupted string literals such as `Ãƒ...`, especially older localized UI text and sample JSON text in docs.

The latest emergency fix makes core UI labels and Settings labels use clean English fallback text, but it does not remove every corrupted literal from the repository.

### Inferred From Behavior

The screenshots showed corrupted labels in:

- lesson catalog summaries;
- study counters;
- card/study screen labels.

### Uncertain / Needs User Confirmation

- Whether user-local lesson data stored on the phone also contains corrupted strings.
- Whether reinstalling/resetting app data is acceptable for testing.

## Localization

### Confirmed From Code

- There are Android `values-*` string resources for multiple languages.
- There are also hardcoded localized strings inside `MainActivity.kt`.
- Settings currently uses English fallback through `st(...)`.

### Inferred From Behavior

The localization approach is fragile and caused the encoding incident.

### Uncertain / Needs User Confirmation

- Final supported interface languages for v0.02.
- Whether buttons should remain English-only to preserve layout.

## Android Architecture

### Confirmed From Code

- `MainActivity.kt` is very large.
- `MainViewModel.kt` is very large.

### Inferred From Behavior

This makes layout and behavior bugs harder to isolate.

## Android Lint Findings

### Confirmed From Validation

`.\gradlew.bat lintDebug` currently fails with 3 errors and 33 warnings.

Errors reported by lint:

- `CardNotificationWorker.kt:52` uses `NotificationManagerCompat.notify(...)` without an explicit notification permission check or `SecurityException` handling.
- `MainViewModel.kt:2256` has `SuspiciousIndentation`.
- `res/values/strings.xml:4` defines `quick_voice_widget_name`, but that string is missing in localized `values-*` folders.

Warnings include obsolete SDK checks for a project with `minSdk = 34`, dependency update suggestions, and SharedPreferences `commit()` usage.

### Inferred From Behavior

The debug APK still builds, but lint should be fixed early in `murrlex-0.02` before adding new features.

## UI / UX Stability

### Inferred From Behavior

Recent user-reported areas needing more validation:

- Translate/Split Translate spacing and text layout.
- Voice recording dialog encoding and expected language text.
- Online/offline dot responsiveness.
- Lesson editor scrolling and card controls.
- Quick microphone card insertion order and focus.
- Empty-card special menu behavior.

### Uncertain / Needs User Confirmation

- Which of these are still present after the latest emergency build.

## Speech And Translation

### Confirmed From Code

- Android includes Google ML Kit Translate dependency.
- Android speech recognition and offline model download work has been added.
- Backend has an OpenAI translation endpoint.

### Inferred From Behavior

Offline translation and speech recognition need real-device validation. Some behavior depends on Android system services and downloaded models.

### Uncertain / Needs User Confirmation

- Which target devices must be supported.
- Whether offline translation is mandatory for MVP.

## Backend

### Confirmed From Code

- SQLite is the current default.
- PostgreSQL is not yet configured as an active deployed database.
- Backend lab UI requires login.

### Inferred From Behavior

The backend is useful for learning/prototyping but is not yet production-ready.
