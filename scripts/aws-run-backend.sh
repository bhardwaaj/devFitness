#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="${REPO_URL:-https://github.com/bhardwaaj/devFitness.git}"
BRANCH="${BRANCH:-main}"
APP_DIR="${APP_DIR:-$HOME/devFitness}"
ENV_DIR="${ENV_DIR:-$HOME/.config/devfitness}"
ENV_FILE="$ENV_DIR/backend.env"
PORT="${PORT:-3000}"
SEED_MARKER="$ENV_DIR/admin.seeded"

for command in git node npm pm2 curl openssl mongosh; do
  command -v "$command" >/dev/null 2>&1 || {
    echo "$command is missing. Run aws-install.sh first." >&2
    exit 1
  }
done

SUDO=""
if [[ "${EUID}" -ne 0 ]]; then
  command -v sudo >/dev/null 2>&1 || {
    echo "sudo is required to secure and restart MongoDB." >&2
    exit 1
  }
  SUDO="sudo"
fi

if [[ -d "$APP_DIR/.git" ]]; then
  echo "Updating DEV Fitness from GitHub…"
  git -C "$APP_DIR" fetch origin "$BRANCH"
  git -C "$APP_DIR" checkout "$BRANCH"
  git -C "$APP_DIR" pull --ff-only origin "$BRANCH"
else
  echo "Cloning DEV Fitness from GitHub…"
  git clone --branch "$BRANCH" "$REPO_URL" "$APP_DIR"
fi

mkdir -p "$ENV_DIR"
chmod 700 "$ENV_DIR"

if [[ -f "$ENV_FILE" ]] && grep -q '^DATABASE_MODE=local$' "$ENV_FILE"; then
  echo "Using existing protected local MongoDB configuration: $ENV_FILE"
else
  EXISTING_JWT_SECRET=""
  if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
    EXISTING_JWT_SECRET="${JWT_SECRET:-}"
  fi

  MONGO_APP_USER="${MONGO_APP_USER:-devfitness_app}"
  MONGO_APP_PASSWORD="${MONGO_APP_PASSWORD:-$(openssl rand -hex 24)}"
  MONGODB_DB="${MONGODB_DB:-devfitness}"
  JWT_SECRET="${EXISTING_JWT_SECRET:-${JWT_SECRET:-$(openssl rand -hex 48)}}"
  CORS_ORIGIN="${CORS_ORIGIN:-}"

  echo "Creating the local MongoDB application user…"
  $SUDO systemctl enable --now mongod
  if $SUDO grep -Eq '^[[:space:]]*authorization:[[:space:]]*enabled' /etc/mongod.conf; then
    echo "MongoDB authentication is already enabled, but no local app configuration exists." >&2
    echo "Remove $ENV_FILE only after disabling authorization or provide a valid local configuration." >&2
    exit 1
  fi

  MONGO_APP_USER="$MONGO_APP_USER" \
  MONGO_APP_PASSWORD="$MONGO_APP_PASSWORD" \
  MONGODB_DB="$MONGODB_DB" \
    mongosh --quiet --eval '
      const target = db.getSiblingDB(process.env.MONGODB_DB);
      const user = process.env.MONGO_APP_USER;
      const options = { pwd: process.env.MONGO_APP_PASSWORD, roles: [{ role: "readWrite", db: process.env.MONGODB_DB }] };
      if (target.getUser(user)) target.updateUser(user, options);
      else target.createUser({ user, ...options });
    '

  ENCODED_USER="$(node -p 'encodeURIComponent(process.argv[1])' "$MONGO_APP_USER")"
  ENCODED_PASSWORD="$(node -p 'encodeURIComponent(process.argv[1])' "$MONGO_APP_PASSWORD")"
  MONGODB_URI="mongodb://${ENCODED_USER}:${ENCODED_PASSWORD}@127.0.0.1:27017/${MONGODB_DB}?authSource=${MONGODB_DB}"

  umask 077
  {
    printf 'DATABASE_MODE=local\n'
    printf 'MONGODB_URI=%q\n' "$MONGODB_URI"
    printf 'MONGODB_DB=%q\n' "$MONGODB_DB"
    printf 'JWT_SECRET=%q\n' "$JWT_SECRET"
    printf 'PORT=%q\n' "$PORT"
    printf 'CORS_ORIGIN=%q\n' "$CORS_ORIGIN"
  } > "$ENV_FILE"
  chmod 600 "$ENV_FILE"

  if ! $SUDO grep -Eq '^[[:space:]]*authorization:[[:space:]]*enabled' /etc/mongod.conf; then
    printf '\nsecurity:\n  authorization: enabled\n' | $SUDO tee -a /etc/mongod.conf >/dev/null
  fi
  $SUDO systemctl restart mongod
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

