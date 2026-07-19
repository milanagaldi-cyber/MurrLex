# Lexamora Studio Operations

## Runtime probes

- `GET /api/health` confirms that the Django process can serve HTTP.
- `GET /api/ready` checks the database and the private Studio storage. It returns
  HTTP 503 when either dependency is unavailable.
- Both endpoints return `X-Request-ID`. A valid caller-supplied request ID is
  preserved; otherwise the server generates one.

Do not route traffic to an instance until readiness returns HTTP 200.

## Logging

Application request logs are single-line JSON on standard output. They include the
request ID, method, path, status and duration. Request bodies, query strings,
prompts, passwords, tokens and provider keys are intentionally excluded. Audit and
private-asset access events inherit the same request ID.

Use the request ID to correlate Nginx, Gunicorn, application and Studio audit logs.

## Staging smoke test

Run after every deployment:

1. `manage.py check --deploy` with the production environment.
2. `manage.py migrate --noinput` and `manage.py collectstatic --noinput`.
3. Restart `murrlex-backend.service`, `murrlex-image-worker.service`,
   `murrlex-media-worker.service` and `murrlex-render-worker.service`; confirm all
   four are active.
4. Confirm `/api/health` and `/api/ready` return HTTP 200.
5. Confirm an unauthenticated `/studio/` request redirects to `/login/`.
6. Sign in, open an allowed workspace, create a temporary project and remove it.
7. Confirm a user without membership cannot fetch a project by UUID.
8. Inspect JSON logs and verify the response `X-Request-ID` is present.

## Security settings

Production defaults enable secure cookies, HTTPS redirect, HSTS, MIME sniffing
protection, same-origin referrers and frame denial. Nginx must pass
`X-Forwarded-Proto: https`. Keep secrets and provider credentials only in the
server environment and encrypted credential store.

For local HTTP development, `.env.example` explicitly disables HTTPS-only options.
Never use those local values on staging or production.
