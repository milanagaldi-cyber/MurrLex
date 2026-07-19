# Lexamora Draft Editor

The Draft Editor prepares clean rough cuts for finishing in CapCut. It is a first-party Lexamora Studio surface; the Twick experiment is not part of the user navigation.

## MVP boundary

The initial editor owns media selection, timeline editing, audio cleanup, proxy preview, rough-cut rendering and handoff files. Localization, advanced compositing, real-time collaborative editing and platform-variant generation are later phases.

## Timeline schema v1

`MovieTimeline.timeline` is a versioned JSON document. Times are integer milliseconds.

```json
{
  "schemaVersion": 1,
  "tracks": [
    {
      "id": "video-1",
      "name": "Video 1",
      "kind": "VIDEO",
      "muted": false,
      "locked": false,
      "clips": [
        {
          "id": "clip-1",
          "assetId": "asset-uuid",
          "name": "Interview opening",
          "start": 0,
          "sourceStart": 1200,
          "duration": 8000,
          "volume": 1.0
        }
      ]
    }
  ]
}
```

Supported v1 track kinds are `VIDEO`, `AUDIO` and `TEXT`. A timeline accepts up to 32 tracks, 1,000 clips and 1 MB of JSON. Clip identifiers and track identifiers are unique within a timeline. Referenced media must belong to the current project and be accessible to the current user.

Unknown fields are retained so later effects and proxy metadata can be introduced without discarding older projects. A schema version other than `1` is rejected until an explicit migration exists.

## Save and recovery

Before a changed timeline is saved, the server records the previous state in `MovieTimelineRevision`. The latest 50 states are retained. Restoring a revision first snapshots the current state, so restoration itself is reversible. A revision cannot be restored if its media is no longer accessible.

## Next implementation phase

Media ingest is handled asynchronously by `murrlex-media-worker.service`:

1. FFprobe records duration, codecs, frame rate, dimensions and audio properties.
2. FFmpeg creates a 720p H.264/AAC browser proxy for video or an AAC proxy for audio.
3. Video receives a JPEG thumbnail; media with audio receives a PNG waveform.
4. The editor polls queued/processing assets and enables dragging only after the proxy is ready.
5. A failed job keeps the original and can be retried from the Media Bin.

The next implementation phase expands timeline editing operations and uses the
waveform/proxy metadata for precise trimming and synchronized playback.
