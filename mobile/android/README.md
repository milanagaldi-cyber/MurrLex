# MurrLex Android

Android app built with Kotlin and Jetpack Compose for local-first language learning, voice input, translation, and universal flashcards.

Current app version: `0.11`.

## Project Location

```text
C:\CodexProjects\murrlex\mobile\android
```

## Current Translator State

- Translate/Split uses the current free Google Translate endpoint by default while backend/OpenAI translation is parked for later.
- Critical offline fallback uses Android on-device SpeechRecognizer and ML Kit Google Translate downloaded models.
- The effective offline condition is `state.useLocalTranslation || !isDeviceOnline`; keep the red offline indicator aligned with actual behavior.
- Split mode has microphone buttons on both sides. The translated-side microphone listens in the target language.
- RU and BY language options intentionally use a white flag glyph.

## Core Features

- Lesson catalog with tile cards.
- Built-in lessons loaded from `app/src/main/assets/lessons`.
- Local lesson import and editing.
- Card, Test, Translate, and Split modes.
- Voice input, text-to-speech, offline speech model downloads, and translation model downloads.
- Settings screen with language, translator, notification, and version log controls.

## Building APK

```powershell
cd C:\CodexProjects\murrlex\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

Debug APK path:

```text
C:\CodexProjects\murrlex\mobile\android\app\build\outputs\apk\debug\app-debug.apk
```

## Manual Smoke Checks

1. Confirm Settings shows `MurrLex v0.11`.
2. Confirm online Translate works with network enabled.
3. Disable network and confirm the title indicator is red.
4. Confirm manual text in Translate uses offline ML Kit translation.
5. Confirm microphone speech is recognized offline and translated offline.
6. Confirm Split mode microphones work on both sides.
