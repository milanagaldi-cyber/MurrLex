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
- Version visible in the app settings: `MurrLex v0.88`
- Main working branch: `Android_Main`
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
- Latest `assembleDebug` succeeds.
- `lintDebug` succeeded at the earlier checkpoint but was not rerun for v0.33.
- Lessons and cards are stored locally.
- Built-in lesson JSON assets are present.
- Study modes include Original, Alphabetical, and Random.
- Card, Test, Translate, and Split UI paths exist.
- Voice input and text-to-speech paths exist.
- Belarusian OpenAI speech recognition routes as `be-BY`, and ElevenLabs Belarusian TTS reports safe HTTP errors such as 403 in the app log.
- Startup purr plays from `res/raw/startup_purr.mp3`, and the ElevenLabs Belarusian voice ID can be typed manually in Settings.
- Card auto-translation and the manual `T` button use the configured translation provider, including OpenAI when enabled; ElevenLabs remains audio-only.
- OpenAI translation has priority for automatic card and Translate paths whenever it is enabled, keyed, and online, including Polish to Belarusian.
- OpenAI voice recognition silence timeout can be configured down to 1 second.
- Audio playback stops on app taps, navigation away from Study, or a new playback request.
- Study card speech reuses cached side audio even while offline, and cards show a blinking green/red/yellow status dot for online/offline/cached-audio state.
- Latvian, Lithuanian, and Portuguese are available language choices. Settings can add/remove languages that use the special ElevenLabs TTS path.
- New audio playback stops older audio, cached card audio appears as a black outline on the status dot, and card logs include model/voice details for STT, translation, and TTS.
- OpenAI card translation adds short explanation/rule/examples to the card, copy-to-input also copies to clipboard, and answer input labels follow the visible card side.
- Tapping `MurrLex` or the Study card language/status area forces an immediate online/offline status check.
- Same-language microphone Quick Vocabulary captures create Mistake cards, corrected by the configured OpenAI text model with explanation/rules/examples.
- Long-pressing a Mistake card can generate Train cards from ten selectable drill options; Train cards do not generate more cards.
- MurrLex accepts shared text posts from other Android apps and can turn up to 500 words into one Basic -> Target retelling card per meaningful sentence.
- A URL icon beside the app title lets users paste a link manually and create a lesson through the same shared-post import flow.
- Telegram URL import extracts real post text from metadata/widget content and filters out Telegram page navigation/embed boilerplate.
- Shared post and URL imports can use up to 500 words and create one Basic -> Target card per meaningful sentence for retelling.
- Manual URL import supports ordinary article pages, extracts readable article text, and sends up to 2000 words to OpenAI for Basic -> Target thesis cards.
- Photo/screenshot import can use a separate OpenAI image-text recognition model, cache recognized text by image hash, and create cards from the extracted text.
- Image import accepts up to five selected photos into one lesson; the Study screen also has a camera photo action near the card language code.
- In Study, tapping the language code opens a display menu for sorting, done visibility, and three-star-card visibility; gallery/camera OCR adds one card per image/photo to the current lesson.
- Study card actions are grouped into a round cat-face menu for sharing, cached visible-side audio export, and card log/progress.
- The cat-face menu keeps Info/rule and edit actions; Catalog has an image OCR action; Study image cards append to the lesson end and navigate to the appended card.
- Catalog has matching gallery/camera OCR actions; Study card Info/Edit are separate buttons beside the cat menu, which includes confirmed Delete.
- Translate/Split uses free online Google Translate by default and falls back to ML Kit offline translation when the device is offline or critical offline mode is enabled.
- Offline speech recognition works through Android on-device SpeechRecognizer after the selected language model is downloaded.
- Split mode has microphones on both sides; the translated-side microphone listens in the target language.
- Recent performance pass reduced repeated Compose recomposition work in Settings/Translate/Test/Catalog paths and made notification card weighting avoid duplicated in-memory lists.
- Language roles are normalized: Basic/Native Language is the known language and front/native card side; Target/Learning Language is the studied language and correct/checked card side.
- Quick Dictionary and Translate language buttons override only the current active Basic/Target pair. They do not mutate global Settings, but the active pair wins for immediate speech recognition, translation, and card/lesson creation.
- Quick Vocabulary creates separate lessons per active Basic/Target pair.
- Split and normal Translate keep the Basic input side at the bottom and the Target translation side at the top.
- After onboarding, users can download local libraries for their default Basic/Target languages or continue online and download them later from Settings.
- Settings includes a Local languages manager with translation/speech icons, status hints, extra language downloads, and ML Kit translation-model removal.
- Settings includes optional OpenAI online models with separate model choices for speech-to-text, translation/text tasks, text-to-speech, and TTS voice.
- OpenAI translation and TTS responses are cached with configurable lifetime and a clear-cache action; STT requests are not cached.
- Settings shows a two-day OpenAI activity log, and OpenAI translation/TTS paths show short cache/API status messages.
- Card content is cached persistently until the card/lesson is deleted or the user clears app cache; OpenAI cache lookup ignores case and punctuation.
- OpenAI STT shows the voice waveform only while there is microphone signal and stops recording after the configured silence timeout.
- Startup uses a soft purr sound; other app action sound effects are silent.
- Belarusian text-to-speech can use ElevenLabs with its own provider/model/voice/API-key settings.
- Study cards can share or save cached/generated TTS audio for the currently visible side.
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

For the next Android_Main thread, start by preserving the working translator behavior: verify online translation, offline speech recognition, offline ML Kit translation, active language-pair overrides, and Split mirrored microphone before refactoring.
