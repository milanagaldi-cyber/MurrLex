import json
import subprocess
import tempfile
from fractions import Fraction
from pathlib import Path

from django.conf import settings
from django.core.files import File
from django.db import transaction
from django.utils import timezone

from .models import Asset
from .revisions import audit


class MediaProcessingError(RuntimeError):
    pass


def _run(command):
    try:
        return subprocess.run(
            command,
            check=True,
            capture_output=True,
            text=True,
            timeout=settings.STUDIO_MEDIA_PROCESS_TIMEOUT_SECONDS,
        )
    except FileNotFoundError as exc:
        raise MediaProcessingError(f"Media tool is not installed: {command[0]}") from exc
    except subprocess.TimeoutExpired as exc:
        raise MediaProcessingError("Media processing timed out") from exc
    except subprocess.CalledProcessError as exc:
        detail = (exc.stderr or exc.stdout or "Media processing failed").strip().splitlines()
        raise MediaProcessingError(detail[-1][:1000] if detail else "Media processing failed") from exc


def _probe(path):
    result = _run([
        settings.STUDIO_FFPROBE_BINARY,
        "-v", "error",
        "-show_streams",
        "-show_format",
        "-of", "json",
        str(path),
    ])
    try:
        return json.loads(result.stdout)
    except (TypeError, json.JSONDecodeError) as exc:
        raise MediaProcessingError("FFprobe returned invalid metadata") from exc


def _integer(value, default=0):
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _float(value, default=0.0):
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _frame_rate(value):
    try:
        return round(float(Fraction(value)), 3) if value and value != "0/0" else None
    except (ValueError, ZeroDivisionError):
        return None


def _metadata(probe):
    streams = probe.get("streams") if isinstance(probe.get("streams"), list) else []
    video = next((item for item in streams if item.get("codec_type") == "video"), None)
    audio = next((item for item in streams if item.get("codec_type") == "audio"), None)
    format_data = probe.get("format") if isinstance(probe.get("format"), dict) else {}
    duration_seconds = _float(format_data.get("duration"))
    if not duration_seconds:
        duration_seconds = max((_float(item.get("duration")) for item in streams), default=0)
    metadata = {
        "format": format_data.get("format_name", ""),
        "bitRate": _integer(format_data.get("bit_rate")),
        "video": None,
        "audio": None,
    }
    if video:
        metadata["video"] = {
            "codec": video.get("codec_name", ""),
            "width": _integer(video.get("width")),
            "height": _integer(video.get("height")),
            "frameRate": _frame_rate(video.get("avg_frame_rate") or video.get("r_frame_rate")),
            "pixelFormat": video.get("pix_fmt", ""),
        }
    if audio:
        metadata["audio"] = {
            "codec": audio.get("codec_name", ""),
            "channels": _integer(audio.get("channels")),
            "sampleRate": _integer(audio.get("sample_rate")),
        }
    return metadata, max(0, round(duration_seconds * 1000)), video, audio


def _create_video_proxy(source, target):
    _run([
        settings.STUDIO_FFMPEG_BINARY, "-y", "-i", str(source),
        "-map", "0:v:0", "-map", "0:a:0?",
        "-vf", "scale=-2:720:force_original_aspect_ratio=decrease,pad=ceil(iw/2)*2:ceil(ih/2)*2",
        "-c:v", "libx264", "-preset", "veryfast", "-crf", "24",
        "-c:a", "aac", "-b:a", "128k", "-ac", "2",
        "-movflags", "+faststart", str(target),
    ])


def _create_audio_proxy(source, target):
    _run([
        settings.STUDIO_FFMPEG_BINARY, "-y", "-i", str(source),
        "-vn", "-c:a", "aac", "-b:a", "128k", "-ac", "2", "-ar", "48000",
        str(target),
    ])


def _create_thumbnail(source, target, duration_ms):
    seek = min(5, max(0, duration_ms / 10000))
    _run([
        settings.STUDIO_FFMPEG_BINARY, "-y", "-ss", f"{seek:.3f}", "-i", str(source),
        "-frames:v", "1", "-vf", "scale=640:-2:force_original_aspect_ratio=decrease",
        "-q:v", "3", str(target),
    ])


