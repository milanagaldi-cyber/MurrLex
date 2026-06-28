# MurrLex Agent Handoff

This repository is the MurrLex monorepo. Use this file as the first stop for any clean Codex thread.

## Current Stable Checkpoint

- Local path: `C:\CodexProjects\murrlex`
- GitHub: `https://github.com/milanagaldi-cyber/MurrLex`
- Working branch for the next phase: `Android_Main`
- Android visible app version: `MurrLex v0.35`
- Latest verified Android build command: `gradlew.bat assembleDebug`
- Latest verified lint command: not rerun for v0.35; previous checkpoint used `gradlew.bat lintDebug`
- Debug APK: `C:\CodexProjects\murrlex\mobile\android\app\build\outputs\apk\debug\app-debug.apk`

## Current Android Translator Notes

- Translate/Split defaults to online translation through the free Google Translate endpoint while backend/OpenAI translation is parked for later.
- Critical offline fallback uses Android on-device speech recognition and ML Kit Google Translate downloaded language models.
- The red offline title dot must drive behavior, not only UI: offline translation/recognition should run when `!isDeviceOnline` even if the manual critical offline toggle is off.
- Split mode includes a mirrored microphone on the translated side; the conversation partner speaks in the target language.
- RU and BY language options intentionally use a white flag glyph.
- Language roles are explicit: Basic/Native Language is the language the user knows and the default for interface, quick voice, Translate dictation, future AI explanations, and documentation; Target/Learning Language is the language being studied and the checked card side.
- LN cards should store Basic/Native text on the front/native side and Target/Learning text on the correct/checked side unless the user explicitly chooses a different pair.
- Quick Dictionary and Translate language buttons change only the current active Basic/Target pair, not global Settings; the active pair wins for current recognition, translation, and card/lesson creation.
- Quick Vocabulary lessons are separated by active Basic/Target pair, and Split keeps Basic input at the bottom with Target on the mirrored side.
- Belarusian speech recognition must route as `be-BY`/`language=be`; ElevenLabs Belarusian TTS failures should show and log safe HTTP diagnostics such as 403 without exposing the API key.
- Startup purr uses `mobile/android/app/src/main/res/raw/startup_purr.mp3`; ElevenLabs voice ID is a free text setting so paid/available voices can be entered manually.
- Card auto-translation and the manual `T` button should use the same configured translation provider as Translate/Quick Vocabulary; ElevenLabs is only for Belarusian TTS/audio.
- When OpenAI translation is enabled, online, and keyed, it has priority for automatic card/Translate paths even if critical offline mode is toggled on; local ML Kit is only the fallback.
- OpenAI voice silence timeout can be set as low as 1 second in Settings.
- Audio playback is single-instance: app taps, navigation away from Study, or a new playback request stop the current sound.
- Study card speech first reuses cached card-side audio, including while offline; card-side status dots blink every five seconds and show online/offline/cached-audio state.
- Latvian, Lithuanian, and Portuguese are available language choices. Settings has a Special ElevenLabs TTS language menu; enabled languages use the same ElevenLabs voice-generation path as Belarusian.
- Audio playback is last-started-wins. Card status blink interval is configurable, cached audio is shown with a black outline, and card logs include STT/translation/TTS model or voice details.
- OpenAI card translation writes a short explanation, rule, and examples into the card hint/log context; copy-to-input also writes to clipboard; answer input labels follow the current card side language.
- Tapping the `MurrLex` title or the Study card language/status area forces an immediate online/offline recheck.
- Same-language Quick Vocabulary microphone input creates a `Mistake` card and asks the configured OpenAI text model to correct it in the same language with explanation/rules/examples.
- Long-pressing a `Mistake` card opens a ten-option Train-card generation dialog. Generated cards are `Train` (`TR`) cards and must not generate more cards.
- Android text sharing into MurrLex is supported for `text/plain` posts. Shared text opens an import dialog, limits source text to 100 words, and creates sentence or vocabulary cards for the current Basic/Target pair.
- Normal Translate also keeps Target translation at the top and Basic input at the bottom.
- After onboarding the app offers to download local libraries for the default languages or continue online; Settings has a Local languages manager with translation/speech status icons and ML Kit translation-model removal.
- OpenAI online mode has separate settings for STT, translation/text, TTS, TTS voice, base URL, and API key. Never log the API key.
- OpenAI translation/TTS responses are cached in `cacheDir/openai_cache` with metadata. STT voice recognition is intentionally not cached.
- Card text content is mirrored into persistent `filesDir/card_cache`; it is not TTL-pruned and is cleared only when cards/lessons are deleted or the user explicitly clears app cache.
- OpenAI cache lookup ignores case and punctuation, and OpenAI STT has a configurable voice-signal silence timeout.
- Startup sound is a soft purr only; other app action sound effects are intentionally silent.
- Belarusian TTS can route through ElevenLabs with separate API key/model/voice settings; never log the ElevenLabs key.
- Study cards include a visible-side cached audio download/share action using a download plus music-note icon.
- Settings shows a two-day OpenAI activity log. Translation/TTS paths show short technical cache/API status messages; API keys must never be logged.

## Rules For The Next Thread

- Work only in `C:\CodexProjects\murrlex` unless the user explicitly says otherwise.
- Start with `git status --short --branch` and confirm the branch.
- Keep backend and connector preserved; do not pull server work into Android unless requested.
- Do not implement new features from this handoff step.
- Keep changes small and verified.
- Be careful with text encoding. Past UI/data corruption showed mojibake such as `Ã...` in some legacy strings.
- Commit only after explicit user approval.

## Useful Commands

```powershell
cd C:\CodexProjects\murrlex\mobile\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Debug APK:

```text
C:\CodexProjects\murrlex\mobile\android\app\build\outputs\apk\debug\app-debug.apk
```
