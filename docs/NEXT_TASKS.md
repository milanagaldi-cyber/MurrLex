# Next Tasks For MurrLex v0.02

Start with stabilization, not new features.

## Recommended First Task

Verify a clean Android install from the current debug APK:

1. Uninstall existing app or clear app data.
2. Install `mobile/android/app/build/outputs/apk/debug/app-debug.apk`.
3. Confirm the app name is MurrLex.
4. Confirm Settings shows version `0.03`.
5. Confirm bundled lessons show readable text.
6. Confirm opening a lesson displays cards.

## Then Fix Safely

1. Centralize localization/settings text.
2. Remove or replace remaining corrupted legacy localized string literals.
3. Split `MainActivity.kt` in small verified chunks.
4. Split `MainViewModel.kt` only after UI extraction is stable.
5. Add a tiny smoke checklist for manual Android testing.

## Defer

- New features.
- Backend integration into mobile.
- Package id migration.
- Server deployment.
- Large UI redesign.
