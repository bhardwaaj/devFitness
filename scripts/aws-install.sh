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
    $SUDO apt-get install -y ca-certificates curl git build-essential openssl gnupg
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

echo "Installing MongoDB Community Server 8.0…"
case "${ID:-}" in
  amzn)
    cat <<'EOF' | $SUDO tee /etc/yum.repos.d/mongodb-org-8.0.repo >/dev/null
[mongodb-org-8.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/amazon/2023/mongodb-org/8.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://pgp.mongodb.com/server-8.0.asc
EOF
    $SUDO dnf install -y mongodb-org
    ;;
  ubuntu|debian)
    curl -fsSL https://www.mongodb.org/static/pgp/server-8.0.asc |
      $SUDO gpg --dearmor --yes -o /usr/share/keyrings/mongodb-server-8.0.gpg
    echo "deb [arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-8.0.gpg] https://repo.mongodb.org/apt/ubuntu ${VERSION_CODENAME}/mongodb-org/8.0 multiverse" |
      $SUDO tee /etc/apt/sources.list.d/mongodb-org-8.0.list >/dev/null
    $SUDO apt-get update
    $SUDO apt-get install -y mongodb-org
    ;;
  rhel|rocky|almalinux|fedora)
    cat <<'EOF' | $SUDO tee /etc/yum.repos.d/mongodb-org-8.0.repo >/dev/null
[mongodb-org-8.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/9/mongodb-org/8.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://pgp.mongodb.com/server-8.0.asc
EOF
    $SUDO dnf install -y mongodb-org
    ;;
esac
$SUDO systemctl enable --now mongod

echo "Installing PM2 process manager…"
$SUDO env NPM_CONFIG_USERCONFIG=/dev/null NPM_CONFIG_REGISTRY=https://registry.npmjs.org \
  npm install --global pm2

echo
echo "AWS server requirements installed successfully."
echo "Node: $(node --version)"
echo "npm:  $(npm --version)"
echo "Git:  $(git --version)"
echo "PM2:  $(pm2 --version)"
echo "MongoDB: $(mongod --version | head -n 1)"
echo
echo "MongoDB is running locally and will be secured during backend setup."
echo "Next command: ./scripts/aws-run-backend.sh"
