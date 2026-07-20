import hashlib
import subprocess
import tempfile
import time
from pathlib import Path

from django.conf import settings
from django.core.files import File
from django.db import transaction
from django.utils import timezone
from django.utils.text import slugify

from .models import Asset, MovieRenderJob
from .revisions import audit


class MovieRenderingError(RuntimeError):
    pass


def _audio_profile(job):
    return getattr(job, "audio_profile", MovieRenderJob.AudioProfile.CLEAN_SPEECH)


def _target_lufs(job):
    value = int(getattr(job, "target_lufs", -16))
    return value if value in {-14, -16, -18, -23} else -16


def _clip_audio_cleanup(job):
    profile = _audio_profile(job)
    if profile == MovieRenderJob.AudioProfile.CLEAN_SPEECH:
        return (
            "highpass=f=80,lowpass=f=14000,"
            "afftdn=nf=-25:tn=1,"
            "acompressor=threshold=0.125:ratio=3:attack=20:release=250:makeup=1.5,"
        )
    if profile == MovieRenderJob.AudioProfile.BALANCED:
        return "highpass=f=40,acompressor=threshold=0.18:ratio=2:attack=20:release=250:makeup=1.2,"
    return ""


def _asset_kind(asset):
    content_type = str(getattr(asset, "content_type", "") or "")
    if content_type.startswith("audio/"):
        return "AUDIO"
    if content_type.startswith("video/"):
        return "VIDEO"
    metadata = getattr(asset, "media_metadata", {}) or {}
    if metadata.get("video"):
        return "VIDEO"
    source = getattr(getattr(asset, "proxy_file", None), "path", "") or getattr(
        getattr(asset, "file", None), "path", ""
    )
    if Path(str(source)).suffix.lower() in {".mp4", ".mov", ".m4v", ".webm", ".mkv", ".avi"}:
        return "VIDEO"
    return "AUDIO"


def timeline_duration_ms(snapshot, assets=None):
    clips = [
        clip for track in snapshot.get("tracks", []) for clip in track.get("clips", [])
        if not assets or _asset_kind(assets.get(str(clip.get("assetId")))) == "VIDEO"
    ]
    return max(
        (int(clip.get("start", 0)) + int(clip.get("duration", 0)) for clip in clips),
        default=0,
    )


def profile_dimensions(aspect_ratio, profile):
    long_edge = 1920 if profile == MovieRenderJob.Profile.REVIEW_1080 else 1280
    short_edge = 1080 if profile == MovieRenderJob.Profile.REVIEW_1080 else 720
    if aspect_ratio == "9:16":
        return short_edge, long_edge
    if aspect_ratio == "1:1":
        return short_edge, short_edge
    return long_edge, short_edge


def _source_path(asset):
    field = asset.proxy_file if asset.proxy_file and asset.processing_status == Asset.ProcessingStatus.READY else asset.file
    try:
        return Path(field.path)
    except (AttributeError, NotImplementedError) as exc:
        raise MovieRenderingError("Rough-cut rendering requires local private media storage") from exc


