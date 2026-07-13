# Lexamora Studio: API

Base path: `/api/v1/studio/`. JSON endpoints use the existing authenticated user.
Browser sessions require CSRF for unsafe methods. Lists are paginated and errors
use `{ "error": { "code": "...", "message": "...", "fields": {} } }`.

## Core endpoints

```text
GET/POST  workspaces
GET/PATCH workspaces/{workspace_id}
GET/POST  workspaces/{workspace_id}/members
PATCH     workspaces/{workspace_id}/members/{membership_id}

GET/POST  projects
GET/PATCH projects/{project_id}
GET/POST  projects/{project_id}/characters
GET/POST  projects/{project_id}/episodes
GET       projects/{project_id}/audit

GET/POST  episodes/{episode_id}/scenes
PATCH     scenes/{scene_id}
POST      scenes/reorder
GET/POST  scenes/{scene_id}/dialogue
GET/POST  scenes/{scene_id}/prompts
```

## Prompt AI

```text
POST prompts/{prompt_id}/improve
POST prompts/{prompt_id}/translate-dialogue
POST suggestions/{suggestion_id}/accept
POST suggestions/{suggestion_id}/reject
```

Improve accepts mode, selected block IDs and target model. It returns a suggestion,
never an overwritten prompt. HTTP 403 is returned without `can_use_ai`; HTTP 429
is returned when rate limited; HTTP 409 indicates a stale source revision.
`improve_translate_en` converts editable non-dialogue blocks to production English.
Dialogue translation accepts `targetLanguage` and `textModel`, writes only the
protected dialogue translation layer and leaves narrative prompt blocks unchanged.

The browser Studio settings page manages active OpenAI text models and mandatory
prompt templates. Provider keys remain in the encrypted MurrLex credential cabinet.

## Assets, history and exports

```text
POST assets
GET  assets/{asset_id}
GET  assets/{asset_id}/thumbnail
GET  assets/{asset_id}/download
GET  entities/{entity_type}/{entity_id}/revisions
GET  entities/{entity_type}/{entity_id}/revisions/{revision_id}/diff
POST entities/{entity_type}/{entity_id}/restore/{revision_id}
POST projects/{project_id}/export/pdf
GET  exports/{export_id}
GET  exports/{export_id}/download
```

Uploads use multipart form data. Downloads stream only after object-scoped
permission checks and create access/audit events.
Browser image actions support soft deletion to a project trash, restoration and
permanent byte deletion. Permanent deletion keeps a tombstone for referential and
audit integrity but removes the original and thumbnail from private storage.

## Subtitles and imports

```text
GET/POST episodes/{episode_id}/subtitle-tracks
GET/POST subtitle-tracks/{track_id}/lines
POST     subtitle-tracks/{track_id}/lines/reorder
POST     imports/docx
GET      imports/{import_id}
POST     imports/{import_id}/accept
```

DOCX acceptance is explicit because heading/table conventions are not guaranteed.

## Permission Matrix

- VIEWER: read and permitted download.
- TRANSLATOR: viewer plus translation/subtitle editing; cannot edit approved source.
- EDITOR: project content editing and asset upload.
- ADMIN: editor plus members and workflow administration.
- OWNER: full workspace control.
- AI/export actions additionally require the corresponding capability.
