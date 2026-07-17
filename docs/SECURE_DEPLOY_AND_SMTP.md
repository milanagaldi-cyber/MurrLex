# Secure deploy and SMTP runbook

This document describes the staging setup. Existing root access and existing root SSH keys are deliberately left unchanged.

## Mental model

- `owner` is a named Linux account with a personal SSH key and a limited set of server-management commands.
- `deploy` is a named Linux account whose key can run only one operation: deploy the latest `Server_Main` revision.
- GitHub Actions uses its own deploy-only account. It never receives the root key.
- SMTP credentials live only in `/opt/MurrLex/.env` and are never committed.

Deploy-only access limits Linux commands, but deployed application code can still use application secrets. Protect `Server_Main` with pull-request review and protect the `staging` GitHub Environment with required reviewers.

## One-time server foundation

Run as root after the repository update:

```bash
cd /opt/MurrLex
bash deploy/access/install-access-control.sh
```

The installer creates `murrlex-owners` and `murrlex-deployers`, installs root-owned commands, validates sudo/SSH configuration, and reloads SSH. It does not edit `/root/.ssh/authorized_keys` and does not disable root login.

## Add a person

The person creates a key on their own computer:

```powershell
ssh-keygen -t ed25519 -f $HOME\.ssh\murrlex_deploy -C "NAME-murrlex-deploy"
Get-Content $HOME\.ssh\murrlex_deploy.pub
```

They send only the `.pub` line. An owner enters three values: Linux username, role, and public key.

```bash
sudo murrlex-access add ivan deploy
```

or:

```bash
sudo murrlex-access add ivan owner
```

The deploy-only user can run:

```powershell
ssh -i $HOME\.ssh\murrlex_deploy ivan@91.99.216.249 deploy
```

An interactive shell, port forwarding, another command, and password login are denied. Disable access without deleting audit history:

```bash
sudo murrlex-access disable ivan
```

## GitHub Actions

Create a dedicated key named `murrlex_github_deploy`, add its public half to a Linux user such as `github-deploy` with the `deploy` role, then create the GitHub Environment `staging`.

Add these four Environment secrets:

- `STAGING_SSH_HOST`: `91.99.216.249`
- `STAGING_SSH_USER`: `github-deploy`
- `STAGING_SSH_PRIVATE_KEY`: the complete private key file
- `STAGING_SSH_HOST_KEY`: the pinned `known_hosts` line for the VPS

Configure `staging` to allow only `Server_Main` and require an owner approval. Configure branch protection so updates reach `Server_Main` through reviewed pull requests. A manual `Deploy staging` workflow run then starts the forced deploy command. Automatic deploy-on-push can be enabled later, after branch protection and required review are proven.

## SMTP in five values

An owner runs:

```bash
sudo murrlex-smtp setup
```

The prompts request only:

1. SMTP host.
2. SMTP username/email.
3. SMTP password or app password.
4. Sender address.
5. Test recipient.

Port `587` and STARTTLS are selected automatically. The script backs up `.env`, keeps verification disabled, sends a real test, and enables email verification only after that test succeeds.

Useful checks:

```bash
sudo murrlex-smtp check
sudo murrlex-smtp check person@example.com
sudo murrlex-smtp disable
```

With verification enabled, a new password-based account remains inactive until the user opens the one-time email link. Google login continues to rely on Google's verified-email result and the staging allowlist.
