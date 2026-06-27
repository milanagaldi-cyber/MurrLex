# Project State

## Stable Checkpoint

This is the stable handoff checkpoint before a clean MurrLex v0.02 Codex thread.

Confirmed:

- Local project path: `C:\CodexProjects\murrlex`
- GitHub repository: `https://github.com/milanagaldi-cyber/MurrLex`
- Working branch: `murrlex-0.02`
- Android app name: `MurrLex`
- Android visible version: `0.03`
- Android namespace/package remains legacy: `com.lexaprograms.polishcards`
- Backend and connector are preserved in the monorepo but are not the focus of this checkpoint.

## Validation

Latest successful commands:

```powershell
cd C:\CodexProjects\murrlex\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Both commands completed with `BUILD SUCCESSFUL`.

## What Works

Confirmed from code and build:

- Android project compiles.
- Debug APK builds.
- Lesson/card data model exists.
- Local repository logic exists.
- Built-in lesson assets exist.
- Study session model exists.
- Study ordering includes Original, Alphabetical, and Random.
- Card, Test, Translate, and Split UI code exists.
- Voice recognition and TTS code paths exist.
- Notification worker exists.
- Quick voice widget provider exists.
- Settings and version log exist.

## Partially Implemented

- Offline speech recognition depends on Android device support and downloaded models.
- Google/ML Kit translation flows depend on installed/downloaded models and device capability.
- Backend and connector are prototypes for later server/ChatGPT integration.
- Localization exists but is not cleanly centralized.

## Inferred From Recent Testing

- The user has a stable debug build to test.
- A clean install or app data reset may be needed if old local data still shows corrupted text.

## Uncertain / Needs User Confirmation

- Whether to rename the Android package id away from `com.lexaprograms.polishcards`.
- Which features form the v0.02 MVP versus later experimental paths.
- Whether backend sync should become active in v0.02 or stay parked.
