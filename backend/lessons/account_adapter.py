from allauth.account.adapter import DefaultAccountAdapter


class ClosedAllauthSignupAdapter(DefaultAccountAdapter):
    """Keep local sign-up on the allowlisted MurrLex registration route only."""

    def is_open_for_signup(self, request):
        return False
