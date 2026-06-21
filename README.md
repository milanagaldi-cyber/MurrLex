# Make Mistake

Make Mistake is a language-learning product built around mistake cards.

This repository is intentionally structured as a monorepo so the Android app, future backend, shared JSON formats, and documentation can evolve together.

## Structure

```text
MakeMistake/
  mobile/
    android/   Android app, Kotlin + Jetpack Compose
  server/      Future backend service
  docs/        Product and technical documentation
```

## Android App

Open this folder in Android Studio:

```text
C:\CodexProjects\MakeMistake\mobile\android
```

The current Android version is `0.47`.

Debug APK path after build:

```text
mobile/android/app/build/outputs/apk/debug/app-debug.apk
```

## Backend

The `server/` folder is reserved for the future server side. Keeping it in the same repository for now makes it easier to share API contracts, lesson JSON examples, changelog notes, and release coordination with the mobile app.

If the backend later needs independent deployment, permissions, or ownership, it can be split into a separate repository without changing the Android app history.
