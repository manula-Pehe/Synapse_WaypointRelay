#!/usr/bin/env bash
# One-time setup for the Waypoint Relay server (Ubuntu 24.04, x86_64).
# Installs Docker, Caddy, swap, firewall and clones the dev/prod checkouts.
# Usage: bash server-setup.sh
set -euo pipefail

REPO_URL="https://github.com/manula-Pehe/Synapse_WaypointRelay.git"
BASE_DIR="/opt/waypoint"
SWAP_SIZE="2G"

echo "==> System update"
sudo apt-get update -y
sudo DEBIAN_FRONTEND=noninteractive apt-get upgrade -y
sudo apt-get install -y ca-certificates curl gnupg git ufw debian-keyring debian-archive-keyring apt-transport-https
sudo timedatectl set-timezone Asia/Colombo

echo "==> Swap (${SWAP_SIZE})"
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l "${SWAP_SIZE}" /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

echo "==> Docker"
if ! command -v docker >/dev/null; then
  sudo install -m 0755 -d /etc/apt/keyrings
  sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  sudo chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
  sudo apt-get update -y
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
sudo mkdir -p /etc/docker
echo '{ "log-driver": "json-file", "log-opts": { "max-size": "10m", "max-file": "3" } }' | sudo tee /etc/docker/daemon.json >/dev/null
sudo systemctl enable --now docker
sudo systemctl restart docker
sudo usermod -aG docker "$USER"

echo "==> Caddy"
if ! command -v caddy >/dev/null; then
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list >/dev/null
  sudo chmod o+r /usr/share/keyrings/caddy-stable-archive-keyring.gpg /etc/apt/sources.list.d/caddy-stable.list
  sudo apt-get update -y
  sudo apt-get install -y caddy
fi
sudo systemctl enable --now caddy

echo "==> Firewall"
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

echo "==> Folders and checkouts"
sudo mkdir -p "${BASE_DIR}/data"
sudo chown -R "$USER":"$USER" "${BASE_DIR}"
chmod 750 "${BASE_DIR}/data"
[ -d "${BASE_DIR}/dev/.git" ]  || git clone -b develop "${REPO_URL}" "${BASE_DIR}/dev"
[ -d "${BASE_DIR}/prod/.git" ] || git clone -b main    "${REPO_URL}" "${BASE_DIR}/prod"

echo
echo "Done. Log out and back in so the docker group applies, then check:"
echo "  docker --version && docker compose version && caddy version && free -h && sudo ufw status"
