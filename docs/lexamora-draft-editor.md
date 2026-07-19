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

## Timeline editing

The editor uses a horizontally scrollable millisecond timeline backed by the
versioned schema above. The current editing surface supports:

- multiple compatible video and audio tracks with mute and lock controls;
- dragging media from the Media Bin or double-clicking it into the rough cut;
- moving clips along a track or between tracks of the same media type;
- left and right source trimming without modifying the original file;
- splitting the selected clip at the playhead;
- snapping to the playhead, clip starts, clip ends and timeline zero;
- adaptive zoom from 6 to 80 pixels per second;
- frame stepping, playhead scrubbing and synchronized proxy playback;
- audio waveform display, per-clip volume and precise inspector values;
- keyboard actions: Space to play/pause, S to split, Delete to remove,
  arrows to step, and Ctrl/Cmd plus or minus to zoom.

The server rejects track/media type mismatches, inaccessible media, media that
is not ready, and source ranges extending past the probed source duration.

## Rough-cut rendering

Saved timelines can be queued as immutable server-side MP4 render jobs. The
editor offers a fast 720p draft profile and a 1080p review profile. Each job
stores the exact timeline snapshot, canvas, frame rate and source trim values,
so later edits cannot silently change an already queued export.

`murrlex-render-worker.service` composes jobs with FFmpeg independently from the
media ingest worker. Video clips are scaled and padded to the selected canvas,
overlapping video tracks are composited, and enabled sound is delayed, mixed and
volume-adjusted on the shared timeline. A silent stereo track is generated when
the edit contains no audio.

The editor shows queue and render progress. Users with export permission can
cancel active work, retry failed or cancelled jobs, and download successful MP4
assets from protected workspace storage.
