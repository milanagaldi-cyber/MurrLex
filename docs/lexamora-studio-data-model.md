# Lexamora Studio: Data Model

All production identifiers are UUIDs. All mutable production entities include
`created_at`, `created_by`, `updated_at`, `updated_by`, `deleted_at` and
`deleted_by` where applicable.

## Ownership

- `Workspace`: name, slug, description, owner, settings.
- `WorkspaceMembership`: workspace, user, role, status, invitation metadata,
  `can_use_ai`, `can_export`, `can_manage_members`. Unique per workspace/user.
- Roles: OWNER, ADMIN, EDITOR, TRANSLATOR, VIEWER.

## Production hierarchy

- `Project`: workspace, type SERIES/STANDALONE_VIDEO, title, concept, original
  language, translation languages, rights/publication metadata, status.
- `Character`: project, name, description, visual description, position.
- `Episode`: project, number, title, summary, position. Standalone projects may
  have one technical episode hidden as an episode concept in UI.
- `Scene`: episode, number, title, hook, description, location, actions,
  performance notes, position, status.

## Script and prompts

- `DialogueLine`: scene, position, speaker/character, text, language, delivery,
  status.
- `AiModelProfile`: provider, name, model identifier, media type, active, defaults.
- `Prompt`: scene, AI model profile, prompt type IMAGE/VIDEO, status, position,
  needs_review.
- `PromptBlock`: prompt, type NARRATIVE/DIALOGUE_REFERENCE/NEGATIVE/AUDIO,
  position, content, source dialogue reference.

## Media and generation

- `Asset`: workspace, project, optional entity link, kind, storage key, original
  filename, MIME, bytes, checksum, width, height, thumbnail key, uploader.
- `AdditionalGeneration`: scene, reason, source asset, prompt, position, status.
- `GenerationOutput`: generation, asset, model metadata, position, is_final.

## Subtitles and translation

- `SubtitleTrack`: episode, language, kind WORKING/FINAL, status.
- `SubtitleLine`: track, position, text, optional start/end milliseconds.
- `TranslationUnit`: source entity type/id/revision, target language, source text,
  translated text, status DRAFT/IN_REVIEW/APPROVED/STALE.

## History and operations

- `Revision`: workspace, entity type/id, sequence, operation, snapshot JSON,
  changed fields, author, timestamp. Unique entity/sequence.
- `AuditEvent`: workspace, actor, action, entity type/id, request ID, IP hash,
  metadata, timestamp. Append-only.
- `AccessEvent`: workspace, actor, asset/export, action VIEW/DOWNLOAD, request ID.
- `AiSuggestion`: prompt/block, mode, source revision, suggestion, status,
  provider/model, usage and timestamps.
- `ExportJob`: project/episode, requested sections, status, output asset, error,
  requester and timestamps.
- `DocxImportJob`: source asset, parse status, parser version, report JSON and
  accepted timestamp. Parsed drafts are reviewed before canonical creation.

## Integrity Rules

- Child workspace must match parent workspace transitively.
- Positions are unique inside their parent and changed transactionally.
- Only one final GenerationOutput is allowed per AdditionalGeneration.
- Approved source dialogue changes mark translations STALE and related prompts
  `needs_review=True`.
- Soft-deleted rows are excluded from normal queries but retained in revisions.
- Asset and export downloads are always resolved through workspace membership.

