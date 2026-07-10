from cryptography.fernet import Fernet, InvalidToken
from django.conf import settings


class CredentialConfigurationError(Exception):
    pass


def _fernet() -> Fernet:
    raw_key = settings.CREDENTIAL_ENCRYPTION_KEY.strip()
    if not raw_key:
        raise CredentialConfigurationError("Credential encryption is not configured on the server.")
    try:
        return Fernet(raw_key.encode("ascii"))
    except (ValueError, UnicodeEncodeError) as exc:
        raise CredentialConfigurationError("Credential encryption key is invalid on the server.") from exc


def encrypt_api_key(value: str) -> str:
    clean_value = value.strip()
    if not clean_value:
        return ""
    return _fernet().encrypt(clean_value.encode("utf-8")).decode("ascii")


def decrypt_api_key(value: str) -> str:
    if not value:
        return ""
    try:
        return _fernet().decrypt(value.encode("ascii")).decode("utf-8")
    except (InvalidToken, ValueError, UnicodeDecodeError) as exc:
        raise CredentialConfigurationError("Stored provider credential cannot be decrypted.") from exc


def get_provider_api_key(provider: str) -> str:
    # Import lazily so settings and the model registry are ready during Django startup.
    from .models import ProviderCredential

    credential = ProviderCredential.objects.filter(provider=provider, encrypted_api_key__isnull=False).first()
    return decrypt_api_key(credential.encrypted_api_key) if credential else ""
