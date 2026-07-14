from django import forms
from django.contrib.auth import authenticate, get_user_model
from django.contrib.auth.forms import AuthenticationForm, UserCreationForm

from .models import Card, Lesson, ProviderCredential
from .mfa_policy import user_requires_admin_mfa
from .signup_access import is_registration_email_allowed


class UsernameOrEmailAuthenticationForm(AuthenticationForm):
    username = forms.CharField(
        label="Username or email",
        widget=forms.TextInput(attrs={"autofocus": True, "placeholder": "Username or Email"}),
    )

    def clean(self):
        login_value = self.cleaned_data.get("username")
        password = self.cleaned_data.get("password")

        if login_value is not None:
            login_value = login_value.strip()
            self.cleaned_data["username"] = login_value

        if login_value is not None and password:
            username = login_value
            if "@" in login_value:
                user_model = get_user_model()
                matches = list(user_model.objects.filter(email__iexact=login_value))
                if len(matches) == 1:
                    username = matches[0].get_username()
                elif len(matches) > 1:
                    raise self.get_invalid_login_error()

            self.user_cache = authenticate(self.request, username=username, password=password)
            if self.user_cache is None:
                raise self.get_invalid_login_error()
            self.confirm_login_allowed(self.user_cache)

        return self.cleaned_data

    def confirm_login_allowed(self, user):
        super().confirm_login_allowed(user)
        if user.is_staff and user_requires_admin_mfa(user):
            raise forms.ValidationError(
                "This staff account must sign in through the secure Admin login.",
                code="staff_requires_mfa_login",
            )


class PublicRegistrationForm(UserCreationForm):
    email = forms.EmailField(required=True)

    class Meta(UserCreationForm.Meta):
        model = get_user_model()
        fields = ("username", "email")

    def clean_email(self):
        email = self.cleaned_data["email"].strip().lower()
        user_model = get_user_model()
        if user_model.objects.filter(email__iexact=email).exists():
            raise forms.ValidationError("A user with this email already exists.")
        if not is_registration_email_allowed(email):
            raise forms.ValidationError("This email is not invited to MurrLex closed testing.")
        return email


class AccountSettingsForm(forms.ModelForm):
    display_name = forms.CharField(
        label="Display name",
        required=False,
        max_length=150,
        help_text="Optional public-facing name for your cabinet.",
    )

    class Meta:
        model = get_user_model()
        fields = ("username", "email", "display_name")

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["display_name"].initial = self.instance.first_name

    def clean_email(self):
        email = self.cleaned_data["email"].strip().lower()
        user_model = get_user_model()
        if user_model.objects.exclude(pk=self.instance.pk).filter(email__iexact=email).exists():
            raise forms.ValidationError("A user with this email already exists.")
        return email

    def clean_username(self):
        username = self.cleaned_data["username"].strip()
        user_model = get_user_model()
        if user_model.objects.exclude(pk=self.instance.pk).filter(username__iexact=username).exists():
            raise forms.ValidationError("A user with this username already exists.")
        return username

    def save(self, commit=True):
        user = super().save(commit=False)
        user.first_name = self.cleaned_data["display_name"].strip()
        if commit:
            user.save()
        return user


class ProviderCredentialForm(forms.Form):
    provider = forms.ChoiceField(choices=ProviderCredential.Provider.choices)
    api_key = forms.CharField(
        required=False,
        widget=forms.PasswordInput(render_value=False, attrs={"autocomplete": "new-password"}),
        help_text="The key is encrypted before it is saved and is never shown again.",
    )
    clear_key = forms.BooleanField(required=False, label="Remove the saved key")

    def clean(self):
        cleaned = super().clean()
        if not cleaned.get("api_key") and not cleaned.get("clear_key"):
            raise forms.ValidationError("Enter a replacement key or select removal.")
        return cleaned


class LessonForm(forms.ModelForm):
    class Meta:
        model = Lesson
        fields = ("title", "source_language", "target_language", "card_kind", "lesson_info")


class CardForm(forms.ModelForm):
    class Meta:
        model = Card
        fields = (
            "native_value",
            "correct_value",
            "hint",
            "mistake",
            "card_kind",
            "source_language",
            "target_language",
            "stars",
        )


class TranslationForm(forms.Form):
    source_language = forms.CharField(max_length=100)
    target_language = forms.CharField(max_length=100)
    text = forms.CharField(widget=forms.Textarea(attrs={"rows": 7}))


class SpeechForm(forms.Form):
    text = forms.CharField(widget=forms.Textarea(attrs={"rows": 6}))
    model = forms.CharField(initial="gpt-4o-mini-tts")
    voice = forms.CharField(initial="coral")


class TranscriptionForm(forms.Form):
    audio = forms.FileField()
    model = forms.CharField(initial="gpt-4o-mini-transcribe")
    language = forms.CharField(required=False, max_length=20)


class ImageTextForm(forms.Form):
    image = forms.ImageField()
    model = forms.CharField(initial="gpt-4o-mini")
