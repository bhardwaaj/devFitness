#!/usr/bin/env bash
set -Eeuo pipefail

if [[ ! -f /etc/debian_version ]]; then
  echo "This installer supports Ubuntu and Debian servers." >&2
  exit 1
fi

SUDO=""
if [[ "${EUID}" -ne 0 ]]; then
  command -v sudo >/dev/null 2>&1 || {
    echo "sudo is required when this script is not run as root." >&2
    exit 1
  }
  SUDO="sudo"
fi

echo "Installing Git, build tools, and Node.js prerequisites…"
$SUDO apt-get update
$SUDO apt-get install -y ca-certificates curl git build-essential openssl

NODE_MAJOR=0
if command -v node >/dev/null 2>&1; then
  NODE_MAJOR="$(node --version | tr -d v | cut -d. -f1)"
fi

if (( NODE_MAJOR < 20 )); then
  echo "Installing Node.js 22 LTS…"
  curl -fsSL https://deb.nodesource.com/setup_22.x | $SUDO -E bash -
  $SUDO apt-get install -y nodejs
fi

echo "Installing PM2…"
$SUDO npm install --global pm2

echo
echo "Server requirements installed:"
echo "  Node: $(node --version)"
echo "  npm:  $(npm --version)"
echo "  Git:  $(git --version)"
echo "  PM2:  $(pm2 --version)"
echo
echo "MongoDB is provided by MongoDB Atlas; a local MongoDB server is not required."
echo "Next, run scripts/setup-server.sh as the Linux user that should own the app."
