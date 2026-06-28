# MurrLex Android

Android app built with Kotlin and Jetpack Compose for local-first language learning, voice input, translation, and universal flashcards.

Current app version: `0.35`.

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
- v0.12 reduces repeated Compose recomposition work in Settings/Translate/Test/Catalog and avoids duplicated weighted notification-card lists.
- v0.13 normalizes Basic/Native and Target/Learning language roles across Settings, onboarding, quick voice, Translate, and card creation.
- v0.14 keeps Quick Dictionary and Translate language-pair buttons session-scoped, lets the active pair override recognition/translation/card creation, separates Quick Vocabulary lessons by pair, and keeps Split input at the bottom.
- v0.15 makes Quick Vocabulary lesson matching strict by Basic/Target pair and puts normal Translate output on top with input at the bottom.
- v0.16 adds local language setup after onboarding and a Settings manager for downloaded translation/speech languages, extra downloads, status hints, and translation-model removal.
- v0.17 adds optional OpenAI online models with separate settings for speech-to-text, translation/text, text-to-speech, TTS voice, base URL, and API key.
- v0.18 adds configurable OpenAI translation/TTS caching, cache clearing, cached audio metadata, and OpenAI STT waveform/progress feedback.
- v0.19 adds OpenAI cache/API technical messages and a two-day OpenAI activity log in Settings.
- v0.20 adds persistent card content cache, punctuation-insensitive OpenAI cache lookup, and configurable OpenAI STT silence timeout based on voice signal.
- v0.21 replaces the startup beep with a soft purr and keeps other app action sounds silent.
- v0.22 makes the startup purr more audible and adds ElevenLabs Belarusian TTS provider/model/voice/API-key settings.
- v0.23 adds a visible-side card audio download/share action for cached or newly generated TTS mp3 files.
- v0.24 fixes Belarusian OpenAI speech recognition routing to `be-BY` and logs safe ElevenLabs HTTP diagnostics for Belarusian TTS failures.
- v0.25 replaces the generated startup purr with the provided mp3 resource and makes the ElevenLabs BY voice ID manually editable.
- v0.26 routes card auto-translation and the manual T button through the configured translation provider, using OpenAI when enabled and keeping ElevenLabs audio-only.
- v0.27 gives enabled online OpenAI translation priority for automatic card and Translate paths, including Polish to Belarusian, even when critical offline mode is toggled on.
- v0.28 adds a 1 second OpenAI voice silence timeout option for faster speech recognition cutoff.
- v0.29 adds 1-10 second OpenAI voice silence timeout options, speeds online status detection, and stops current audio on app taps/navigation/new playback.
- v0.30 plays cached card-side TTS audio while offline and adds blinking card status dots for online, offline, and cached-audio states.
- v0.31 adds Latvian, Lithuanian, and Portuguese and lets Settings choose which languages use the special ElevenLabs TTS path.
- v0.32 makes new audio stop older audio, adds configurable card status blinking, shows cached audio with a black outline, expands ElevenLabs TTS selection to all app languages, and logs STT/translation/TTS model details on cards.
- v0.33 enriches OpenAI card translation with short explanation/rule/examples, copies answer-bar copy values to the clipboard, and fixes current-side answer language labels.
- v0.34 adds forced online/offline refresh taps, same-language microphone Mistake correction through the configured OpenAI text model, and Mistake long-press generation of Train cards.
- v0.35 adds Android text share-import from other apps, previews up to 100 words, and creates sentence or vocabulary cards for the current Basic/Target pair.

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

1. Confirm Settings shows `MurrLex v0.35`.
2. Confirm online Translate works with network enabled.
3. Disable network and confirm the title indicator is red.
4. Confirm manual text in Translate uses offline ML Kit translation.
5. Confirm microphone speech is recognized offline and translated offline.
6. Confirm Split mode microphones work on both sides.
7. Confirm Basic/Native text becomes the card front and Target/Learning text becomes the checked side.
8. Confirm changing the Quick Dictionary or Translate pair does not change Settings but does affect immediate recognition, translation, and new card/lesson creation.
9. Confirm OpenAI cache lookup ignores case and punctuation and the waveform stops during silence.
10. Confirm only the startup screen plays a soft purr; other app actions stay silent.
11. Confirm Belarusian playback uses ElevenLabs with voice `q19tj6dG7gitafffmfLO` when selected and keyed.
12. Confirm the card-side audio action exports only the side currently visible on the card.
13. Confirm Belarusian OpenAI speech recognition does not fall back to Russian.
14. Confirm ElevenLabs 403 appears as a safe diagnostic in the app log without exposing the API key.
15. Confirm startup purr is audible on app launch.
16. Confirm a new ElevenLabs voice ID can be typed and saved.
17. Confirm card auto-translation and the `T` button use OpenAI when enabled.
18. Confirm automatic Polish to Belarusian card translation fills without pressing `T` when OpenAI is enabled.
19. Confirm OpenAI voice silence timeout can be set to `1 second`.
20. Confirm OpenAI voice silence timeout offers 1 through 10 seconds.
21. Confirm app audio stops on tap, navigation away from Study, or another playback request.
22. Confirm cached card-side TTS audio plays while offline.
23. Confirm the Study card status dot blinks every five seconds and shows green online, red offline, and yellow when visible-side audio is cached while online.
24. Confirm Latvian, Lithuanian, and Portuguese appear in language pickers.
25. Confirm Special ElevenLabs TTS language chips can add/remove languages and enabled languages route generated speech through ElevenLabs.
26. Confirm new audio playback stops any older audio.
27. Confirm card status blink interval options include Off, 0.5, 1, 2, 3, 4, and 5 seconds.
28. Confirm cached audio uses a black outline and offline dots do not blink.
29. Confirm card info logs show STT, translation, and TTS model/voice details.
30. Confirm OpenAI card translation fills the card hint with explanation, rule, and examples.
31. Confirm the answer-bar copy icon also writes to the system clipboard.
32. Confirm Polish/Lithuanian answer input labels match the side being typed.
33. Confirm tapping `MurrLex` and the Study card language/status area refreshes online/offline status immediately.
34. Confirm same-language Quick Vocabulary microphone input creates a Mistake card with OpenAI correction details.
35. Confirm long-press on a Mistake card opens ten Train-card options and generated Train cards do not open the generator.
36. Confirm sharing text from another app into MurrLex opens the post import dialog and creates cards from up to 100 words.
