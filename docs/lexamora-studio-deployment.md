# Lexamora Studio: Deployment

Lexamora Studio extends the existing Django service. It does not introduce another
reverse proxy, database or authentication service.

## Environment

Add only non-secret examples to `.env.example`:

```text
STUDIO_PRIVATE_MEDIA_ROOT=/opt/MurrLex/private-media
STUDIO_MAX_UPLOAD_BYTES=52428800
STUDIO_AI_RATE_PER_MINUTE=10
STUDIO_EXPORT_RATE_PER_HOUR=10
STUDIO_STORAGE_BACKEND=filesystem
```

Existing `DATABASE_URL`, `SECRET_KEY`, encrypted provider credentials and JWT
settings remain authoritative. Real values stay on the server.

## Staging procedure

1. Back up PostgreSQL and private media metadata.
2. Pull `Server_Main` with fast-forward only.
3. Install requirements into the existing virtual environment.
4. Run `manage.py check --deploy` with the production environment.
5. Run migrations with `--noinput` and collect static files.
6. Ensure private media is owned by the service account and is not public Nginx content.
7. Restart `murrlex-backend.service`.
8. Poll health, readiness and authenticated `/studio/`.
9. Verify unauthenticated access redirects to login and cross-workspace IDs leak no data.

## Rollback

Application rollback uses the previous Git commit only when migrations are
compatible. Destructive reverse migrations are not automatic. Restore the database
backup when schema rollback is required. Uploaded originals are retained.

## Production hardening gate

- HTTPS and secure session/CSRF cookies.
- PostgreSQL, not SQLite.
- restricted hosts and CSRF origins.
- aligned upload limits in Nginx and Django.
- private storage outside public static/media mappings.
- structured logs without prompts, secrets or provider keys.
- dependency audit, tests and permission smoke checks.

