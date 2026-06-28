# Next Tasks For Android_Main

Start with stabilization around the working Translate/Split behavior, not new features.

## Recommended First Task

Verify a clean Android install from the current debug APK:

1. Uninstall existing app or clear app data.
2. Install `mobile/android/app/build/outputs/apk/debug/app-debug.apk`.
3. Confirm the app name is MurrLex.
4. Confirm Settings shows version `0.11`.
5. Confirm bundled lessons show readable text.
6. Confirm opening a lesson displays cards.
7. Confirm Translate works online through the free Google Translate path.
8. Confirm offline speech recognition still captures speech after disabling network.
9. Confirm offline ML Kit translation returns translated text after speech recognition and after manual text input.
10. Confirm Split mode has microphone buttons on both sides and the translated-side microphone listens in the target language.

## Then Fix Safely

1. Centralize localization/settings text.
2. Remove or replace remaining corrupted legacy localized string literals.
3. Split `MainActivity.kt` in small verified chunks.
4. Split `MainViewModel.kt` only after UI extraction is stable.
5. Add a tiny smoke checklist for manual Android testing, including online/offline translator checks.

## Defer

- New features.
- Backend integration into mobile.
- Package id migration.
- Server deployment.
- Large UI redesign.
