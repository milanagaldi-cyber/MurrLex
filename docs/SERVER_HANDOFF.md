# MurrLex Server Handoff

## AI Gateway

The current backend is Django and serves the Android online AI path. The app has no OpenAI or ElevenLabs provider keys. It sends the selected model, task payload, and its authenticated MurrLex session to the server. The Android session values are encrypted with Android Keystore before being written to local preferences.

The server reads provider credentials exclusively from `.env`:

```text
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_API_KEY=
ELEVENLABS_BASE_URL=https://api.elevenlabs.io/v1
ELEVENLABS_API_KEY=
JWT_SIGNING_KEY=
JWT_ACCESS_MINUTES=15
JWT_REFRESH_DAYS=30
```

Never add any of these values to Android resources, shared preferences, logs, crash reports, or Git.

## Mobile Contract

`POST /api/auth/register` and `POST /api/auth/login` return an access token, rotating refresh token, expiry, and safe user profile. `POST /api/auth/refresh` rotates the refresh token. `POST /api/auth/logout` revokes its refresh session.

All AI routes require a bearer access token:

```text
POST /api/ai/text
POST /api/ai/transcribe
POST /api/ai/speech
POST /api/ai/image-text
```

The app selects the model and voice. The server validates allowed model IDs and makes the provider request. Text/image/transcription responses are JSON. Speech returns `audio/mpeg` and safe provider/model/voice response headers.

Offline Android behavior remains local: Android SpeechRecognizer, downloaded ML Kit Translate models, device TTS, and cached audio. Online behavior requires a valid MurrLex server session.

## Deployment

Use PostgreSQL for shared production sessions, migrate before release, run behind HTTPS, set `DEBUG=false`, and use a long independent `JWT_SIGNING_KEY`. Access and refresh endpoints must be rate-limited at the reverse proxy or application edge before public rollout.