def build_render_command(job, assets, output_path):
    snapshot = job.snapshot
    duration_seconds = max(0.2, job.duration_ms / 1000)
    inputs = []
    clips = []
    for track_index, track in enumerate(snapshot.get("tracks", [])):
        if track.get("kind") not in {"VIDEO", "AUDIO"} or track.get("muted"):
            continue
        for clip_index, clip in enumerate(track.get("clips", [])):
            asset = assets[str(clip["assetId"])]
            input_index = len(clips)
            inputs.extend(["-i", str(_source_path(asset))])
            clips.append((track_index, clip_index, track, clip, asset, input_index))

    filters = [f"color=c=black:s={job.width}x{job.height}:r={job.fps}:d={duration_seconds:.3f}[base]"]
    video_label = "base"
    video_number = 0
    audio_labels = []
    video_clips = [item for item in clips if _asset_kind(item[4]) == "VIDEO"]
    visual_order = sorted(video_clips, key=lambda item: (-item[0], item[1]))
    for track_index, clip_index, track, clip, asset, input_index in visual_order:
        source_start = int(clip.get("sourceStart", 0)) / 1000
        clip_duration = int(clip.get("duration", 0)) / 1000
        timeline_start = int(clip.get("start", 0)) / 1000
        scale = min(4, max(0.5, float(clip.get("scale", 1))))
        position_x = min(100, max(-100, float(clip.get("positionX", 0))))
        position_y = min(100, max(-100, float(clip.get("positionY", 0))))
        horizontal_factor = 1 - position_x / 100
        vertical_factor = 1 - position_y / 100
        prepared = f"v{video_number}"
        composed = f"vc{video_number}"
        filters.append(
            f"[{input_index}:v]trim=start={source_start:.3f}:duration={clip_duration:.3f},"
            f"setpts=PTS-STARTPTS+{timeline_start:.3f}/TB,"
            f"scale=w='if(gte(a,{job.width}/{job.height}),ceil({job.height}*a*{scale:.4f}/2)*2,ceil({job.width}*{scale:.4f}/2)*2)':"
            f"h='if(gte(a,{job.width}/{job.height}),ceil({job.height}*{scale:.4f}/2)*2,ceil({job.width}/a*{scale:.4f}/2)*2)',"
            f"setsar=1,format=yuv420p[{prepared}]"
        )
        filters.append(
            f"[{video_label}][{prepared}]overlay=eof_action=pass:repeatlast=0:shortest=0:"
            f"x='if(gte(w,W),(W-w)/2*{horizontal_factor:.4f},(W-w)/2)':"
            f"y='if(gte(h,H),(H-h)/2*{vertical_factor:.4f},(H-h)/2)':"
            f"enable='between(t,{timeline_start:.3f},{timeline_start + clip_duration:.3f})'[{composed}]"
        )
        video_label = composed
        video_number += 1

    for track_index, clip_index, track, clip, asset, input_index in clips:
        source_start = int(clip.get("sourceStart", 0)) / 1000
        clip_duration = int(clip.get("duration", 0)) / 1000
        timeline_start = int(clip.get("start", 0)) / 1000
        volume = float(clip.get("volume", 1))
        has_audio = bool((asset.media_metadata or {}).get("audio"))
        if has_audio:
            audio_label = f"a{len(audio_labels)}"
            delay_ms = round(timeline_start * 1000)
            cleanup = _clip_audio_cleanup(job)
            filters.append(
                f"[{input_index}:a]atrim=start={source_start:.3f}:duration={clip_duration:.3f},"
                f"asetpts=PTS-STARTPTS,volume={volume:.4f},{cleanup}"
                f"adelay={delay_ms}|{delay_ms}[{audio_label}]"
            )
            audio_labels.append(audio_label)

    if audio_labels:
        joined = "".join(f"[{label}]" for label in audio_labels)
        mastering = ""
        if _audio_profile(job) != MovieRenderJob.AudioProfile.ORIGINAL:
            mastering = f"loudnorm=I={_target_lufs(job)}:TP=-1.5:LRA=11,"
        filters.append(
            f"{joined}amix=inputs={len(audio_labels)}:duration=longest:dropout_transition=0:normalize=0,"
            f"atrim=duration={duration_seconds:.3f},{mastering}aresample=48000[aout]"
        )
    else:
        filters.append(f"anullsrc=r=48000:cl=stereo,atrim=duration={duration_seconds:.3f}[aout]")

    return [
        settings.STUDIO_FFMPEG_BINARY, "-v", "error", "-y", *inputs,
        "-filter_complex", ";".join(filters),
        "-map", f"[{video_label}]", "-map", "[aout]",
        "-c:v", "libx264", "-preset", "veryfast", "-crf", "23",
        "-pix_fmt", "yuv420p", "-r", str(job.fps),
        "-c:a", "aac", "-b:a", "160k", "-ac", "2", "-ar", "48000",
        "-t", f"{duration_seconds:.3f}", "-movflags", "+faststart",
        "-progress", "pipe:1", "-nostats", str(output_path),
    ]


def _store_result(job, output_path):
    checksum = hashlib.sha256()
    with output_path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            checksum.update(chunk)
    filename = f"{slugify(job.title) or 'rough-cut'}-{str(job.id)[:8]}.mp4"
    asset = Asset(
        workspace=job.project.workspace,
        project=job.project,
        kind=Asset.Kind.EXPORT,
        original_filename=filename,
        content_type="video/mp4",
        size_bytes=output_path.stat().st_size,
        checksum_sha256=checksum.hexdigest(),
        width=job.width,
        height=job.height,
        duration_ms=job.duration_ms,
        media_metadata={
            "format": "mp4",
            "video": {"codec": "h264", "width": job.width, "height": job.height, "frameRate": job.fps},
            "audio": {
                "codec": "aac", "channels": 2, "sampleRate": 48000,
                "cleanupProfile": _audio_profile(job), "targetLufs": _target_lufs(job),
            },
        },
        processing_status=Asset.ProcessingStatus.READY,
        processing_finished_at=timezone.now(),
        created_by=job.requested_by,
        updated_by=job.requested_by,
    )
    with output_path.open("rb") as source:
        asset.file.save(filename, File(source), save=False)
    asset.full_clean()
    asset.save()
    asset.projects.add(job.project)
    return asset


