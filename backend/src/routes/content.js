const express = require("express");
const { db } = require("../db");
const { authenticate, authorize } = require("../middleware/auth");
const { asyncRoute, objectId, pageOptions } = require("../utils/http");

const router = express.Router();
router.use(authenticate);

function contentType(request) {
  return request.baseUrl.slice(1);
}

router.get(
  "/",
  asyncRoute(async (request, response) => {
    const { limit, skip, page } = pageOptions(request.query);
    const filter = { type: contentType(request) };
    if (request.user.role !== "admin") {
      filter.status = "published";
      filter.$and = [
        { $or: [{ audience: "all" }, { audience: request.user.role }, { userIds: request.user._id }] },
        { $or: [{ deadline: null }, { deadline: { $gte: new Date() } }] },
      ];
    }
    const items = await db().collection("content").find(filter)
      .sort({ publishedAt: -1, createdAt: -1 }).skip(skip).limit(limit).toArray();
    response.json({ items, page, limit });
  }),
);

router.post(
  "/",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const title = String(request.body.title || "").trim();
    if (!title) return response.status(400).json({ error: "TITLE_REQUIRED" });
    const item = {
      type: contentType(request),
      title,
      body: String(request.body.body || ""),
      imageUrl: String(request.body.imageUrl || ""),
      audience: request.body.audience || "all",
      userIds: (request.body.userIds || []).map(objectId),
      deadline: request.body.deadline ? new Date(request.body.deadline) : null,
      status: request.body.status === "draft" ? "draft" : "published",
      publishedAt: request.body.status === "draft" ? null : new Date(),
      createdBy: request.user._id,
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    const result = await db().collection("content").insertOne(item);
    item._id = result.insertedId;
    if (item.type === "notices" && item.status === "published") {
      let recipients = item.userIds;
      if (item.audience === "all") {
        recipients = await db().collection("users").distinct("_id", { role: "member", status: "active" });
      } else if (item.audience === "member") {
        recipients = await db().collection("users").distinct("_id", { role: "member", status: "active" });
      }
      const notifications = recipients.map((userId) => ({
        userId,
        title: item.title,
        body: item.body,
        type: "notice",
        data: { noticeId: item._id.toString(), deadline: item.deadline },
        readAt: null,
        createdAt: new Date(),
      }));
      if (notifications.length) {
        await db().collection("notifications").insertMany(notifications);
        for (const notification of notifications) {
          request.app.get("io")?.to(`user:${notification.userId}`).emit("notification", notification);
        }
      }
    }
    response.status(201).json(item);
  }),
);

router.patch(
  "/:id",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const update = { updatedAt: new Date() };
    for (const key of ["title", "body", "imageUrl", "audience", "status"]) {
      if (request.body[key] !== undefined) update[key] = request.body[key];
    }
    if (request.body.userIds) update.userIds = request.body.userIds.map(objectId);
    if (request.body.deadline !== undefined) {
      update.deadline = request.body.deadline ? new Date(request.body.deadline) : null;
    }
    if (request.body.status === "published") update.publishedAt = new Date();
    const item = await db().collection("content").findOneAndUpdate(
      { _id: objectId(request.params.id), type: contentType(request) },
      { $set: update },
      { returnDocument: "after" },
    );
    if (!item) return response.status(404).json({ error: "CONTENT_NOT_FOUND" });
    response.json(item);
  }),
);

router.delete(
  "/:id",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const result = await db().collection("content").deleteOne({
      _id: objectId(request.params.id),
      type: contentType(request),
    });
    if (!result.deletedCount) return response.status(404).json({ error: "CONTENT_NOT_FOUND" });
    response.status(204).end();
  }),
);

module.exports = router;
