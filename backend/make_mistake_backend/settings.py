import os
from pathlib import Path
from urllib.parse import unquote, urlparse

from django.core.exceptions import ImproperlyConfigured

# Build paths inside the project like this: BASE_DIR / 'subdir'.
BASE_DIR = Path(__file__).resolve().parent.parent


def load_env_file(path: Path) -> None:
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def env_bool(name: str, default: bool) -> bool:
    value = os.environ.get(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


def env_list(name: str, default: list[str]) -> list[str]:
    value = os.environ.get(name)
    if not value:
        return default
    return [item.strip() for item in value.split(",") if item.strip()]


def database_from_url(database_url: str | None) -> dict:
    if not database_url:
        return {
            "ENGINE": "django.db.backends.sqlite3",
            "NAME": BASE_DIR / "db.sqlite3",
        }

    parsed = urlparse(database_url)
    if parsed.scheme in {"postgres", "postgresql"}:
        return {
            "ENGINE": "django.db.backends.postgresql",
            "NAME": unquote(parsed.path.lstrip("/")),
            "USER": unquote(parsed.username or ""),
            "PASSWORD": unquote(parsed.password or ""),
            "HOST": parsed.hostname or "",
            "PORT": str(parsed.port or ""),
        }

    if parsed.scheme == "sqlite":
        db_path = unquote(parsed.path)
        return {
            "ENGINE": "django.db.backends.sqlite3",
            "NAME": db_path or BASE_DIR / "db.sqlite3",
        }

    raise ImproperlyConfigured(f"Unsupported DATABASE_URL scheme: {parsed.scheme}")


load_env_file(BASE_DIR.parent / ".env")
load_env_file(BASE_DIR / ".env")

SECRET_KEY = os.environ.get("SECRET_KEY", "dev-only-change-me")

DEBUG = env_bool("DEBUG", True)

if not DEBUG and SECRET_KEY == "dev-only-change-me":
    raise ImproperlyConfigured("SECRET_KEY must be set when DEBUG is false.")

ALLOWED_HOSTS = env_list("ALLOWED_HOSTS", ["127.0.0.1", "localhost", "testserver"])
CSRF_TRUSTED_ORIGINS = env_list("CSRF_TRUSTED_ORIGINS", [])
INTERNAL_IMPORT_TOKEN = os.environ.get("INTERNAL_IMPORT_TOKEN", "")
OPENAI_BASE_URL = os.environ.get("OPENAI_BASE_URL", "https://api.openai.com/v1").rstrip("/")
GOOGLE_AI_BASE_URL = os.environ.get("GOOGLE_AI_BASE_URL", "https://generativelanguage.googleapis.com/v1beta").rstrip("/")
OPENAI_TRANSLATION_MODEL = os.environ.get("OPENAI_TRANSLATION_MODEL", "gpt-5.4-mini")
ELEVENLABS_BASE_URL = os.environ.get("ELEVENLABS_BASE_URL", "https://api.elevenlabs.io/v1").rstrip("/")
CREDENTIAL_ENCRYPTION_KEY = os.environ.get("CREDENTIAL_ENCRYPTION_KEY", "")
JWT_SIGNING_KEY = os.environ.get("JWT_SIGNING_KEY", SECRET_KEY)
JWT_ACCESS_MINUTES = max(1, int(os.environ.get("JWT_ACCESS_MINUTES", "15")))
JWT_REFRESH_DAYS = max(1, int(os.environ.get("JWT_REFRESH_DAYS", "30")))
if not DEBUG and len(JWT_SIGNING_KEY) < 32:
    raise ImproperlyConfigured("JWT_SIGNING_KEY must be at least 32 characters when DEBUG is false.")
if not DEBUG and not CREDENTIAL_ENCRYPTION_KEY:
    raise ImproperlyConfigured("CREDENTIAL_ENCRYPTION_KEY must be set when DEBUG is false.")
AI_MAX_TEXT_CHARS = max(256, int(os.environ.get("AI_MAX_TEXT_CHARS", "16000")))
AI_MAX_AUDIO_BYTES = max(1024 * 1024, int(os.environ.get("AI_MAX_AUDIO_BYTES", str(25 * 1024 * 1024))))
AI_MAX_IMAGE_BYTES = max(1024 * 1024, int(os.environ.get("AI_MAX_IMAGE_BYTES", str(12 * 1024 * 1024))))
GOOGLE_OAUTH_CLIENT_ID = os.environ.get("GOOGLE_OAUTH_CLIENT_ID", "")
GOOGLE_OAUTH_CLIENT_SECRET = os.environ.get("GOOGLE_OAUTH_CLIENT_SECRET", "")
GOOGLE_OAUTH_ENABLED = env_bool("GOOGLE_OAUTH_ENABLED", bool(GOOGLE_OAUTH_CLIENT_ID))
GOOGLE_OAUTH_REDIRECT_URI = os.environ.get(
    "GOOGLE_OAUTH_REDIRECT_URI",
    "https://ml-staging-api.lexaailabs.com/accounts/google/login/callback/",
)
GOOGLE_OAUTH_TEST_ALLOWLIST_ENABLED = env_bool("GOOGLE_OAUTH_TEST_ALLOWLIST_ENABLED", True)
GOOGLE_OAUTH_ALLOWED_EMAILS = [value.lower() for value in env_list("GOOGLE_OAUTH_ALLOWED_EMAILS", [])]
GOOGLE_OAUTH_ALLOWED_SUBS = env_list("GOOGLE_OAUTH_ALLOWED_SUBS", [])
PUBLIC_SIGNUP_ENABLED = env_bool("PUBLIC_SIGNUP_ENABLED", False)
REGISTRATION_ALLOWLIST_ENABLED = env_bool("REGISTRATION_ALLOWLIST_ENABLED", True)
REGISTRATION_EMAIL_VERIFICATION_REQUIRED = env_bool("REGISTRATION_EMAIL_VERIFICATION_REQUIRED", False)
PUBLIC_BASE_URL = os.environ.get("PUBLIC_BASE_URL", "https://ml-staging-api.lexaailabs.com").rstrip("/")
LOGIN_URL = "/login/"
LOGIN_REDIRECT_URL = "/account/"
LOGOUT_REDIRECT_URL = "/login/"
ACCOUNT_LOGIN_REDIRECT_URL = "/account/"
ACCOUNT_LOGOUT_REDIRECT_URL = "/login/"
ACCOUNT_EMAIL_VERIFICATION = "none"
SOCIALACCOUNT_EMAIL_VERIFICATION = "none"
SOCIALACCOUNT_AUTO_SIGNUP = True
SOCIALACCOUNT_STORE_TOKENS = False
SOCIALACCOUNT_ADAPTER = "lessons.social_auth.StagingSocialAccountAdapter"
ACCOUNT_ADAPTER = "lessons.account_adapter.ClosedAllauthSignupAdapter"
SITE_ID = 1


# Application definition

INSTALLED_APPS = [
    "lessons",
    "lexamora_studio",
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.sites",
    "django.contrib.staticfiles",
    "allauth",
    "allauth.account",
    "allauth.socialaccount",
    "allauth.socialaccount.providers.google",
    "allauth.mfa",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "make_mistake_backend.observability.RequestIdMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "lessons.social_auth.GoogleOAuthGuardMiddleware",
    "allauth.account.middleware.AccountMiddleware",
    "lessons.middleware.StaffMFARequiredMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

AUTHENTICATION_BACKENDS = [
    "django.contrib.auth.backends.ModelBackend",
    "allauth.account.auth_backends.AuthenticationBackend",
]

SOCIALACCOUNT_PROVIDERS = {
    "google": {
        "SCOPE": ["openid", "email", "profile"],
        "AUTH_PARAMS": {"access_type": "online"},
        "OAUTH_PKCE_ENABLED": True,
        "APP": {
            "client_id": GOOGLE_OAUTH_CLIENT_ID,
            "secret": GOOGLE_OAUTH_CLIENT_SECRET,
            "key": "",
        },
    }
}

MFA_SUPPORTED_TYPES = ["totp", "recovery_codes"]
MFA_TOTP_ISSUER = "MurrLex"
MFA_RECOVERY_CODE_COUNT = 10
MFA_RECOVERY_CODES_SHOW_ONCE = True

ROOT_URLCONF = "make_mistake_backend.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
                "lessons.context_processors.oauth_status",
                "lexamora_studio.context_processors.studio_token_context",
            ],
        },
    },
]

