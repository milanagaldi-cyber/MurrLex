from django import template


register = template.Library()


@register.filter
def display_name(user):
    full_name = (user.get_full_name() or "").strip()
    if full_name:
        return full_name
    email = (getattr(user, "email", "") or "").strip()
    if "@" in email:
        return email.split("@", 1)[0]
    return email or user.get_username()


@register.filter
def user_details(user):
    parts = [display_name(user)]
    email = (getattr(user, "email", "") or "").strip()
    username = user.get_username()
    if email and email.casefold() != parts[0].casefold():
        parts.append(email)
    if username and username.casefold() not in {value.casefold() for value in parts}:
        parts.append(f"@{username}")
    return " · ".join(parts)
