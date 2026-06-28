# MurrLex Agent Handoff

This repository is the MurrLex monorepo. Use this file as the first stop for any clean Codex thread.

## Current Stable Checkpoint

- Local path: `C:\CodexProjects\murrlex`
- GitHub: `https://github.com/milanagaldi-cyber/MurrLex`
- Working branch for the next phase: `Android_Main`
- Android visible app version: `MurrLex v0.11`
- Latest verified Android build command: `gradlew.bat assembleDebug`
- Latest verified lint command: not rerun for v0.11; previous checkpoint used `gradlew.bat lintDebug`
- Debug APK: `C:\CodexProjects\murrlex\mobile\android\app\build\outputs\apk\debug\app-debug.apk`

## Current Android Translator Notes

- Translate/Split defaults to online translation through the free Google Translate endpoint while backend/OpenAI translation is parked for later.
- Critical offline fallback uses Android on-device speech recognition and ML Kit Google Translate downloaded language models.
- The red offline title dot must drive behavior, not only UI: offline translation/recognition should run when `!isDeviceOnline` even if the manual critical offline toggle is off.
- Split mode includes a mirrored microphone on the translated side; the conversation partner speaks in the target language.
- RU and BY language options intentionally use a white flag glyph.

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
