# DEV Fitness

DEV Fitness is a dark, iOS-inspired Android application for gym members and administrators.
The same APK provides role-based member and admin dashboards backed by an Express, Socket.IO,
and MongoDB Atlas API.

## Features

- Member feed, likes, comments, notices, offers, notifications, chat, and gallery
- Location-restricted attendance check-in/check-out
- Diet plans, subscription packages, monthly dues, PhonePe/UPI renewal, and payment history
- Health dashboard with attendance, calories, age, height, weight, and fitness goals
- Encrypted Aadhaar number storage, protected document uploads, and admin verification
- Admin CRUD for members, posts, diets, subscriptions, payments, content, gallery, and alerts

## Android development

1. Open the project in Android Studio.
2. Copy `secrets.properties.example` to `secrets.properties`.
3. Set `API_BASE_URL` to the backend URL reachable from the Android device.
4. Build with:

```bash
./gradlew :app:assembleDebug
```

Secrets and uploaded media are ignored by Git.

## Backend development

Requires Node.js 18+ and MongoDB Atlas:

```bash
cd backend
npm ci
MONGODB_URI='mongodb+srv://…' JWT_SECRET='a-long-random-secret' npm start
```

The health endpoint is available at `GET /health`.

## Ubuntu/Debian production deployment

The project contains two deployment scripts:

1. `scripts/install-server.sh` installs Git, Node.js 22, build tools, and PM2.
2. `scripts/setup-server.sh` clones or updates this repository, securely requests the Atlas
   connection URI, installs backend dependencies, starts the API with PM2, enables startup after
   reboot, checks health, and prints the public API URL.

Run on a new server:

```bash
curl -fsSLO https://raw.githubusercontent.com/bhardwaaj/devFitness/main/scripts/install-server.sh
chmod +x install-server.sh
./install-server.sh

curl -fsSLO https://raw.githubusercontent.com/bhardwaaj/devFitness/main/scripts/setup-server.sh
chmod +x setup-server.sh
./setup-server.sh
```

For unattended setup:

```bash
MONGODB_URI='mongodb+srv://…' \
APP_DIR="$HOME/devFitness" \
PORT=3000 \
./setup-server.sh
```

MongoDB is hosted by Atlas, so MongoDB should not be installed on the application server. Keep
port `3000` private when using a TLS reverse proxy, or allow it in the server firewall for direct
access. Update the Android `API_BASE_URL` to the printed HTTPS/API address before producing the
release APK.

Useful commands:

```bash
pm2 status
pm2 logs devfitness-api
pm2 restart devfitness-api
```

## AWS EC2 deployment

AWS-specific scripts support Ubuntu 22.04/24.04 and Amazon Linux 2023:

```bash
curl -fsSLO https://raw.githubusercontent.com/bhardwaaj/devFitness/main/scripts/aws-install.sh
chmod +x aws-install.sh
./aws-install.sh

curl -fsSLO https://raw.githubusercontent.com/bhardwaaj/devFitness/main/scripts/aws-run-backend.sh
chmod +x aws-run-backend.sh
./aws-run-backend.sh
```

The installation script installs Node.js 22, Git, compiler tools, and PM2. The run script clones
or updates the repository, creates a protected environment file, starts the backend, enables PM2
after reboot, verifies `/health`, and prints the EC2 API URL.

After deployment:

- Add the EC2 public IP to MongoDB Atlas Network Access.
- Allow inbound TCP `3000` in the EC2 Security Group while testing.
- Use HTTPS through an Application Load Balancer or Nginx before production use.
- Set the Android `API_BASE_URL` to the final HTTPS backend address and rebuild the APK.
