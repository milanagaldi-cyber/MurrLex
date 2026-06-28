# Project State

## Stable Checkpoint

This is the stable handoff checkpoint after the Android_Main Translate/Split online/offline translator work.

Confirmed:

- Local project path: `C:\CodexProjects\murrlex`
- GitHub repository: `https://github.com/milanagaldi-cyber/MurrLex`
- Working branch: `Android_Main`
- Android app name: `MurrLex`
- Android visible version: `0.11`
- Android namespace/package remains legacy: `com.lexaprograms.polishcards`
- Backend and connector are preserved in the monorepo but are not the focus of this checkpoint.

## Validation

Latest successful command:

```powershell
cd C:\CodexProjects\murrlex\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

`assembleDebug` completed with `BUILD SUCCESSFUL` for v0.11. `lintDebug` was not rerun for this checkpoint.

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
- Translate/Split online translation uses the free Google Translate endpoint by default.
- Critical offline translation uses ML Kit Google Translate with downloaded models.
- Offline speech recognition works through Android on-device SpeechRecognizer after the selected language model is downloaded.
- The red offline title dot now drives behavior: offline recognition/translation run when the device is offline even if the manual critical offline toggle is off.
- Split mode has mirrored microphone input on the translated side, listening in the target language.
- Voice recognition and TTS code paths exist.
- Notification worker exists.
- Quick voice widget provider exists.
- Settings and version log exist.

## Partially Implemented

- Offline speech recognition depends on Android device support and downloaded models.
- Google/ML Kit offline translation depends on downloaded language models and device capability.
- Backend and connector are prototypes for later server/ChatGPT integration.
- Localization exists but is not cleanly centralized.

## Inferred From Recent Testing

- The user has a stable debug build to test.
- A clean install or app data reset may be needed if old local data still shows corrupted text.

## Uncertain / Needs User Confirmation

- Whether to rename the Android package id away from `com.lexaprograms.polishcards`.
- Which features form the v0.02 MVP versus later experimental paths.
- Whether backend sync should become active in v0.02 or stay parked.
