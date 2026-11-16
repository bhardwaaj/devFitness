#!/usr/bin/env bash
set -Eeuo pipefail

SUDO=""
if [[ "${EUID}" -ne 0 ]]; then
  command -v sudo >/dev/null 2>&1 || {
    echo "sudo is required. Run this script as the EC2 user with sudo access." >&2
    exit 1
  }
  SUDO="sudo"
fi

source /etc/os-release

case "${ID:-}" in
  ubuntu|debian)
    echo "Installing requirements on Ubuntu/Debian…"
    $SUDO apt-get update
    $SUDO apt-get install -y ca-certificates curl git build-essential openssl
    if ! command -v node >/dev/null 2>&1 || (( $(node -p 'Number(process.versions.node.split(".")[0])') < 20 )); then
      curl -fsSL https://deb.nodesource.com/setup_22.x | $SUDO -E bash -
      $SUDO apt-get install -y nodejs
    fi
    ;;
  amzn)
    echo "Installing requirements on Amazon Linux…"
    # Amazon Linux 2023 ships curl-minimal. Installing the full curl package conflicts with it.
    $SUDO dnf install -y ca-certificates curl-minimal git gcc-c++ make openssl
    if ! command -v node >/dev/null 2>&1 || (( $(node -p 'Number(process.versions.node.split(".")[0])') < 20 )); then
      curl -fsSL https://rpm.nodesource.com/setup_22.x | $SUDO -E bash -
      $SUDO dnf install -y nodejs
    fi
    ;;
  rhel|rocky|almalinux|fedora)
    echo "Installing requirements on RHEL-compatible Linux…"
    $SUDO dnf install -y ca-certificates curl git gcc-c++ make openssl
    if ! command -v node >/dev/null 2>&1 || (( $(node -p 'Number(process.versions.node.split(".")[0])') < 20 )); then
      curl -fsSL https://rpm.nodesource.com/setup_22.x | $SUDO -E bash -
      $SUDO dnf install -y nodejs
    fi
    ;;
  *)
    echo "Unsupported EC2 operating system: ${PRETTY_NAME:-unknown}" >&2
    echo "Use Ubuntu 22.04/24.04 or Amazon Linux 2023." >&2
    exit 1
    ;;
esac

echo "Installing PM2 process manager…"
$SUDO npm install --global pm2

echo
echo "AWS server requirements installed successfully."
echo "Node: $(node --version)"
echo "npm:  $(npm --version)"
echo "Git:  $(git --version)"
echo "PM2:  $(pm2 --version)"
echo
echo "MongoDB Atlas is remote; MongoDB is intentionally not installed on this EC2 instance."
echo "Next command: ./scripts/aws-run-backend.sh"
