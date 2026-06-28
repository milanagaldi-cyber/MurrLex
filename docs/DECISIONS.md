# Decisions

## Monorepo Kept

MurrLex remains a monorepo for now:

- `mobile/android` for Android.
- `backend` for the Django prototype.
- `connector` for Python sync/import experiments.
- `docs` for handoff and planning.

Reason: the product is still early and shared JSON/API decisions are evolving.

## SQLite / Backend Parked

The backend stays preserved but is not part of the current Android stabilization checkpoint.

Reason: the immediate goal is a stable mobile app and clean v0.02 thread.

## Android Version At Checkpoint

The visible Android version is `MurrLex v0.35`.

Reason: user requested every new Android build to increase the app version by `0.01`; v0.35 adds Android text share-import from other apps and converts shared posts into sentence or vocabulary cards for the current Basic/Target pair.

## Language Roles

Basic Language is also Native Language: it is the language the user knows. It is the default for interface language, quick vocabulary dictation, Translate dictation, future AI explanations, and generated documentation.

Target Language is also Learning Language: it is the language the user studies. LN cards should store Basic text on the front/native side and Target text on the correct/checked side.

Reason: speech recognition, Translate, card generation, and future AI document analysis must all use one consistent language direction unless the user explicitly chooses a different pair.

## Active Language Pair Overrides

Quick Dictionary and Translate language buttons change the current active Basic/Target pair only. They do not mutate global Settings.

The active pair wins for immediate speech recognition, translation, and card/lesson creation. Quick Vocabulary lessons are separated by strict active Basic/Target lesson metadata.

Reason: a user can temporarily work with another language pair without losing their Basic/Native and Target/Learning app settings.

## Local Language Libraries

After onboarding, the app offers to download local translation and speech recognition libraries for the default Basic/Target languages, or skip and work online.

Settings has a Local languages manager showing downloaded language code/status with translation and speech icons. ML Kit translation models can be removed from the app; Android speech recognition models are requested and tracked by MurrLex but removal is system-managed.

## OpenAI Model Settings

OpenAI online mode is optional and requires the device to be online, the setting enabled, and an API key present.

Speech-to-text, translation/text tasks, and text-to-speech each store their own model setting. The API key does not imply a model, and the app must never log the API key.

OpenAI translation/text and text-to-speech outputs are cacheable with metadata including model/voice/task. Speech-to-text requests are not cached because each voice input is treated as unique.

OpenAI activity is logged for the last two days and pruned automatically on read/write. The log must not include API keys.

Card text content is cached separately in persistent app files storage. It is not governed by the short OpenAI cache TTL and should be removed only when the card/lesson is deleted or the user explicitly clears app cache.

## App Sounds

The app should play sound only at startup, using a soft purr. Tap, swipe, success, and other action feedback should remain silent; vibration can still be controlled separately.

## Belarusian TTS

Belarusian speech playback can use ElevenLabs separately from OpenAI. Store provider, ElevenLabs API key, model, and voice independently; never write the ElevenLabs API key to logs.

Special ElevenLabs TTS is now language-list driven. Languages enabled in Settings use the ElevenLabs path before OpenAI/device TTS; Belarusian remains enabled by default for backward compatibility.

All app languages can be enabled for Special ElevenLabs TTS in Settings. Cached audio is no longer a yellow status; it is shown as a thin black outline around the normal online/offline dot.

## Card-Side Audio Export

Study cards can export the cached or newly generated TTS mp3 for the currently visible side. The action must stay side-specific so front/native and back/correct audio are not mixed.

Study card speech should reuse an existing cached side mp3 before attempting online or local playback, including when the phone is offline.

## Small Performance Refactors First

Prefer small measured Android performance refactors before broad file extraction.

Reason: the current bottleneck risk is repeated work in Compose/repository paths on weak devices; splitting files alone improves maintainability but does not make the app faster.

## Translator Online/Offline Direction

Translate/Split should use online translation by default through the current free Google Translate endpoint. Backend/OpenAI translation is intentionally parked for later.

Critical offline mode and loss of connectivity should use Android on-device speech recognition and ML Kit Google Translate with downloaded language models.

Reason: offline is a critical fallback, while normal quality should come from online translation until the future backend/OpenAI model is available.

## Safe Build Commands

The accepted validation commands are:

```powershell
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Reason: they are enough to prove the Android project compiles and lint blockers are absent.

## v0.02 Direction

The next phase should start with cleanup and stabilization, not new features.

Reason: the app has many experiments accumulated in large files; stable refactoring needs small commits and frequent builds.
