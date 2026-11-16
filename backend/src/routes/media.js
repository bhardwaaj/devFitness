const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const express = require("express");
const { db } = require("../db");
const { authenticate, authorize } = require("../middleware/auth");
const { asyncRoute, objectId, pageOptions } = require("../utils/http");

const router = express.Router();
const uploadDirectory = path.resolve(__dirname, "..", "..", "uploads");
fs.mkdirSync(uploadDirectory, { recursive: true });

const allowedTypes = new Map([
  ["image/jpeg", ".jpg"],
  ["image/png", ".png"],
  ["image/webp", ".webp"],
  ["image/gif", ".gif"],
  ["video/mp4", ".mp4"],
  ["video/webm", ".webm"],
  ["video/quicktime", ".mov"],
]);

router.post(
  "/uploads",
  authenticate,
  express.raw({ type: () => true, limit: "30mb" }),
  asyncRoute(async (request, response) => {
    const mimeType = String(request.headers["content-type"] || "").split(";")[0].trim().toLowerCase();
    const extension = allowedTypes.get(mimeType);
    if (!extension || !Buffer.isBuffer(request.body) || !request.body.length) {
      return response.status(400).json({ error: "INVALID_MEDIA" });
    }
    const filename = `${Date.now()}-${crypto.randomBytes(8).toString("hex")}${extension}`;
    await fs.promises.writeFile(path.join(uploadDirectory, filename), request.body);
    response.status(201).json({
      url: `${request.protocol}://${request.get("host")}/uploads/${filename}`,
      mimeType,
      filename,
    });
  }),
);

router.get(
  "/gallery",
  authenticate,
  asyncRoute(async (request, response) => {
    const { limit, skip, page } = pageOptions(request.query);
    const items = await db().collection("gallery")
      .find({}).sort({ momentDate: -1, createdAt: -1 }).skip(skip).limit(limit).toArray();
    response.json({ items, page, limit });
  }),
);

router.post(
  "/gallery",
  authenticate,
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const url = String(request.body.url || "").trim();
    if (!url) return response.status(400).json({ error: "MEDIA_URL_REQUIRED" });
    const item = {
      url,
      mimeType: String(request.body.mimeType || "image/jpeg"),
      caption: String(request.body.caption || ""),
      momentDate: request.body.momentDate ? new Date(request.body.momentDate) : new Date(),
      createdBy: request.user._id,
      createdAt: new Date(),
    };
    const result = await db().collection("gallery").insertOne(item);
    item._id = result.insertedId;
    response.status(201).json(item);
  }),
);

router.delete(
  "/gallery/:id",
  authenticate,
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const item = await db().collection("gallery").findOneAndDelete({ _id: objectId(request.params.id) });
    if (!item) return response.status(404).json({ error: "GALLERY_ITEM_NOT_FOUND" });
    response.status(204).end();
  }),
);

module.exports = { mediaRoutes: router, uploadDirectory };
