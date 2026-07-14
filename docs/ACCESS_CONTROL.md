# MurrLex Access Control

## Account Layers

MurrLex keeps four different levels separate:

1. Application users use the site and mobile API. They do not receive Django or SSH access.
2. Django staff use individual accounts, role groups, and TOTP when the Admin 2FA policy is enabled.
3. Django superusers have full application administration and are reserved for owners and recovery.
4. Linux owner/deploy accounts manage infrastructure. They are unrelated to Django users.

Nginx is the outer web door. Django permissions decide what a signed-in staff member may do. SSH is a separate operating-system entrance and does not pass through Nginx.

## Roles

Run `python manage.py bootstrap_roles` after migrations. It creates or synchronizes:

- `Daily Admin`: current broad routine administration without provider secrets or group control.
- `Support`: user status, sessions, allowlist, API access, subscription and credit visibility.
- `Billing`: subscriptions, immutable credit adjustments, usage, and security audit visibility.
- `Content`: lessons, cards, imports, and Studio content.
- `ReadOnly`: view-only access without provider credentials.
- `Developer`: technical read-only diagnostics.
- `Superadmin`: all Django permissions; this does not automatically set `is_superuser`.

Permissions are deny-by-default. New models do not automatically become editable by operational roles.

## User Lifecycle

Physical deletion of users is disabled in Django Admin. Authorized staff use actions on the Users list:

- Block selected users
- Unblock selected users
- Mark selected users for deletion
- Grant 100 credits

Critical actions require a written reason and create an `AdminAuditLog` entry. Staff and superuser targets are excluded from bulk status and credit actions. Blocking also revokes active mobile API sessions.

Credits are stored as an immutable ledger. A correction creates an opposite reversal entry instead of rewriting history. Subscription status changes are also performed through audited actions.

## Admin 2FA Recovery

Keep recovery codes outside the server in a password manager. If a staff member loses the device:

1. Verify the person through a separate trusted channel.
2. A superuser runs `python manage.py reset_staff_mfa USERNAME --actor OWNER --reason "verified reason"`.
3. The staff member signs in and enrolls TOTP again.
4. Confirm the `staff_mfa_reset` entry in Security Audit Log.

Run `python manage.py review_access` monthly or quarterly. Review staff, superusers, TOTP, break-glass markers, last login, groups, GitHub access, SSH keys, VPN users, and shared Basic Auth credentials.

## Deployment Boundary

The repository contains a manual GitHub Actions workflow and examples for an eventual restricted local runner. They are intentionally inert until a server owner:

1. Creates the Linux `owner` and `deploy` accounts.
2. Tests key-only access in separate sessions.
3. Installs the validated sudoers rule.
4. Installs and labels a self-hosted GitHub runner as `murrlex-staging`.
5. Protects the GitHub `staging` environment and `Server_Main` branch.

Never install the example SSH hardening file before owner and deploy key login have both been proven. Otherwise root/password access could be disabled before a replacement entrance works.