WSGI_APPLICATION = "make_mistake_backend.wsgi.application"


DATABASES = {"default": database_from_url(os.environ.get("DATABASE_URL"))}


# Password validation
# https://docs.djangoproject.com/en/5.2/ref/settings/#auth-password-validators

AUTH_PASSWORD_VALIDATORS = [
    {
        "NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator",
    },
    {
        "NAME": "django.contrib.auth.password_validation.MinimumLengthValidator",
    },
    {
        "NAME": "django.contrib.auth.password_validation.CommonPasswordValidator",
    },
    {
        "NAME": "django.contrib.auth.password_validation.NumericPasswordValidator",
    },
]


# Internationalization
# https://docs.djangoproject.com/en/5.2/topics/i18n/

LANGUAGE_CODE = "en-us"

TIME_ZONE = "UTC"

USE_I18N = True

USE_TZ = True


# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/5.2/howto/static-files/

STATIC_URL = "static/"
STATIC_ROOT = BASE_DIR / "staticfiles"

# Default primary key field type
# https://docs.djangoproject.com/en/5.2/ref/settings/#default-auto-field

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

STUDIO_PRIVATE_MEDIA_ROOT = Path(os.environ.get("STUDIO_PRIVATE_MEDIA_ROOT", BASE_DIR / "private-media"))
STUDIO_MAX_UPLOAD_BYTES = max(1024 * 1024, int(os.environ.get("STUDIO_MAX_UPLOAD_BYTES", str(50 * 1024 * 1024))))
STUDIO_MEDIA_MAX_UPLOAD_BYTES = max(
    STUDIO_MAX_UPLOAD_BYTES,
    int(os.environ.get("STUDIO_MEDIA_MAX_UPLOAD_BYTES", str(512 * 1024 * 1024))),
)
STUDIO_FFMPEG_BINARY = os.environ.get("STUDIO_FFMPEG_BINARY", "ffmpeg")
STUDIO_FFPROBE_BINARY = os.environ.get("STUDIO_FFPROBE_BINARY", "ffprobe")
STUDIO_MEDIA_PROCESS_TIMEOUT_SECONDS = max(
    60, int(os.environ.get("STUDIO_MEDIA_PROCESS_TIMEOUT_SECONDS", "1800")),
)
STUDIO_MOVIE_RENDER_TIMEOUT_SECONDS = max(
    300, int(os.environ.get("STUDIO_MOVIE_RENDER_TIMEOUT_SECONDS", "7200")),
)
STUDIO_AI_RATE_PER_MINUTE = max(1, int(os.environ.get("STUDIO_AI_RATE_PER_MINUTE", "10")))
STUDIO_EXPORT_RATE_PER_HOUR = max(1, int(os.environ.get("STUDIO_EXPORT_RATE_PER_HOUR", "10")))
STUDIO_PDF_FONT_PATH = os.environ.get("STUDIO_PDF_FONT_PATH", "")

