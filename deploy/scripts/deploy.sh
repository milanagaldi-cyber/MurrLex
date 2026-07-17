#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/opt/MurrLex"
BRANCH="Server_Main"
BACKUP_DIR="$APP_DIR/backups"

cd "$APP_DIR"
test -z "$(git status --porcelain --untracked-files=no)"
git fetch origin "$BRANCH"
git merge --ff-only "origin/$BRANCH"

mkdir -p "$BACKUP_DIR"
if [[ -f backend/db.sqlite3 ]]; then
  cp --preserve=mode,timestamps backend/db.sqlite3 "$BACKUP_DIR/db-$(date -u +%Y%m%dT%H%M%SZ)-pre-deploy.sqlite3"
fi

cd backend
./.venv/bin/python -m pip install -r requirements.txt
./.venv/bin/python manage.py migrate --noinput
./.venv/bin/python manage.py bootstrap_roles
./.venv/bin/python manage.py collectstatic --noinput
DEBUG=true GOOGLE_OAUTH_ENABLED=false GOOGLE_OAUTH_CLIENT_ID= GOOGLE_OAUTH_CLIENT_SECRET= \
  ./.venv/bin/python manage.py test

sudo /usr/bin/install -m 0644 "$APP_DIR/deploy/systemd/murrlex-credit-refill.service" /etc/systemd/system/murrlex-credit-refill.service
sudo /usr/bin/install -m 0644 "$APP_DIR/deploy/systemd/murrlex-credit-refill.timer" /etc/systemd/system/murrlex-credit-refill.timer
sudo /usr/bin/systemctl daemon-reload
sudo /usr/bin/systemctl enable --now murrlex-credit-refill.timer

sudo /usr/bin/systemctl restart murrlex-backend.service
sudo /usr/bin/systemctl is-active --quiet murrlex-backend.service
sudo /usr/bin/systemctl restart murrlex-image-worker.service
sudo /usr/bin/systemctl is-active --quiet murrlex-image-worker.service
