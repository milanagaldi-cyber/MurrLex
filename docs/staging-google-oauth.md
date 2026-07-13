# Staging Google Sign-In

This integration is for the MurrLex staging environment. It uses only `openid`, `email`, and `profile`. Google access tokens and refresh tokens are not stored. The database keeps the django-allauth `SocialAccount` UID plus a minimal safe profile.

## Google Cloud

Create a Web application OAuth client and add this exact authorized redirect URI:

```text
https://ml-staging-api.lexaailabs.com/accounts/google/login/callback/
```

The browser login begins at `/accounts/google/login/`. Mobile clients can exchange a Google ID token through `POST /api/auth/google/` and receive the normal MurrLex access/refresh session payload.

## Server environment

```text
GOOGLE_OAUTH_ENABLED=true
GOOGLE_OAUTH_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_OAUTH_CLIENT_SECRET=your-secret
GOOGLE_OAUTH_REDIRECT_URI=https://ml-staging-api.lexaailabs.com/accounts/google/login/callback/
GOOGLE_OAUTH_TEST_ALLOWLIST_ENABLED=true
GOOGLE_OAUTH_ALLOWED_EMAILS=you@gmail.com,test@gmail.com
GOOGLE_OAUTH_ALLOWED_SUBS=
```

Never commit the real client ID or secret. Environment email matching is case-insensitive; Google `sub` matching is exact. When allowlisting is enabled and both env and database lists are empty, access is denied to everyone.

Test users can also be managed in Django Admin under **Google staging allowlist**. A successful first login creates a normal active user with no staff, superuser, or AI API grant.

## API request

```http
POST /api/auth/google/
Content-Type: application/json

{"id_token":"google-id-token","deviceName":"Android"}
```

The ID token is verified with `google-auth`, including audience, issuer, expiry, subject, and verified email. ID tokens and full claims are never logged or persisted.
