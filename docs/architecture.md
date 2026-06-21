# Architecture

Make Mistake is currently a mobile-first app with local JSON lessons.

Planned repository shape:

- `mobile/android`: Kotlin + Jetpack Compose Android client.
- `server`: future backend.
- `docs`: shared product and technical notes.

The monorepo approach is deliberate for the early stage: mobile and server changes will likely touch the same JSON contracts and product behavior.
