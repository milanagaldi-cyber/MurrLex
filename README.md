# MurrLex

MurrLex is a local-first Android language-learning app built around cards, lessons, voice input, translation experiments, and quick vocabulary capture.

This repository is currently a monorepo:

```text
murrlex/
  mobile/android/   Android app, Kotlin + Jetpack Compose
  backend/          Preserved Django prototype for future server work
  connector/        Preserved Python connector prototype
  docs/             Project handoff and planning notes
```

## Current Stable Android State

- App name: `MurrLex`
- Android package/namespace still uses the legacy id: `com.lexaprograms.polishcards`
- Version visible in the app settings: `MurrLex 0.03`
- Main working branch: `murrlex-0.02`
- Stable debug APK output: `mobile/android/app/build/outputs/apk/debug/app-debug.apk`

## Build Android

Open a PowerShell terminal:

```powershell
cd C:\CodexProjects\murrlex\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

A successful debug build produces:

```text
C:\CodexProjects\murrlex\mobile\android\app\build\outputs\apk\debug\app-debug.apk
```

## What Works At The Checkpoint

Confirmed from code and recent validation:

- Android Gradle project builds successfully.
- `lintDebug` succeeds.
- `assembleDebug` succeeds.
- Lessons and cards are stored locally.
- Built-in lesson JSON assets are present.
- Study modes include Original, Alphabetical, and Random.
- Card, Test, Translate, and Split UI paths exist.
- Voice input and text-to-speech paths exist.
- Notification worker exists.
- Quick voice widget provider exists.
- Settings include version log and language-related options.

## Backend And Connector

The backend and connector are preserved in this repository for future phases. They are not part of the current Android stabilization checkpoint and should not be pulled into the next Android cleanup unless requested.

## Known Risks

- `MainActivity.kt` is still very large and should be split later in small verified steps.
- Some old localized Settings literals still contain mojibake, although the currently used UI path mostly routes to safe English labels.
- User data already stored on a device may contain old corrupted text. A clean reinstall or app data reset may be needed when testing bundled JSON fixes.
- Android package id is still legacy and may need a future migration decision.

## Next Recommended Task

For MurrLex v0.02, start with stabilization only: verify a clean install, confirm text encoding on bundled lessons, then extract localization/settings code in small commits.
