#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="${REPO_URL:-https://github.com/bhardwaaj/devFitness.git}"
BRANCH="${BRANCH:-main}"
APP_DIR="${APP_DIR:-$HOME/devFitness}"
ENV_DIR="${ENV_DIR:-$HOME/.config/devfitness}"
ENV_FILE="$ENV_DIR/backend.env"
PORT="${PORT:-3000}"

for command in git node npm pm2 curl openssl; do
  command -v "$command" >/dev/null 2>&1 || {
    echo "$command is missing. Run aws-install.sh first." >&2
    exit 1
  }
done

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

if [[ ! -f "$ENV_FILE" ]]; then
  MONGODB_URI="${MONGODB_URI:-}"
  if [[ -z "$MONGODB_URI" ]]; then
    read -r -s -p "MongoDB Atlas URI: " MONGODB_URI
    echo
  fi
  [[ "$MONGODB_URI" == mongodb://* || "$MONGODB_URI" == mongodb+srv://* ]] || {
    echo "A valid MongoDB Atlas URI is required." >&2
    exit 1
  }

  JWT_SECRET="${JWT_SECRET:-$(openssl rand -hex 48)}"
  MONGODB_DB="${MONGODB_DB:-devfitness}"
  CORS_ORIGIN="${CORS_ORIGIN:-}"

  umask 077
  {
    printf 'MONGODB_URI=%q\n' "$MONGODB_URI"
    printf 'MONGODB_DB=%q\n' "$MONGODB_DB"
    printf 'JWT_SECRET=%q\n' "$JWT_SECRET"
    printf 'PORT=%q\n' "$PORT"
    printf 'CORS_ORIGIN=%q\n' "$CORS_ORIGIN"
  } > "$ENV_FILE"
  chmod 600 "$ENV_FILE"
else
  echo "Using existing environment file: $ENV_FILE"
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

echo "Installing locked production dependencies…"
cd "$APP_DIR/backend"
npm ci --omit=dev

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
echo "2. Allow this EC2 public IP (${PUBLIC_IP}) in MongoDB Atlas Network Access."
echo "3. For production, place HTTPS (Nginx/ALB + certificate) in front of port ${PORT}."