EMAIL_BACKEND = os.environ.get("EMAIL_BACKEND", "django.core.mail.backends.smtp.EmailBackend")
EMAIL_HOST = os.environ.get("EMAIL_HOST", "localhost")
EMAIL_PORT = int(os.environ.get("EMAIL_PORT", "25"))
EMAIL_HOST_USER = os.environ.get("EMAIL_HOST_USER", "")
EMAIL_HOST_PASSWORD = os.environ.get("EMAIL_HOST_PASSWORD", "")
EMAIL_USE_TLS = env_bool("EMAIL_USE_TLS", False)
EMAIL_USE_SSL = env_bool("EMAIL_USE_SSL", False)
if EMAIL_USE_TLS and EMAIL_USE_SSL:
    raise ImproperlyConfigured("EMAIL_USE_TLS and EMAIL_USE_SSL cannot both be enabled.")
DEFAULT_FROM_EMAIL = os.environ.get("DEFAULT_FROM_EMAIL", "Lexamora Studio <noreply@lexaailabs.com>")
SERVER_EMAIL = os.environ.get("SERVER_EMAIL", DEFAULT_FROM_EMAIL)
EMAIL_TIMEOUT = max(1, int(os.environ.get("EMAIL_TIMEOUT", "15")))
EMAIL_SUBJECT_PREFIX = os.environ.get("EMAIL_SUBJECT_PREFIX", "[MurrLex] ")
PASSWORD_RESET_TIMEOUT = max(60, int(os.environ.get("EMAIL_VERIFICATION_TIMEOUT", "86400")))

SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")
SECURE_SSL_REDIRECT = env_bool("SECURE_SSL_REDIRECT", not DEBUG)
SECURE_HSTS_SECONDS = max(0, int(os.environ.get("SECURE_HSTS_SECONDS", "31536000" if not DEBUG else "0")))
SECURE_HSTS_INCLUDE_SUBDOMAINS = env_bool("SECURE_HSTS_INCLUDE_SUBDOMAINS", False)
SESSION_COOKIE_SECURE = env_bool("SESSION_COOKIE_SECURE", not DEBUG)
CSRF_COOKIE_SECURE = env_bool("CSRF_COOKIE_SECURE", not DEBUG)
SECURE_CONTENT_TYPE_NOSNIFF = True
SECURE_REFERRER_POLICY = "same-origin"
X_FRAME_OPTIONS = "DENY"

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {"json": {"()": "make_mistake_backend.observability.JsonFormatter"}},
    "handlers": {"console": {"class": "logging.StreamHandler", "formatter": "json"}},
    "loggers": {
        "django.request": {"handlers": ["console"], "level": "WARNING", "propagate": False},
        "murrlex.request": {"handlers": ["console"], "level": "INFO", "propagate": False},
        "lexamora_studio": {"handlers": ["console"], "level": "INFO", "propagate": False},
    },
}
