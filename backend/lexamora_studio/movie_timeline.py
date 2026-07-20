import json


MOVIE_TIMELINE_SCHEMA_VERSION = 1
MAX_TIMELINE_BYTES = 1_000_000
MAX_TRACKS = 32
MAX_CLIPS = 1_000
MAX_CLIP_DURATION_MS = 24 * 60 * 60 * 1_000


class MovieTimelineValidationError(ValueError):
    pass


def default_movie_timeline():
    return {
        "schemaVersion": MOVIE_TIMELINE_SCHEMA_VERSION,
        "tracks": [
            {
                "id": "video-1",
                "name": "Video 1",
                "kind": "VIDEO",
                "muted": False,
                "locked": False,
                "height": 84,
                "clips": [],
            },
            {
                "id": "audio-1",
                "name": "Audio 1",
                "kind": "AUDIO",
                "muted": False,
                "locked": False,
                "height": 84,
                "clips": [],
            },
        ],
    }


def _bounded_text(value, *, field, maximum):
    text = str(value or "").strip()
    if not text or len(text) > maximum:
        raise MovieTimelineValidationError(f"{field} must contain between 1 and {maximum} characters.")
    return text


def _number(value, *, field, minimum=0, maximum=None):
    if isinstance(value, bool):
        raise MovieTimelineValidationError(f"{field} must be a number.")
    try:
        number = float(value)
    except (TypeError, ValueError):
        raise MovieTimelineValidationError(f"{field} must be a number.") from None
    if number < minimum or (maximum is not None and number > maximum):
        raise MovieTimelineValidationError(f"{field} is outside the supported range.")
    return int(round(number))


def _float_number(value, *, field, minimum, maximum):
    if isinstance(value, bool):
        raise MovieTimelineValidationError(f"{field} must be a number.")
    try:
        number = float(value)
    except (TypeError, ValueError):
        raise MovieTimelineValidationError(f"{field} must be a number.") from None
    if number < minimum or number > maximum:
        raise MovieTimelineValidationError(f"{field} is outside the supported range.")
    return number


