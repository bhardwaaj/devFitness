const crypto = require("crypto");

const ITERATIONS = 12000;

function createPassword(password) {
  const salt = crypto.randomBytes(16).toString("hex");
  return { salt, passwordHash: hashPassword(password, salt) };
}

function hashPassword(password, saltHex) {
  return crypto
    .pbkdf2Sync(Buffer.from(password, "utf8"), Buffer.from(saltHex, "hex"), ITERATIONS, 32, "sha256")
    .toString("hex");
}

function verifyPassword(password, user) {
  if (!user?.salt || !user?.passwordHash) return false;
  const calculated = hashPassword(password, user.salt);
  if (calculated.length !== user.passwordHash.length) return false;
  return crypto.timingSafeEqual(
    Buffer.from(calculated, "hex"),
    Buffer.from(user.passwordHash, "hex"),
  );
}

module.exports = { ITERATIONS, createPassword, hashPassword, verifyPassword };
