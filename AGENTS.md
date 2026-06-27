# MurrLex Agent Handoff

This repository is the MurrLex monorepo. Use this file as the first stop for any clean Codex thread.

## Current Stable Checkpoint

- Local path: `C:\CodexProjects\murrlex`
- GitHub: `https://github.com/milanagaldi-cyber/MurrLex`
- Working branch for the next phase: `murrlex-0.02`
- Android visible app version: `MurrLex 0.03`
- Latest verified Android build command: `gradlew.bat assembleDebug`
- Latest verified lint command: `gradlew.bat lintDebug`

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
