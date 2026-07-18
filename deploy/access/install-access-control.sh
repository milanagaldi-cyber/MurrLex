#!/usr/bin/env bash
set -Eeuo pipefail

[[ "${EUID}" -eq 0 ]] || { echo "Run as root." >&2; exit 1; }
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

visudo -cf "${SCRIPT_DIR}/murrlex-deployers.sudoers"
visudo -cf "${SCRIPT_DIR}/murrlex-owners.sudoers"

groupadd --force murrlex-owners
groupadd --force murrlex-deployers
install -o root -g root -m 0755 "${SCRIPT_DIR}/murrlex-deploy" /usr/local/sbin/murrlex-deploy
install -o root -g root -m 0755 "${SCRIPT_DIR}/murrlex-deploy-key" /usr/local/sbin/murrlex-deploy-key
install -o root -g root -m 0755 "${SCRIPT_DIR}/murrlex-access" /usr/local/sbin/murrlex-access
install -o root -g root -m 0755 "${SCRIPT_DIR}/murrlex-smtp" /usr/local/sbin/murrlex-smtp
install -o root -g root -m 0440 "${SCRIPT_DIR}/murrlex-deployers.sudoers" /etc/sudoers.d/murrlex-deployers
install -o root -g root -m 0440 "${SCRIPT_DIR}/murrlex-owners.sudoers" /etc/sudoers.d/murrlex-owners
install -o root -g root -m 0644 "${SCRIPT_DIR}/90-murrlex-deployers.conf" /etc/ssh/sshd_config.d/90-murrlex-deployers.conf

sshd -t
systemctl reload ssh.service
echo "Access-control foundation installed. Existing root settings and authorized_keys were not changed."
