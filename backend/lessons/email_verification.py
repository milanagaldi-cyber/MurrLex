from urllib.parse import urljoin

from allauth.account.models import EmailAddress
from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.tokens import default_token_generator
from django.core.exceptions import ObjectDoesNotExist
from django.core.mail import send_mail
from django.urls import reverse
from django.utils.encoding import force_bytes
from django.utils.http import urlsafe_base64_decode, urlsafe_base64_encode


def verification_url(user):
    path = reverse(
        "verify_registration_email",
        kwargs={
            "uidb64": urlsafe_base64_encode(force_bytes(user.pk)),
            "token": default_token_generator.make_token(user),
        },
    )
    return urljoin(f"{settings.PUBLIC_BASE_URL}/", path.lstrip("/"))


def send_registration_verification(user):
    link = verification_url(user)
    send_mail(
        f"{settings.EMAIL_SUBJECT_PREFIX}Confirm your MurrLex email",
        (
            f"Hello {user.get_username()},\n\n"
            "Confirm that this email belongs to you by opening this link:\n"
            f"{link}\n\n"
            "If you did not create this account, you can ignore this message."
        ),
        settings.DEFAULT_FROM_EMAIL,
        [user.email],
        fail_silently=False,
    )


def activate_from_token(uidb64, token):
    try:
        user_id = urlsafe_base64_decode(uidb64).decode()
        user = get_user_model().objects.get(pk=user_id)
    except (TypeError, ValueError, OverflowError, UnicodeDecodeError, ObjectDoesNotExist):
        return None

    if user.is_active or not default_token_generator.check_token(user, token):
        return None

    user.is_active = True
    user.save(update_fields=["is_active"])
    EmailAddress.objects.update_or_create(
        user=user,
        email=user.email,
        defaults={"verified": True, "primary": True},
    )
    return user
