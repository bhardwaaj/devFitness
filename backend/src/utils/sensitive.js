const crypto = require("crypto");
const config = require("../config");

const key = crypto.createHash("sha256").update(config.jwtSecret).digest();

function encryptSensitive(value) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);
  const encrypted = Buffer.concat([cipher.update(String(value), "utf8"), cipher.final()]);
  return {
    value: encrypted.toString("base64"),
    iv: iv.toString("base64"),
    tag: cipher.getAuthTag().toString("base64"),
  };
}

function decryptSensitive(payload) {
  if (!payload?.value || !payload?.iv || !payload?.tag) return "";
  try {
    const decipher = crypto.createDecipheriv(
      "aes-256-gcm",
      key,
      Buffer.from(payload.iv, "base64"),
    );
    decipher.setAuthTag(Buffer.from(payload.tag, "base64"));
    return Buffer.concat([
      decipher.update(Buffer.from(payload.value, "base64")),
      decipher.final(),
    ]).toString("utf8");
  } catch (_error) {
    return "";
  }
}

module.exports = { encryptSensitive, decryptSensitive };
