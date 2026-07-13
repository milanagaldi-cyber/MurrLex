# Lexamora Studio: Architecture

## Context

Lexamora Studio is a protected collaboration module in the existing MurrLex Django
deployment. It replaces a large Word production document with structured,
versioned records while preserving the current mobile and lesson APIs.

## Runtime Shape

```text
Browser
  -> HTTPS / Nginx
  -> Gunicorn / Django
       -> studio HTML views (/studio/)
       -> studio REST API (/api/v1/studio/)
       -> existing lessons and mobile APIs (unchanged)
       -> PostgreSQL
       -> private file storage
       -> encrypted provider credentials -> OpenAI
```

The first UI is Django templates with conventional forms and small JavaScript
enhancements. It shares the current design shell but has a dense production-tool
layout: workspace navigation, project context, ordered scene list and a primary
scene editor. No marketing landing page is introduced.

## Module Boundaries

- `studio.models`: production entities and durable workflow state.
- `studio.permissions`: capability and object-scope decisions.
- `studio.services`: transactional mutations, ordering and stale propagation.
- `studio.revisions`: snapshots, diffs and restore operations.
- `studio.audit`: append-only security and activity events.
- `studio.storage`: validated private upload/download abstraction.
- `studio.ai`: prompt assembly, provider call and suggestion lifecycle.
- `studio.exports`: PDF job and artifact generation.
- `studio.api`: versioned JSON views and serializers/validators.
- `studio.web`: session-authenticated HTML views and forms.

Views must not implement authorization by themselves. They obtain scoped querysets
or call a permission service and then invoke a transactional service.

## Authentication and Authorization

Browser access reuses Django session authentication and django-allauth. Studio
authorization is based on active WorkspaceMembership plus capabilities. Roles
provide defaults, while explicit booleans such as `can_use_ai`, `can_export` and
`can_manage_members` support controlled exceptions.

Every object lookup is scoped through a workspace accessible to the current user.
Direct `Model.objects.get(pk=...)` lookups are forbidden in request handlers.
Superusers may administer data but normal application behavior remains scoped.

## Consistency

Ordered records use integer positions and transactional reorder operations.
Editable entities carry `updated_at` and a revision sequence. Mutations compare a
client precondition and return HTTP 409 when another editor saved first.

Core entities use soft deletion (`deleted_at`, `deleted_by`). Default managers hide
deleted rows. Revisions and audit records are never cascade-deleted with soft
deleted content.

## Versioning

Revision records contain entity type, entity UUID, sequence, author, timestamp,
operation, normalized JSON snapshot and changed-field metadata. A restore writes a
new canonical state and a new revision; it never rewinds or deletes history.

## Files

Assets store metadata in PostgreSQL and bytes in private storage. Filenames are
generated UUID paths, not user paths. Uploads validate size, extension, MIME and
image decoding where applicable. Downloads are authenticated Django streaming
responses in MVP; the storage interface may later issue short-lived signed URLs.

## AI

AI calls reuse encrypted server provider credentials. A request requires active
membership, `can_use_ai`, a configured provider and rate-limit capacity. The
provider output is stored as an immutable suggestion with model, prompt version,
usage and response metadata. Accepting a suggestion is a separate user action.
Dialogue blocks are excluded from generic improvement. The explicit dialogue
translation action sends only direct speech, stores a separate translated layer and
never sends or rewrites the English narrative prompt. Prompt improvement can also
convert simple non-English source blocks to reviewed production English. Both
actions use an active OpenAI text model selected from shared Studio settings.

## Observability and Security

- Request ID middleware propagates an ID to logs and responses.
- Production errors return stable codes without stack traces or secrets.
- Sensitive reads, writes, downloads, exports and AI use create audit events.
- CSRF protects session-authenticated mutations.
- Same-origin browser deployment avoids broad CORS.
- Nginx and Django upload limits must agree.
- Readiness checks include database connectivity; health remains lightweight.
