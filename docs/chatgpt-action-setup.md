# ChatGPT Action Setup

Goal:

```text
ChatGPT
-> HTTPS tunnel
-> local Django backend
-> SQLite database
-> Lab UI / Android data workflow later
```

ChatGPT cannot call `http://127.0.0.1:8000` on your computer directly. It needs a public HTTPS URL. For local development, use a tunnel such as ngrok or Cloudflare Tunnel.

## 1. Start Django

```powershell
cd C:\CodexProjects\murrlex
Copy-Item .env.example .env
cd C:\CodexProjects\murrlex\backend
.\.venv\Scripts\python.exe manage.py runserver
```

Local checks:

```text
http://127.0.0.1:8000/api/health
http://127.0.0.1:8000/lab/imports/
```

## 2. Expose Django Through HTTPS

Example with ngrok:

```powershell
ngrok http 8000
```

ngrok will show a URL like:

```text
https://abc123.ngrok-free.app
```

Set `ALLOWED_HOSTS` in `.env` to include the tunnel host:

```text
ALLOWED_HOSTS=127.0.0.1,localhost,testserver,abc123.ngrok-free.app
CSRF_TRUSTED_ORIGINS=https://abc123.ngrok-free.app
```

Restart Django after changing `.env`.

For Cloudflare quick tunnels:

```text
ALLOWED_HOSTS=127.0.0.1,localhost,testserver,.trycloudflare.com
CSRF_TRUSTED_ORIGINS=https://*.trycloudflare.com
```

## 2.1. Protect Lab UI for a Colleague

The Lab UI requires Django login. Create a temporary user:

```powershell
cd C:\CodexProjects\murrlex\backend
$env:DJANGO_SUPERUSER_PASSWORD = "choose-a-temporary-password"
.\.venv\Scripts\python.exe manage.py createsuperuser --username methodist --email methodist@example.com --noinput
```

Send your colleague:

```text
https://YOUR-TUNNEL-URL/lab/lessons/
username: methodist
password: choose-a-temporary-password
```

Use a temporary password and change/delete this user after testing.

## 3. Prepare OpenAPI Schema

Open:

```text
C:\CodexProjects\murrlex\docs\chatgpt-action-openapi.yaml
```

Replace:

```text
https://YOUR-TUNNEL-URL
```

with your actual tunnel URL.

## 4. Create ChatGPT Action

In your custom GPT:

1. Open Configure.
2. Add Action.
3. Import or paste the OpenAPI schema.
4. Configure authentication as Bearer token.
5. Use the value from `.env`:

```text
INTERNAL_IMPORT_TOKEN=change-me-import-token
```

For real usage, change this token to your own value first.

## 5. Visual Test

Ask ChatGPT:

```text
Create one Make Mistake lesson with two cards and save it to Make Mistake.
```

Expected backend response:

```json
{
  "status": "ok",
  "lessonId": "...",
  "cardsImported": 2
}
```

Then open:

```text
http://127.0.0.1:8000/lab/lessons/
http://127.0.0.1:8000/lab/imports/
```

You should see the imported lesson and a success import log.

## Important Local Security Notes

- Do not publish `.env`.
- Use a temporary tunnel only while testing.
- Change `INTERNAL_IMPORT_TOKEN` before exposing the backend.
- This is still a local development bridge, not production deployment.