mongosh "$MONGODB_URI" --quiet --eval 'db.runCommand({ ping: 1 })' >/dev/null

echo "Installing locked production dependencies…"
cd "$APP_DIR/backend"
NPM_CONFIG_USERCONFIG=/dev/null NPM_CONFIG_REGISTRY=https://registry.npmjs.org \
  npm ci --omit=dev

if [[ ! -f "$SEED_MARKER" ]]; then
  ADMIN_EMAIL="${ADMIN_EMAIL:-abhay.singh191103@gmail.com}"
  ADMIN_NAME="${ADMIN_NAME:-Abhay Singh}"
  ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
  if [[ -z "$ADMIN_PASSWORD" ]]; then
    read -r -s -p "Password for admin ${ADMIN_EMAIL}: " ADMIN_PASSWORD
    echo
  fi
  if (( ${#ADMIN_PASSWORD} < 8 )); then
    echo "Admin password must contain at least 8 characters." >&2
    exit 1
  fi
  echo "Seeding the DEV Fitness admin account…"
  ADMIN_EMAIL="$ADMIN_EMAIL" ADMIN_NAME="$ADMIN_NAME" ADMIN_PASSWORD="$ADMIN_PASSWORD" \
    npm run seed:admin
  touch "$SEED_MARKER"
  chmod 600 "$SEED_MARKER"
fi

echo "Starting backend with PM2…"
pm2 startOrReload ecosystem.config.cjs --update-env
pm2 save

if command -v systemctl >/dev/null 2>&1 && command -v sudo >/dev/null 2>&1; then
  sudo env "PATH=$PATH" "$(command -v pm2)" startup systemd -u "$USER" --hp "$HOME" >/dev/null
  pm2 save
fi

echo "Waiting for backend health check…"
for _ in {1..30}; do
  if curl -fsS "http://127.0.0.1:${PORT}/health" >/dev/null; then
    HEALTHY=true
    break
  fi
  sleep 1
done

if [[ "${HEALTHY:-false}" != true ]]; then
  echo "Backend failed its health check." >&2
  pm2 logs devfitness-api --lines 80 --nostream
  exit 1
fi

IMDS_TOKEN="$(curl -fsS --max-time 2 -X PUT \
  -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' \
  http://169.254.169.254/latest/api/token || true)"
PUBLIC_IP=""
if [[ -n "$IMDS_TOKEN" ]]; then
  PUBLIC_IP="$(curl -fsS --max-time 2 \
    -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" \
    http://169.254.169.254/latest/meta-data/public-ipv4 || true)"
fi
PUBLIC_IP="${PUBLIC_IP:-$(curl -4fsS --max-time 5 https://api.ipify.org || true)}"
PUBLIC_IP="${PUBLIC_IP:-YOUR_EC2_PUBLIC_IP}"

echo
echo "DEV Fitness backend is running."
echo "Local health:  http://127.0.0.1:${PORT}/health"
echo "Public API:    http://${PUBLIC_IP}:${PORT}"
echo "PM2 status:    pm2 status"
echo "Backend logs:  pm2 logs devfitness-api"
echo
echo "AWS actions still required:"
echo "1. Allow inbound TCP ${PORT} in the EC2 Security Group, preferably only from trusted sources."
echo "2. Do not expose MongoDB port 27017; MongoDB is bound for local backend use."
echo "3. For production, place HTTPS (Nginx/ALB + certificate) in front of port ${PORT}."
