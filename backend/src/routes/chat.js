const express = require("express");
const { db } = require("../db");
const { authenticate } = require("../middleware/auth");
const { asyncRoute, objectId, pageOptions, publicUser } = require("../utils/http");

const router = express.Router();
router.use(authenticate);

router.get(
  "/contacts",
  asyncRoute(async (request, response) => {
    const role = request.user.role === "admin" ? "member" : "admin";
    const users = await db().collection("users")
      .find({ role, status: "active", _id: { $ne: request.user._id } })
      .sort({ name: 1 })
      .toArray();
    response.json(users.map(publicUser));
  }),
);

function participantFilter(request, id) {
  return { _id: objectId(id), participantIds: request.user._id };
}

router.get(
  "/conversations",
  asyncRoute(async (request, response) => {
    const items = await db().collection("conversations").aggregate([
      { $match: { participantIds: request.user._id } },
      { $sort: { updatedAt: -1 } },
      {
        $lookup: {
          from: "messages",
          let: { conversationId: "$_id" },
          pipeline: [
            { $match: { $expr: { $eq: ["$conversationId", "$$conversationId"] } } },
            { $match: { senderId: { $ne: request.user._id }, seenBy: { $ne: request.user._id } } },
            { $count: "count" },
          ],
          as: "unread",
        },
      },
      { $addFields: { unreadCount: { $ifNull: [{ $first: "$unread.count" }, 0] } } },
      { $project: { unread: 0 } },
    ]).toArray();
    response.json(items);
  }),
);

router.post(
  "/conversations",
  asyncRoute(async (request, response) => {
    const participantIds = [...new Set(
      [request.user._id.toString(), ...(request.body.participantIds || [])].map(String),
    )].map(objectId);
    if (participantIds.length < 2) return response.status(400).json({ error: "PARTICIPANTS_REQUIRED" });
    const existing = await db().collection("conversations").findOne({
      participantIds: { $all: participantIds, $size: participantIds.length },
    });
    if (existing) return response.json(existing);
    const conversation = {
      participantIds,
      title: String(request.body.title || ""),
      createdBy: request.user._id,
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    const result = await db().collection("conversations").insertOne(conversation);
    conversation._id = result.insertedId;
    response.status(201).json(conversation);
  }),
);

router.get(
  "/conversations/:id/messages",
  asyncRoute(async (request, response) => {
    const conversation = await db().collection("conversations").findOne(
      participantFilter(request, request.params.id),
    );
    if (!conversation) return response.status(404).json({ error: "CONVERSATION_NOT_FOUND" });
    const { limit, skip, page } = pageOptions(request.query);
    const items = await db().collection("messages").find({ conversationId: conversation._id })
      .sort({ createdAt: -1 }).skip(skip).limit(limit).toArray();
    response.json({ items: items.reverse(), page, limit });
  }),
);

router.post(
  "/conversations/:id/messages",
  asyncRoute(async (request, response) => {
    const conversation = await db().collection("conversations").findOne(
      participantFilter(request, request.params.id),
    );
    if (!conversation) return response.status(404).json({ error: "CONVERSATION_NOT_FOUND" });
    const text = String(request.body.text || "").trim();
    if (!text || text.length > 5000) return response.status(400).json({ error: "INVALID_MESSAGE" });
    const message = {
      conversationId: conversation._id,
      senderId: request.user._id,
      text,
      seenBy: [request.user._id],
      createdAt: new Date(),
    };
    const result = await db().collection("messages").insertOne(message);
    message._id = result.insertedId;
    await db().collection("conversations").updateOne(
      { _id: conversation._id },
      { $set: { lastMessage: text, updatedAt: new Date() } },
    );
    request.app.get("io")?.to(`conversation:${conversation._id}`).emit("chat:message", message);
    for (const userId of conversation.participantIds) {
      if (!userId.equals(request.user._id)) {
        const notification = {
          userId,
          title: request.user.name || "New message",
          body: text,
          type: "chat",
          data: { conversationId: conversation._id.toString() },
          readAt: null,
          createdAt: new Date(),
        };
        await db().collection("notifications").insertOne(notification);
        request.app.get("io")?.to(`user:${userId}`).emit("notification", notification);
      }
    }
    response.status(201).json(message);
  }),
);

router.patch(
  "/conversations/:id/seen",
  asyncRoute(async (request, response) => {
    const conversation = await db().collection("conversations").findOne(
      participantFilter(request, request.params.id),
    );
    if (!conversation) return response.status(404).json({ error: "CONVERSATION_NOT_FOUND" });
    await db().collection("messages").updateMany(
      { conversationId: conversation._id, seenBy: { $ne: request.user._id } },
      { $addToSet: { seenBy: request.user._id } },
    );
    request.app.get("io")?.to(`conversation:${conversation._id}`).emit("chat:seen", {
      conversationId: conversation._id,
      userId: request.user._id,
    });
    response.status(204).end();
  }),
);

module.exports = router;
