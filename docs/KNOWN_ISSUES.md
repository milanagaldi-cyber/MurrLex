# Known Issues

## Encoding / Mojibake

Some legacy localized strings in Android source still contain mojibake such as `Ã...`. Recent fixes cleaned the speech language matching literals, but not every old Settings localization argument.

Risk:

- If those old strings are routed into the UI again, they may display incorrectly.
- Device-local data from older builds may already contain corrupted strings.

Suggested v0.02 approach:

- Do not mass-edit blindly.
- Move user-facing text into controlled resources/helpers.
- Test with a clean install or cleared app data.

## Large Android Files

`MainActivity.kt` and `MainViewModel.kt` are still too large. This makes feature work risky.

Suggested v0.02 approach:

- Extract one small area at a time.
- Run `lintDebug` and `assembleDebug` after each extraction.

## Legacy Package Id

The Android package/namespace remains `com.lexaprograms.polishcards` even though the product name is now MurrLex.

This may be fine for debug builds, but a release decision is needed later.

## Device-Dependent Features

Speech recognition, offline model download, TTS, and translation behavior depends on Android device support, installed services, permissions, and downloaded language models.

## Translator Mode Coupling

Translate/Split now has two paths:

- Online by default through the free Google Translate endpoint.
- Offline fallback through Android on-device speech recognition and ML Kit Google Translate downloaded models.

Risk:

- Future refactors can accidentally update only the red/green status UI while leaving translation or recognition on the wrong path.
- ML Kit offline translation must still pass through `downloadModelIfNeeded()` before `translate()`, even when the model is already downloaded.

Suggested approach:

- Keep the effective offline condition aligned as `state.useLocalTranslation || !isDeviceOnline`.
- Re-test both manual input and microphone input whenever translator code changes.
- Re-test Split mirrored target-side microphone after any Translate layout or language-picker changes.

## Backend Status

Backend and connector exist but are not the active mobile data source. Treat them as preserved prototypes until the user requests server integration again.
