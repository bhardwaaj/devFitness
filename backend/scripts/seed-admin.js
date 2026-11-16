// Creates or updates the admin account in Atlas.
// Usage: ADMIN_EMAIL=... ADMIN_PASSWORD=... [ADMIN_NAME=...] npm run seed:admin
// Atlas credentials are read from ../secrets.properties (MONGODB_HOST, MONGODB_USER, MONGODB_PASSWORD).
// Password hashing must stay identical to AuthStore.kt: PBKDF2-HMAC-SHA256, 12000 iterations, 32-byte key, hex salt.

const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const { MongoClient } = require("mongodb");

const ITERATIONS = 12000;

function readSecrets() {
  const file = path.resolve(__dirname, "..", "..", "secrets.properties");
  const values = {};
  for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const index = trimmed.indexOf("=");
    if (index > 0) values[trimmed.slice(0, index).trim()] = trimmed.slice(index + 1).trim();
  }
  return values;
}

function hashPassword(password, saltHex) {
  return crypto
    .pbkdf2Sync(Buffer.from(password, "utf8"), Buffer.from(saltHex, "hex"), ITERATIONS, 32, "sha256")
    .toString("hex");
}

async function main() {
  const email = (process.env.ADMIN_EMAIL || "").trim().toLowerCase();
  const password = process.env.ADMIN_PASSWORD || "";
  const name = (process.env.ADMIN_NAME || "Admin").trim();
  if (!email || !password) {
    throw new Error("Set ADMIN_EMAIL and ADMIN_PASSWORD.");
  }

  const secrets = readSecrets();
  const uri =
    `mongodb+srv://${encodeURIComponent(secrets.MONGODB_USER)}:${encodeURIComponent(secrets.MONGODB_PASSWORD)}` +
    `@${secrets.MONGODB_HOST}/?retryWrites=true&w=majority&appName=DevFitnessSeed`;

  const client = new MongoClient(uri, { serverSelectionTimeoutMS: 15000 });
  await client.connect();
  try {
    const users = client.db("devfitness").collection("users");
    await users.createIndex({ email: 1 }, { unique: true });

    const salt = crypto.randomBytes(16).toString("hex");
    const now = new Date();
    const result = await users.updateOne(
      { email },
      {
        $set: {
          email,
          name,
          role: "admin",
          status: "active",
          salt,
          passwordHash: hashPassword(password, salt),
          updatedAt: now,
        },
        $setOnInsert: { createdAt: now },
      },
      { upsert: true },
    );
    console.log(result.upsertedCount ? `Created admin ${email}` : `Updated admin ${email}`);
  } finally {
    await client.close();
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
