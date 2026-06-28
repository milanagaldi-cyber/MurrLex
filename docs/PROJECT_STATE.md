# Project State

## Stable Checkpoint

This is the stable handoff checkpoint after the Android_Main Translate/Split online/offline translator and active language-pair work.

Confirmed:

- Local project path: `C:\CodexProjects\murrlex`
- GitHub repository: `https://github.com/milanagaldi-cyber/MurrLex`
- Working branch: `Android_Main`
- Android app name: `MurrLex`
- Android visible version: `0.35`
- Android namespace/package remains legacy: `com.lexaprograms.polishcards`
- Backend and connector are preserved in the monorepo but are not the focus of this checkpoint.

## Validation

Latest successful command:

```powershell
cd C:\CodexProjects\murrlex\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

`assembleDebug` completed with `BUILD SUCCESSFUL` for v0.35. `lintDebug` was not rerun for this checkpoint.

## What Works

Confirmed from code and build:

- Android project compiles.
- Debug APK builds.
- Belarusian OpenAI speech recognition routes through `be-BY`/`language=be`.
- ElevenLabs Belarusian TTS now reports safe HTTP diagnostics such as 403 instead of a generic failure.
- Startup purr uses a packaged mp3 resource.
- ElevenLabs Belarusian voice ID is editable by hand in Settings.
- Card auto-translation and the manual T action use the configured translation provider, including OpenAI when enabled.
- ElevenLabs remains scoped to Belarusian TTS/audio only.
- OpenAI translation has priority for automatic card and Translate paths whenever it is enabled, keyed, and online.
- OpenAI voice silence timeout can be configured down to 1 second.
- Online status detection uses internet capability directly instead of waiting for delayed Android validation.
- Audio playback is single-instance and stops on app taps, navigation away from Study, or a new playback request.
- Study card speech reuses cached side audio even while offline, and cards show blinking online/offline/cached-audio status dots.
- Latvian, Lithuanian, and Portuguese language choices exist, and Settings can add/remove Special ElevenLabs TTS languages.
- Audio playback is last-started-wins, card status blink timing is configurable, cached audio shows as a black outline, and card logs record STT/translation/TTS model or voice details.
- OpenAI card translation can write explanation, rule, and examples into the card; copy-to-input also copies to clipboard; answer labels follow the current side language.
- Tapping the `MurrLex` title or the Study card language/status area forces an immediate online/offline recheck.
- Same-language Quick Vocabulary microphone input creates a Mistake card and asks the configured OpenAI text model for a same-language correction with explanation, all applicable rules, and examples.
- Long-pressing a Mistake card opens ten selectable Train-card generation options. Generated Train cards use card kind `TR` and do not react as generation sources.
- Android text sharing into MurrLex is supported for `text/plain` posts. The import dialog previews up to 100 words and creates sentence cards or Target-language vocabulary cards for the current Basic/Target pair.
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
- Settings/Translate/Test/Catalog paths avoid several repeated recomposition computations after the v0.12 performance pass.
- Notification card selection uses weighted random selection without building duplicated weighted card lists.
- Basic/Native Language is the known language and front/native card side.
- Target/Learning Language is the studied language and correct/checked card side.
- Quick vocabulary and Translate dictation default to Basic/Native unless the user explicitly chooses a different pair.
- Quick Dictionary and Translate language buttons change only the current active Basic/Target pair, not global Settings.
- The active Basic/Target pair wins for current speech recognition, translation, and card/lesson creation.
- Quick Vocabulary lessons are separated by strict active Basic/Target lesson metadata.
- Split and normal Translate keep Basic input at the bottom and Target translation at the top.
- After onboarding, users are offered local language downloads for default Basic/Target languages or can continue online.
- Settings includes a Local languages manager for downloaded translation/speech status, extra downloads, and ML Kit translation-model removal.
- Optional OpenAI online mode has separate settings for STT, translation/text, TTS, TTS voice, base URL, and API key.
- OpenAI translation/TTS caching exists with configurable lifetime, cache clearing, and metadata. STT is not cached.
- OpenAI translation/TTS shows cache/API technical status and Settings exposes a two-day auto-pruned activity log.
- Card content cache is persistent in app files storage and is removed only with card/lesson deletion or explicit app cache clearing.
- OpenAI cache lookup ignores punctuation and case. OpenAI STT waveform follows actual voice signal and has a configurable silence timeout.
- Startup sound is a soft purr and other app action sound effects are silent.
- Belarusian TTS has separate ElevenLabs provider/model/voice/API-key settings.
- Study cards have a visible-side cached audio download/share action.
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
