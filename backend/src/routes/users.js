const express = require("express");
const path = require("path");
const { db } = require("../db");
const { authenticate, authorize } = require("../middleware/auth");
const { asyncRoute, objectId, pageOptions, publicUser, adminUser } = require("../utils/http");
const { createPassword } = require("../utils/password");

const router = express.Router();
const verificationDirectory = path.resolve(__dirname, "..", "..", "private-uploads");
router.use(authenticate, authorize("admin"));

router.get(
  "/",
  asyncRoute(async (request, response) => {
    const { limit, skip, page } = pageOptions(request.query);
    const filter = {};
    if (request.query.role) filter.role = request.query.role;
    if (request.query.status) filter.status = request.query.status;
    const [items, total] = await Promise.all([
      db().collection("users").find(filter).sort({ createdAt: -1 }).skip(skip).limit(limit).toArray(),
      db().collection("users").countDocuments(filter),
    ]);
    response.json({ items: items.map(adminUser), total, page, limit });
  }),
);

router.post(
  "/",
  asyncRoute(async (request, response) => {
    const name = String(request.body.name || "").trim();
    const email = String(request.body.email || "").trim().toLowerCase();
    const password = String(request.body.password || "");
    const role = request.body.role === "admin" ? "admin" : "member";
    if (name.length < 2 || !email || password.length < 6) {
      return response.status(400).json({ error: "INVALID_USER" });
    }
    const now = new Date();
    const user = { name, email, role, status: "active", ...createPassword(password), createdAt: now, updatedAt: now };
    const result = await db().collection("users").insertOne(user);
    user._id = result.insertedId;
    response.status(201).json(publicUser(user));
  }),
);

router.get(
  "/:id",
  asyncRoute(async (request, response) => {
    const user = await db().collection("users").findOne({ _id: objectId(request.params.id) });
    if (!user) return response.status(404).json({ error: "USER_NOT_FOUND" });
    response.json(adminUser(user));
  }),
);

router.get(
  "/:id/aadhaar-photo",
  asyncRoute(async (request, response) => {
    const user = await db().collection("users").findOne({ _id: objectId(request.params.id) });
    if (!user?.aadhaarPhotoFile) {
      return response.status(404).json({ error: "AADHAAR_PHOTO_NOT_FOUND" });
    }
    response.set("Cache-Control", "private, no-store");
    response.type(user.aadhaarPhotoMime || "image/jpeg");
    response.sendFile(path.join(verificationDirectory, user.aadhaarPhotoFile));
  }),
);

router.patch(
  "/:id/verification",
  asyncRoute(async (request, response) => {
    const status = String(request.body.status || "").toLowerCase();
    if (!["approved", "rejected"].includes(status)) {
      return response.status(400).json({ error: "INVALID_VERIFICATION_STATUS" });
    }
    const existing = await db().collection("users").findOne({ _id: objectId(request.params.id) });
    if (!existing) return response.status(404).json({ error: "USER_NOT_FOUND" });
    if (!existing.aadhaarEncrypted || !existing.aadhaarPhotoFile) {
      return response.status(400).json({ error: "AADHAAR_SUBMISSION_INCOMPLETE" });
    }
    const user = await db().collection("users").findOneAndUpdate(
      { _id: existing._id },
      {
        $set: {
          verificationStatus: status,
          verificationNote: String(request.body.note || "").trim().slice(0, 500),
          verifiedBy: request.user._id,
          verifiedAt: new Date(),
          updatedAt: new Date(),
        },
      },
      { returnDocument: "after" },
    );
    response.json(adminUser(user));
  }),
);

router.patch(
  "/:id",
  asyncRoute(async (request, response) => {
    const update = {};
    for (const key of ["name", "phone"]) {
      if (request.body[key] !== undefined) update[key] = String(request.body[key]).trim();
    }
    if (["admin", "member"].includes(request.body.role)) update.role = request.body.role;
    if (["active", "inactive"].includes(request.body.status)) update.status = request.body.status;
    update.updatedAt = new Date();
    const user = await db().collection("users").findOneAndUpdate(
      { _id: objectId(request.params.id) },
      { $set: update },
      { returnDocument: "after" },
    );
    if (!user) return response.status(404).json({ error: "USER_NOT_FOUND" });
    response.json(publicUser(user));
  }),
);

router.delete(
  "/:id",
  asyncRoute(async (request, response) => {
    if (request.params.id === request.user._id.toString()) {
      return response.status(400).json({ error: "CANNOT_DEACTIVATE_SELF" });
    }
    const result = await db().collection("users").updateOne(
      { _id: objectId(request.params.id) },
      { $set: { status: "inactive", updatedAt: new Date() } },
    );
    if (!result.matchedCount) return response.status(404).json({ error: "USER_NOT_FOUND" });
    response.status(204).end();
  }),
);

router.post(
  "/:id/reset-password",
  asyncRoute(async (request, response) => {
    const password = String(request.body.password || "");
    if (password.length < 6) return response.status(400).json({ error: "PASSWORD_TOO_SHORT" });
    const result = await db().collection("users").updateOne(
      { _id: objectId(request.params.id) },
      { $set: { ...createPassword(password), updatedAt: new Date() } },
    );
    if (!result.matchedCount) return response.status(404).json({ error: "USER_NOT_FOUND" });
    response.status(204).end();
  }),
);

module.exports = router;
