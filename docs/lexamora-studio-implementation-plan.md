# Lexamora Studio: Implementation Plan

## Status

Phases 0-8 implemented on staging. This plan is based on the current `Server_Main`
code and the supplied `Master Document - Serial Dom (12.07.2026).docx`.

## Repository Audit

- Backend: Django 5.2, Python, Gunicorn, PostgreSQL in staging and SQLite locally.
- Frontend: server-rendered Django templates; there is no separate web SPA.
- Authentication: Django sessions, django-allauth and optional Google OAuth.
- Mobile authentication: short-lived JWT access tokens plus rotating hashed refresh
  sessions. This remains separate from browser session authentication.
- AI credentials: encrypted server-side `ProviderCredential`; keys are never sent
  to browsers or Android clients.
- Deployment: Nginx -> Gunicorn on the existing staging host.
- Existing business app: `lessons`. Studio must be a separate Django app and must
  not extend lesson/card models into production entities.

## Reference Document Findings

The reference DOCX contains 293 paragraphs, 15 tables, 14 inline images and two
sections. It combines series metadata, episode summaries, characters, ordered
scenes, dialogue, image/video prompts, generation models, additional generations,
subtitles, publication metadata and montage screenshots. The import format is
semi-structured, so DOCX ingestion must be staged and reviewable rather than
writing directly into canonical project records.

## Architecture Decisions

1. Add a new `studio` Django application.
2. Reuse Django users and browser sessions; do not create another user table or
   login flow.
3. Use workspace membership for every authorization decision. Never infer access
   from a guessed object ID.
4. Keep HTML UI under `/studio/` and JSON endpoints under `/api/v1/studio/`.
5. Use server-rendered templates and small progressive JavaScript interactions for
   MVP. A SPA can be introduced later without changing the API or data model.
6. Store original assets privately and stream them through permission-checked
   Django endpoints. Local filesystem is the first backend; storage is abstracted
   for later S3-compatible storage.
7. Store revisions and audit events as append-only database records.
8. AI improvements create suggestions. They never overwrite prompts automatically.
9. Use optimistic concurrency with an entity revision number or `updated_at`
   precondition for editors.
10. Parse imported DOCX into a draft import report before users accept entities.

## Delivery Phases

### Phase 0: Audit and design

- Produce architecture, data model, API, deployment and workflow documents.
- Record reference DOCX structure and risks.
- Confirm boundaries with existing lessons/mobile APIs.

Acceptance: documents agree on namespaces, ownership, permissions and deployment.

### Phase 1: Core workspace

- Add `studio` app, models and migrations for Workspace, Membership, Project,
  Character, Episode and Scene.
- Add role/capability service and object-scoped query helpers.
- Add dashboard, workspace, project, character, episode and scene CRUD.
- Seed AI model profiles for Nano Banana 2, Kling 3.0 and Veo 3.1 Fast.

Acceptance: membership isolation tests pass; editor can manage ordered scenes.

### Phase 2: Script and prompts

- Add DialogueLine, Prompt and PromptBlock.
- Add scene editor and structured prompt editor.
- Add status transitions and validation.

Acceptance: dialogue and prompt blocks preserve ordering and attribution.

### Phase 3: Private assets and generation records

- Add Asset, AdditionalGeneration and GenerationOutput.
- Validate MIME type, extension and size; calculate checksums.
- Generate thumbnails asynchronously or after upload within bounded limits.
- Add authenticated original/thumbnail downloads and audit events.

Acceptance: cross-workspace asset access is denied and logged.

### Phase 4: Revisions and audit

- Add generic Revision, AuditEvent and AccessEvent services.
- Produce text diffs and restore-as-new-revision behavior.
- Add soft-delete managers and concurrency checks.

Acceptance: update and restore history is complete and immutable from normal UI.

### Phase 5: AI prompt improvement

- Add suggestion and AI usage records.
- Add rate limiting and capability checks.
- Support block, non-dialogue and full-prompt improvement modes.
- Add compare, accept and reject flows.

Acceptance: dialogue is excluded by default; accepting creates a revision.

### Phase 6: Subtitles and translation

- Add subtitle tracks/lines and translation units.
- Add bulk paste, reorder and draft/final statuses.
- Mark translations stale after source dialogue changes.

Acceptance: source changes invalidate dependent translations and prompts.

### Phase 7: PDF export

- Add ExportJob and generated private assets.
- Render project/episode sections server-side with headers, footer and page numbers.
- Log generation and every download.

Acceptance: export is permission checked, reproducible and downloadable privately.

### Phase 8: Hardening and staging

- Complete unit/integration tests, security review and dependency audit.
- Add readiness checks, structured logs and request IDs.
- Apply migrations, collect static assets and deploy with the existing service.

Acceptance: required test matrix passes and staging smoke tests are recorded.

Status: implemented with readiness dependency checks, request IDs, structured safe logs, production security defaults and an operations runbook.

## Commit Strategy

Each phase is a separate logical commit. Schema, feature behavior and broad cleanup
must not be mixed. Deployment happens only after migrations and tests pass.

## Primary Risks

- The DOCX is visually structured but not a stable machine contract.
- SQLite cannot represent staging concurrency and locking behavior accurately.
- Large media files require quotas and eventually object storage.
- Generic revisions can leak data unless snapshots are permission-scoped.
- Prompt AI calls need both per-user capability and workspace membership checks.
- Existing docs contain stale backend descriptions; update them only alongside a
  verified implementation.

