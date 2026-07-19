# Lexamora Studio: Deployment

Lexamora Studio extends the existing Django service. It does not introduce another
reverse proxy, database or authentication service.

## Environment

Add only non-secret examples to `.env.example`:

```text
STUDIO_PRIVATE_MEDIA_ROOT=/opt/MurrLex/private-media
STUDIO_MAX_UPLOAD_BYTES=52428800
STUDIO_MEDIA_MAX_UPLOAD_BYTES=536870912
STUDIO_FFMPEG_BINARY=ffmpeg
STUDIO_FFPROBE_BINARY=ffprobe
STUDIO_MEDIA_PROCESS_TIMEOUT_SECONDS=1800
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

The tracked service and reverse-proxy templates live in `deploy/systemd/` and
`deploy/nginx/`. Image generation can legitimately take several minutes, so the
Gunicorn worker and Nginx upstream read/send timeouts are aligned at 300 seconds.
After changing either template, install it in `/etc`, run `systemctl daemon-reload`,
validate Nginx with `nginx -t`, then restart Gunicorn and reload Nginx.

Image generation itself is queued in the database and processed by the tracked
`murrlex-image-worker.service`. Its five worker threads are independent of browser
connections and authenticated page sessions. Deployments must migrate the database,
install/enable this unit and restart it after the Django web service.

Uploaded video and audio are processed by `murrlex-media-worker.service`. The worker
uses FFprobe for metadata and FFmpeg for immutable browser proxies, video thumbnails
and audio waveforms. The deploy command installs FFmpeg when it is missing, enables
the worker and aligns the Nginx request limit with `STUDIO_MEDIA_MAX_UPLOAD_BYTES`.

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



Operational probes and the post-deploy smoke matrix are documented in lexamora-studio-operations.md.
