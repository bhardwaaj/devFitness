const express = require("express");
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const jwt = require("jsonwebtoken");
const config = require("../config");
const { db } = require("../db");
const { authenticate } = require("../middleware/auth");
const { asyncRoute, publicUser, privateUser } = require("../utils/http");
const { createPassword, verifyPassword } = require("../utils/password");
const { encryptSensitive } = require("../utils/sensitive");

const router = express.Router();
const verificationDirectory = path.resolve(__dirname, "..", "..", "private-uploads");
fs.mkdirSync(verificationDirectory, { recursive: true });

function tokenFor(user) {
  return jwt.sign(
    { sub: user._id.toString(), email: user.email, role: user.role || "member" },
    config.jwtSecret,
    { expiresIn: "7d", issuer: config.jwtIssuer },
  );
}

router.post(
  "/login",
  asyncRoute(async (request, response) => {
    const email = String(request.body?.email || "").trim().toLowerCase();
    const password = String(request.body?.password || "");
    if (!email || !password) return response.status(400).json({ error: "EMAIL_AND_PASSWORD_REQUIRED" });
    const user = await db().collection("users").findOne({ email });
    if (!user || !verifyPassword(password, user)) {
      return response.status(401).json({ error: "INVALID_CREDENTIALS" });
    }
    if (user.status === "inactive") return response.status(403).json({ error: "ACCOUNT_INACTIVE" });
    response.json({ token: tokenFor(user), user: publicUser(user) });
  }),
);

router.post(
  "/register",
  asyncRoute(async (request, response) => {
    const name = String(request.body?.name || "").trim();
    const email = String(request.body?.email || "").trim().toLowerCase();
    const password = String(request.body?.password || "");
    if (name.length < 2 || !email || password.length < 6) {
      return response.status(400).json({ error: "INVALID_REGISTRATION" });
    }
    const now = new Date();
    const document = {
      name,
      email,
      role: "member",
      status: "active",
      ...createPassword(password),
      createdAt: now,
      updatedAt: now,
    };
    const result = await db().collection("users").insertOne(document);
    document._id = result.insertedId;
    response.status(201).json({ token: tokenFor(document), user: publicUser(document) });
  }),
);

router.get("/me", authenticate, (request, response) => response.json(privateUser(request.user)));

router.get(
  "/me/aadhaar-photo",
  authenticate,
  asyncRoute(async (request, response) => {
    if (!request.user.aadhaarPhotoFile) {
      return response.status(404).json({ error: "AADHAAR_PHOTO_NOT_FOUND" });
    }
    response.set("Cache-Control", "private, no-store");
    response.type(request.user.aadhaarPhotoMime || "image/jpeg");
    response.sendFile(path.join(verificationDirectory, request.user.aadhaarPhotoFile));
  }),
);

router.post(
  "/me/aadhaar-photo",
  authenticate,
  express.raw({ type: ["image/jpeg", "image/png", "image/webp"], limit: "10mb" }),
  asyncRoute(async (request, response) => {
    const extensions = { "image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp" };
    const mimeType = String(request.headers["content-type"] || "").split(";")[0];
    const extension = extensions[mimeType];
    if (!extension || !Buffer.isBuffer(request.body) || request.body.length === 0) {
      return response.status(400).json({ error: "INVALID_AADHAAR_PHOTO" });
    }
    const filename = `${request.user._id}-${Date.now()}-${crypto.randomBytes(6).toString("hex")}${extension}`;
    await fs.promises.writeFile(path.join(verificationDirectory, filename), request.body);
    if (request.user.aadhaarPhotoFile) {
      fs.promises.unlink(path.join(verificationDirectory, request.user.aadhaarPhotoFile)).catch(() => {});
    }
    const verificationStatus = request.user.aadhaarEncrypted ? "pending" : "unverified";
    const user = await db().collection("users").findOneAndUpdate(
      { _id: request.user._id },
      {
        $set: {
          aadhaarPhotoFile: filename,
          aadhaarPhotoMime: mimeType,
          verificationStatus,
          verificationNote: "",
          updatedAt: new Date(),
        },
      },
      { returnDocument: "after" },
    );
    response.status(201).json(privateUser(user));
  }),
);

router.patch(
  "/me",
  authenticate,
  asyncRoute(async (request, response) => {
    const update = { updatedAt: new Date() };
    if (request.body.name !== undefined) {
      const name = String(request.body.name).trim();
      if (name.length < 2) return response.status(400).json({ error: "INVALID_NAME" });
      update.name = name;
    }
    if (request.body.phone !== undefined) update.phone = String(request.body.phone).trim();
    const numericFields = {
      age: [12, 100],
      heightCm: [80, 250],
      weightKg: [20, 400],
      dailyCalories: [500, 10000],
    };
    for (const [field, [minimum, maximum]] of Object.entries(numericFields)) {
      if (request.body[field] !== undefined) {
        const value = Number(request.body[field]);
        if (!Number.isFinite(value) || value < minimum || value > maximum) {
          return response.status(400).json({ error: `INVALID_${field.toUpperCase()}` });
        }
        update[field] = value;
      }
    }
    if (request.body.fitnessGoal !== undefined) {
      update.fitnessGoal = String(request.body.fitnessGoal).trim().slice(0, 120);
    }
    if (request.body.aadhaarNumber !== undefined) {
      const aadhaarNumber = String(request.body.aadhaarNumber).replace(/\s/g, "");
      if (!/^\d{12}$/.test(aadhaarNumber)) {
        return response.status(400).json({ error: "AADHAAR_MUST_BE_12_DIGITS" });
      }
      if (request.body.aadhaarConsent !== true) {
        return response.status(400).json({ error: "AADHAAR_CONSENT_REQUIRED" });
      }
      update.aadhaarEncrypted = encryptSensitive(aadhaarNumber);
      update.aadhaarLast4 = aadhaarNumber.slice(-4);
      update.verificationStatus = request.user.aadhaarPhotoFile ? "pending" : "unverified";
      update.verificationNote = "";
      update.aadhaarSubmittedAt = new Date();
    }
    const user = await db().collection("users").findOneAndUpdate(
      { _id: request.user._id },
      { $set: update },
      { returnDocument: "after" },
    );
    response.json(privateUser(user));
  }),
);

router.patch(
  "/me/password",
  authenticate,
  asyncRoute(async (request, response) => {
    const currentPassword = String(request.body.currentPassword || "");
    const newPassword = String(request.body.newPassword || "");
    if (!verifyPassword(currentPassword, request.user)) {
      return response.status(400).json({ error: "CURRENT_PASSWORD_INCORRECT" });
    }
    if (newPassword.length < 6) return response.status(400).json({ error: "PASSWORD_TOO_SHORT" });
    await db().collection("users").updateOne(
      { _id: request.user._id },
      { $set: { ...createPassword(newPassword), updatedAt: new Date() } },
    );
    response.status(204).end();
  }),
);

module.exports = router;
