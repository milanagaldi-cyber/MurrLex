# Next Recommended Tasks For MurrLex v0.02

## Highest Priority

### Confirmed From Code

1. Create/switch to a clean branch named `murrlex-0.02`.
2. Decide whether to commit the current v0.01 checkpoint changes as-is before starting v0.02.
3. Fix localization architecture properly.
   - Current emergency fix routes damaged UI labels through English fallback text.
   - Move user-visible strings into Android resources or a typed localization table.
   - Remove mojibake literals from source.
4. Split `MainActivity.kt`.
   - It is over 300 KB and contains too many responsibilities.
   - Suggested split: study UI, catalog UI, settings UI, translator UI, dialogs, onboarding, theme/helpers.
5. Split `MainViewModel.kt`.
   - Suggested split: lesson/session state, speech/voice state, translation state, settings state.

## Product / UX Tasks

### Inferred From Behavior

1. Re-test the whole Android app on a physical device after the encoding hotfix.
2. Verify whether locally stored lesson data is corrupted or only source UI strings were corrupted.
3. Stabilize the lesson editor UI.
4. Stabilize Translate and Split Translate layouts.
5. Confirm quick vocabulary flow:
   - source language;
   - target language;
   - where new cards are inserted;
   - whether translation is automatic.
6. Confirm test mode behavior:
   - answer shuffling;
   - editing from test mode;
   - star behavior after correct answer.
7. Confirm notification weighting and star logic.

## Backend / Connector Tasks

### Confirmed From Code

1. Add docs/tests for the backend translation endpoint.
2. Decide whether backend translation should be used by Android or only by connector/ChatGPT tooling.
3. Add an explicit JSON schema document for lesson/card payloads.
4. Add migration path from SQLite to PostgreSQL when needed.

### Inferred From Behavior

1. Revisit Cloudflare Tunnel/demo deployment once the local app state is stable.
2. Add basic user/account plan later, not immediately required for test system.

## Testing Tasks

### Confirmed From Code

1. Keep running:
   - Android `assembleDebug`;
   - Android `lintDebug`;
   - backend `manage.py check`;
   - backend `manage.py test`;
   - connector `unittest`.

### Inferred From Behavior

1. Add Android smoke tests or at least a manual checklist for:
   - app launch;
   - lesson catalog;
   - opening a lesson;
   - answering a card;
   - editing a card;
   - translator;
   - quick microphone;
   - Settings.

## Do Not Start v0.02 With

- New feature expansion before stabilizing strings and architecture.
- More large edits inside `MainActivity.kt`.
- Silent schema changes without updating docs.