def process_movie_render(job_id):
    with transaction.atomic():
        job = MovieRenderJob.objects.select_for_update().select_related("project__workspace", "requested_by").get(id=job_id)
        if job.cancel_requested:
            job.status = MovieRenderJob.Status.CANCELLED
            job.completed_at = timezone.now()
            job.save(update_fields=["status", "completed_at", "updated_at"])
            return job
        job.status = MovieRenderJob.Status.RUNNING
        job.progress = 1
        job.error_message = ""
        job.started_at = timezone.now()
        job.completed_at = None
        job.save(update_fields=["status", "progress", "error_message", "started_at", "completed_at", "updated_at"])

    assets = {str(asset.id): asset for asset in Asset.objects.filter(id__in={
        clip["assetId"] for track in job.snapshot.get("tracks", []) for clip in track.get("clips", [])
    })}
    process = None
    try:
        with tempfile.TemporaryDirectory(prefix="murrlex-render-") as directory:
            output_path = Path(directory) / "rough-cut.mp4"
            command = build_render_command(job, assets, output_path)
            process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1)
            process_started = time.monotonic()
            last_check = 0.0
            for raw_line in process.stdout:
                key, _, value = raw_line.strip().partition("=")
                if key in {"out_time_ms", "out_time_us"}:
                    try:
                        rendered_ms = int(value) // 1000
                    except ValueError:
                        rendered_ms = 0
                    progress = min(99, max(1, round(rendered_ms * 100 / max(1, job.duration_ms))))
                    MovieRenderJob.objects.filter(id=job.id, status=MovieRenderJob.Status.RUNNING).update(progress=progress)
                now = time.monotonic()
                if now - process_started > settings.STUDIO_MOVIE_RENDER_TIMEOUT_SECONDS:
                    process.kill()
                    raise MovieRenderingError("Movie rendering timed out")
                if now - last_check >= 0.5:
                    last_check = now
                    if MovieRenderJob.objects.filter(id=job.id, cancel_requested=True).exists():
                        process.terminate()
                        try:
                            process.wait(timeout=5)
                        except subprocess.TimeoutExpired:
                            process.kill()
                        MovieRenderJob.objects.filter(id=job.id).update(
                            status=MovieRenderJob.Status.CANCELLED, completed_at=timezone.now(), progress=0,
                        )
                        return MovieRenderJob.objects.get(id=job.id)
            stderr = process.stderr.read()
            return_code = process.wait(timeout=10)
            if return_code:
                detail = (stderr or "FFmpeg render failed").strip().splitlines()
                raise MovieRenderingError(detail[-1][:2000] if detail else "FFmpeg render failed")
            if not output_path.exists() or output_path.stat().st_size == 0:
                raise MovieRenderingError("FFmpeg did not create a render output")
            with transaction.atomic():
                job = MovieRenderJob.objects.select_for_update().select_related("project__workspace", "requested_by").get(id=job.id)
                asset = _store_result(job, output_path)
                job.output_asset = asset
                job.status = MovieRenderJob.Status.SUCCEEDED
                job.progress = 100
                job.completed_at = timezone.now()
                job.save(update_fields=["output_asset", "status", "progress", "completed_at", "updated_at"])
        audit(
            workspace=job.project.workspace, actor=job.requested_by, action="MOVIE_RENDER_SUCCEEDED",
            instance=job.project, metadata={"renderId": str(job.id), "assetId": str(job.output_asset_id)},
        )
        return job
    except Exception as exc:
        if process and process.poll() is None:
            process.kill()
        message = str(exc)[:2000] or "Movie rendering failed"
        MovieRenderJob.objects.filter(id=job_id).update(
            status=MovieRenderJob.Status.FAILED, error_message=message,
            completed_at=timezone.now(), progress=0,
        )
        raise MovieRenderingError(message) from exc
