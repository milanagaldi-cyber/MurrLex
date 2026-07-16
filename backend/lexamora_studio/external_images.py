import ipaddress
import socket
from pathlib import Path
from urllib.parse import unquote, urljoin, urlparse

import requests
from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.files.uploadedfile import SimpleUploadedFile


ALLOWED_IMAGE_TYPES = {
    "image/jpeg": ".jpg",
    "image/png": ".png",
    "image/webp": ".webp",
}


def _validate_public_url(value):
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname:
        raise ValidationError("Enter a public HTTP or HTTPS image URL.")
    try:
        addresses = {
            item[4][0]
            for item in socket.getaddrinfo(
                parsed.hostname,
                parsed.port or (443 if parsed.scheme == "https" else 80),
                type=socket.SOCK_STREAM,
            )
        }
    except socket.gaierror as exc:
        raise ValidationError("The image host could not be resolved.") from exc
    for address in addresses:
        ip = ipaddress.ip_address(address)
        if not ip.is_global:
            raise ValidationError("Private and local network URLs are not allowed.")


def download_external_image(url):
    current = url.strip()
    session = requests.Session()
    session.headers["User-Agent"] = "MurrLex-Lexamora-Image-Import/1.0"
    for _ in range(4):
        _validate_public_url(current)
        try:
            response = session.get(current, stream=True, timeout=(8, 30), allow_redirects=False)
        except requests.RequestException as exc:
            raise ValidationError("The external image could not be downloaded.") from exc
        if response.is_redirect:
            location = response.headers.get("Location")
            response.close()
            if not location:
                raise ValidationError("The external image returned an invalid redirect.")
            current = urljoin(current, location)
            continue
        if response.status_code != 200:
            response.close()
            raise ValidationError(f"The external image returned HTTP {response.status_code}.")
        content_type = response.headers.get("Content-Type", "").split(";", 1)[0].strip().lower()
        if content_type not in ALLOWED_IMAGE_TYPES:
            response.close()
            raise ValidationError("The URL must point directly to a JPG, PNG or WEBP image.")
        limit = min(settings.STUDIO_MAX_UPLOAD_BYTES, settings.AI_MAX_IMAGE_BYTES)
        chunks = []
        size = 0
        for chunk in response.iter_content(64 * 1024):
            if not chunk:
                continue
            size += len(chunk)
            if size > limit:
                response.close()
                raise ValidationError(f"The external image exceeds the {limit}-byte limit.")
            chunks.append(chunk)
        response.close()
        if not chunks:
            raise ValidationError("The external image is empty.")
        source_name = Path(unquote(urlparse(current).path)).name
        stem = Path(source_name).stem[:120] or "external-image"
        suffix = ALLOWED_IMAGE_TYPES[content_type]
        return SimpleUploadedFile(f"{stem}{suffix}", b"".join(chunks), content_type=content_type)
    raise ValidationError("The external image redirected too many times.")