def _create_filmstrip(source, target, duration_ms):
    frame_count = max(1, min(120, (int(duration_ms) + 1999) // 2000))
    _run([
        settings.STUDIO_FFMPEG_BINARY, "-y", "-i", str(source),
        "-vf", (
            "fps=1/2:start_time=0,"
            "scale=72:72:force_original_aspect_ratio=increase,"
            "crop=72:72,"
            f"tile={frame_count}x1"
        ),
        "-frames:v", "1", "-q:v", "4", str(target),
    ])


def _create_waveform(source, target):
    _run([
        settings.STUDIO_FFMPEG_BINARY, "-y", "-i", str(source),
        "-filter_complex", "aformat=channel_layouts=mono,showwavespic=s=1200x160:colors=38d99b",
        "-frames:v", "1", str(target),
    ])


def _replace_file(field, source, filename):
    old_name = field.name
    with source.open("rb") as handle:
        field.save(filename, File(handle), save=False)
    if old_name and old_name != field.name:
        storage = field.storage
        transaction.on_commit(lambda: storage.exists(old_name) and storage.delete(old_name))


def process_media_asset(asset_id):
    with transaction.atomic():
        asset = Asset.all_objects.select_for_update().get(id=asset_id)
        if asset.deleted_at or asset.purged_at:
            return asset
        if not asset.content_type.startswith(("video/", "audio/")):
            asset.processing_status = Asset.ProcessingStatus.NOT_REQUIRED
            asset.save(update_fields=["processing_status", "updated_at"])
            return asset
        asset.processing_status = Asset.ProcessingStatus.PROCESSING
        asset.processing_error = ""
        asset.processing_attempts += 1
        asset.processing_started_at = timezone.now()
        asset.processing_finished_at = None
        asset.save(update_fields=[
            "processing_status", "processing_error", "processing_attempts",
            "processing_started_at", "processing_finished_at", "updated_at",
        ])

    try:
        source = Path(asset.file.path)
        probe = _probe(source)
        metadata, duration_ms, video_stream, audio_stream = _metadata(probe)
        if asset.content_type.startswith("video/") and not video_stream:
            raise MediaProcessingError("The uploaded file has no video stream")
        if asset.content_type.startswith("audio/") and not audio_stream:
            raise MediaProcessingError("The uploaded file has no audio stream")
        with tempfile.TemporaryDirectory(prefix="murrlex-media-") as directory:
            temporary = Path(directory)
            proxy = temporary / ("proxy.mp4" if video_stream else "proxy.m4a")
            if video_stream:
                _create_video_proxy(source, proxy)
                thumbnail = temporary / "thumbnail.jpg"
                _create_thumbnail(source, thumbnail, duration_ms)
                filmstrip = temporary / "filmstrip.jpg"
                _create_filmstrip(source, filmstrip, duration_ms)
            else:
                _create_audio_proxy(source, proxy)
                thumbnail = None
                filmstrip = None
            waveform = temporary / "waveform.png" if audio_stream else None
            if waveform:
                _create_waveform(source, waveform)

            with transaction.atomic():
                asset = Asset.all_objects.select_for_update().get(id=asset_id)
                if asset.deleted_at or asset.purged_at:
                    return asset
                _replace_file(asset.proxy_file, proxy, proxy.name)
                if thumbnail:
                    _replace_file(asset.thumbnail, thumbnail, thumbnail.name)
                if filmstrip:
                    _replace_file(asset.filmstrip_file, filmstrip, filmstrip.name)
                if waveform:
                    _replace_file(asset.waveform_file, waveform, waveform.name)
                asset.duration_ms = duration_ms
                asset.media_metadata = metadata
                if video_stream:
                    asset.width = metadata["video"]["width"] or asset.width
                    asset.height = metadata["video"]["height"] or asset.height
                asset.processing_status = Asset.ProcessingStatus.READY
                asset.processing_error = ""
                asset.processing_finished_at = timezone.now()
                asset.save()
        audit(
            workspace=asset.workspace, actor=asset.created_by, action="ASSET_MEDIA_READY",
            instance=asset, metadata={"durationMs": duration_ms, "hasAudio": bool(audio_stream)},
        )
        return asset
    except Exception as exc:
        message = str(exc)[:2000] or "Media processing failed"
        Asset.all_objects.filter(id=asset_id).update(
            processing_status=Asset.ProcessingStatus.FAILED,
            processing_error=message,
            processing_finished_at=timezone.now(),
        )
        raise MediaProcessingError(message) from exc