def normalize_movie_timeline(value):
    if not isinstance(value, dict):
        raise MovieTimelineValidationError("Timeline must be a JSON object.")
    schema_version = value.get("schemaVersion", MOVIE_TIMELINE_SCHEMA_VERSION)
    if schema_version != MOVIE_TIMELINE_SCHEMA_VERSION:
        raise MovieTimelineValidationError(f"Timeline schema version {schema_version} is not supported.")
    tracks = value.get("tracks", [])
    if not isinstance(tracks, list) or len(tracks) > MAX_TRACKS:
        raise MovieTimelineValidationError(f"Timeline supports no more than {MAX_TRACKS} tracks.")

    normalized = dict(value)
    normalized["schemaVersion"] = MOVIE_TIMELINE_SCHEMA_VERSION
    normalized_tracks = []
    track_ids = set()
    clip_ids = set()
    clip_count = 0
    for track_index, track in enumerate(tracks):
        if not isinstance(track, dict):
            raise MovieTimelineValidationError(f"Track {track_index + 1} must be an object.")
        track_id = _bounded_text(track.get("id"), field="Track id", maximum=100)
        if track_id in track_ids:
            raise MovieTimelineValidationError(f"Track id {track_id} is duplicated.")
        track_ids.add(track_id)
        kind = str(track.get("kind") or "").upper()
        if kind not in {"VIDEO", "AUDIO", "TEXT"}:
            raise MovieTimelineValidationError(f"Track {track_id} has an unsupported kind.")
        clips = track.get("clips", [])
        if not isinstance(clips, list):
            raise MovieTimelineValidationError(f"Track {track_id} clips must be a list.")
        normalized_track = dict(track)
        normalized_track.update({
            "id": track_id,
            "name": str(track.get("name") or kind.title())[:100],
            "kind": kind,
            "muted": bool(track.get("muted", False)),
            "locked": bool(track.get("locked", False)),
            "height": _number(track.get("height", 84), field="Track height", minimum=56, maximum=200),
            "displayMode": str(track.get("displayMode") or "CLIPS").upper()
            if str(track.get("displayMode") or "CLIPS").upper() in {"CLIPS", "WAVEFORM"} else "CLIPS",
            "loudnessGuide": _float_number(
                track.get("loudnessGuide", 1), field="Track loudness guide", minimum=0, maximum=2,
            ),
        })
        normalized_clips = []
        for clip in clips:
            clip_count += 1
            if clip_count > MAX_CLIPS:
                raise MovieTimelineValidationError(f"Timeline supports no more than {MAX_CLIPS} clips.")
            if not isinstance(clip, dict):
                raise MovieTimelineValidationError(f"Track {track_id} contains an invalid clip.")
            clip_id = _bounded_text(clip.get("id"), field="Clip id", maximum=100)
            if clip_id in clip_ids:
                raise MovieTimelineValidationError(f"Clip id {clip_id} is duplicated.")
            clip_ids.add(clip_id)
            asset_id = _bounded_text(clip.get("assetId"), field="Clip asset id", maximum=100)
            normalized_clip = dict(clip)
            normalized_clip.update({
                "id": clip_id,
                "assetId": asset_id,
                "sourceAssetId": str(clip.get("sourceAssetId") or asset_id)[:100],
                "name": str(clip.get("name") or "Clip")[:200],
                "start": _number(clip.get("start", 0), field="Clip start"),
                "sourceStart": _number(clip.get("sourceStart", 0), field="Clip source start"),
                "duration": _number(
                    clip.get("duration", 0), field="Clip duration", minimum=200,
                    maximum=MAX_CLIP_DURATION_MS,
                ),
            })
            volume = clip.get("volume", 1)
            try:
                volume = float(volume)
            except (TypeError, ValueError):
                raise MovieTimelineValidationError("Clip volume must be a number.") from None
            if not 0 <= volume <= 2:
                raise MovieTimelineValidationError("Clip volume must be between 0 and 2.")
            normalized_clip["volume"] = volume
            normalized_clip["scale"] = _float_number(
                clip.get("scale", 1), field="Clip scale", minimum=0.05, maximum=8,
            )
            normalized_clip["positionX"] = _float_number(
                clip.get("positionX", 0), field="Clip horizontal position", minimum=-500, maximum=500,
            )
            normalized_clip["positionY"] = _float_number(
                clip.get("positionY", 0), field="Clip vertical position", minimum=-500, maximum=500,
            )
            normalized_clip["speed"] = _float_number(
                clip.get("speed", 1), field="Clip speed", minimum=0.1, maximum=8,
            )
            speed_method = str(clip.get("speedMethod") or "FRAME_SAMPLE").upper()
            normalized_clip["speedMethod"] = speed_method if speed_method in {
                "FRAME_SAMPLE", "FRAME_BLEND", "OPTICAL_FLOW",
            } else "FRAME_SAMPLE"
            normalized_clip["fadeIn"] = _number(
                clip.get("fadeIn", 0), field="Clip fade in", minimum=0, maximum=2_000,
            )
            normalized_clip["fadeOut"] = _number(
                clip.get("fadeOut", 0), field="Clip fade out", minimum=0, maximum=2_000,
            )
            keyframes = clip.get("volumeKeyframes", [])
            if not isinstance(keyframes, list) or len(keyframes) > 100:
                raise MovieTimelineValidationError("Clip volume keyframes must be a list with at most 100 points.")
            normalized_keyframes = []
            for point in keyframes:
                if not isinstance(point, dict):
                    raise MovieTimelineValidationError("Clip volume keyframe is invalid.")
                normalized_keyframes.append({
                    "time": _number(
                        point.get("time", 0), field="Volume keyframe time", minimum=0,
                        maximum=normalized_clip["duration"],
                    ),
                    "value": _float_number(
                        point.get("value", 1), field="Volume keyframe value", minimum=0, maximum=2,
                    ),
                })
            normalized_clip["volumeKeyframes"] = sorted(normalized_keyframes, key=lambda point: point["time"])
            normalized_clips.append(normalized_clip)
        normalized_track["clips"] = normalized_clips
        normalized_tracks.append(normalized_track)
    normalized["tracks"] = normalized_tracks
    if len(json.dumps(normalized, separators=(",", ":"), ensure_ascii=False).encode("utf-8")) > MAX_TIMELINE_BYTES:
        raise MovieTimelineValidationError("Timeline is too large.")
    return normalized


def movie_timeline_asset_ids(value):
    return {
        str(clip["assetId"])
        for track in value.get("tracks", [])
        for clip in track.get("clips", [])
        if clip.get("assetId")
    }
