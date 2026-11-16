const express = require("express");
const { db } = require("../db");
const { authenticate, authorize } = require("../middleware/auth");
const { asyncRoute, objectId, pageOptions } = require("../utils/http");

const router = express.Router();
router.use(authenticate);

router.get(
  "/",
  asyncRoute(async (request, response) => {
    const { limit, skip, page } = pageOptions(request.query);
    const items = await db().collection("notifications")
      .find({ userId: request.user._id }).sort({ createdAt: -1 }).skip(skip).limit(limit).toArray();
    response.json({ items, page, limit });
  }),
);

router.patch(
  "/:id/read",
  asyncRoute(async (request, response) => {
    const item = await db().collection("notifications").findOneAndUpdate(
      { _id: objectId(request.params.id), userId: request.user._id },
      { $set: { readAt: new Date() } },
      { returnDocument: "after" },
    );
    if (!item) return response.status(404).json({ error: "NOTIFICATION_NOT_FOUND" });
    response.json(item);
  }),
);

router.post(
  "/",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    let userIds = (request.body.userIds || []).map(objectId);
    if (request.body.audience === "all") {
      userIds = await db().collection("users").distinct("_id", { status: "active" });
    }
    if (!userIds.length) return response.status(400).json({ error: "RECIPIENTS_REQUIRED" });
    const now = new Date();
    const documents = userIds.map((userId) => ({
      userId,
      title: String(request.body.title || ""),
      body: String(request.body.body || ""),
      data: request.body.data || {},
      readAt: null,
      createdAt: now,
    }));
    await db().collection("notifications").insertMany(documents);
    for (const item of documents) request.app.get("io")?.to(`user:${item.userId}`).emit("notification", item);
    response.status(201).json({ created: documents.length });
  }),
);

module.exports = router;
