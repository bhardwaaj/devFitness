const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

function readLegacySecrets() {
  const file = path.resolve(__dirname, "..", "..", "secrets.properties");
  if (!fs.existsSync(file)) return {};
  return Object.fromEntries(
    fs
      .readFileSync(file, "utf8")
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#") && line.includes("="))
      .map((line) => {
        const index = line.indexOf("=");
        return [line.slice(0, index).trim(), line.slice(index + 1).trim()];
      }),
  );
}

const legacy = readLegacySecrets();
const legacyUri =
  legacy.MONGODB_HOST && legacy.MONGODB_USER && legacy.MONGODB_PASSWORD
    ? `mongodb+srv://${encodeURIComponent(legacy.MONGODB_USER)}:${encodeURIComponent(
        legacy.MONGODB_PASSWORD,
      )}@${legacy.MONGODB_HOST}/?retryWrites=true&w=majority&appName=DevFitnessApi`
    : "";

const generatedSecret = crypto.randomBytes(48).toString("hex");
if (!process.env.JWT_SECRET && !legacy.JWT_SECRET) {
  console.warn("JWT_SECRET is not set; using a temporary secret. Tokens will not survive restarts.");
}

module.exports = {
  port: Number(process.env.PORT || 3000),
  mongoUri: process.env.MONGODB_URI || legacyUri,
  mongoDb: process.env.MONGODB_DB || "devfitness",
  jwtSecret: process.env.JWT_SECRET || legacy.JWT_SECRET || generatedSecret,
  jwtIssuer: "devfitness-api",
  gymLocation: { latitude: 26.882172, longitude: 75.904685 },
  attendanceRadiusMeters: 50,
};
