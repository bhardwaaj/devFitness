#!/usr/bin/env bash
set -Eeuo pipefail

REPO_URL="${REPO_URL:-https://github.com/bhardwaaj/devFitness.git}"
APP_DIR="${APP_DIR:-$HOME/devFitness}"
BRANCH="${BRANCH:-main}"
PORT="${PORT:-3000}"

for command in git node npm pm2; do
  command -v "$command" >/dev/null 2>&1 || {
    echo "$command is missing. Run scripts/install-server.sh first." >&2
    exit 1
  }
done

if [[ -d "$APP_DIR/.git" ]]; then
  echo "Updating DEV Fitness in $APP_DIR…"
  git -C "$APP_DIR" fetch origin "$BRANCH"
  git -C "$APP_DIR" checkout "$BRANCH"
  git -C "$APP_DIR" pull --ff-only origin "$BRANCH"
else
  echo "Cloning DEV Fitness into $APP_DIR…"
  git clone --branch "$BRANCH" "$REPO_URL" "$APP_DIR"
fi

ENV_FILE="$APP_DIR/backend/.env"
if [[ -f "$ENV_FILE" ]]; then
  echo "Using existing protected environment file: $ENV_FILE"
else
  MONGODB_URI="${MONGODB_URI:-}"
  if [[ -z "$MONGODB_URI" ]]; then
    read -r -s -p "MongoDB Atlas connection URI: " MONGODB_URI
    echo
  fi
  [[ "$MONGODB_URI" == mongodb://* || "$MONGODB_URI" == mongodb+srv://* ]] || {
    echo "A valid MongoDB URI is required." >&2
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
fi

echo "Installing production dependencies…"
cd "$APP_DIR/backend"
npm ci --omit=dev

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

echo "Starting DEV Fitness API with PM2…"
pm2 startOrReload ecosystem.config.cjs --update-env
pm2 save

if command -v systemctl >/dev/null 2>&1 && command -v sudo >/dev/null 2>&1; then
  sudo env "PATH=$PATH:/usr/bin" pm2 startup systemd -u "$USER" --hp "$HOME" >/dev/null
  pm2 save
fi

for _ in {1..20}; do
  if curl -fsS "http://127.0.0.1:${PORT}/health" >/dev/null; then
    break
  fi
  sleep 1
done

curl -fsS "http://127.0.0.1:${PORT}/health" >/dev/null || {
  echo "API did not become healthy. Inspect it with: pm2 logs devfitness-api" >&2
  exit 1
}

PUBLIC_IP="$(curl -4fsS --max-time 5 https://api.ipify.org || hostname -I | awk '{print $1}')"
echo
echo "DEV Fitness deployment completed."
echo "Backend API: http://${PUBLIC_IP}:${PORT}"
echo "Health check: http://${PUBLIC_IP}:${PORT}/health"
echo "PM2 status: pm2 status"
echo "PM2 logs:   pm2 logs devfitness-api"
echo
echo "Allow TCP port ${PORT} in the server/cloud firewall, or place a TLS reverse proxy in front of it."
