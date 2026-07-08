from django import forms
from django.contrib.auth import authenticate, get_user_model
from django.contrib.auth.forms import AuthenticationForm, UserCreationForm


class UsernameOrEmailAuthenticationForm(AuthenticationForm):
    username = forms.CharField(
        label="Username or email",
        widget=forms.TextInput(attrs={"autofocus": True}),
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
