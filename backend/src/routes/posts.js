const express = require("express");
const { db } = require("../db");
const { authenticate } = require("../middleware/auth");
const { asyncRoute, objectId, pageOptions } = require("../utils/http");

const router = express.Router();
router.use(authenticate);

router.get(
  "/",
  asyncRoute(async (request, response) => {
    const { limit, skip, page } = pageOptions(request.query);
    const items = await db().collection("posts").aggregate([
      { $sort: { createdAt: -1 } },
      { $skip: skip },
      { $limit: limit },
      { $lookup: { from: "postLikes", localField: "_id", foreignField: "postId", as: "likes" } },
      { $lookup: { from: "comments", localField: "_id", foreignField: "postId", as: "comments" } },
      { $addFields: { likeCount: { $size: "$likes" }, commentCount: { $size: "$comments" } } },
      { $project: { likes: 0, comments: 0 } },
    ]).toArray();
    response.json({ items, page, limit });
  }),
);

router.post(
  "/",
  asyncRoute(async (request, response) => {
    const text = String(request.body.text || "").trim();
    if (!text || text.length > 5000) return response.status(400).json({ error: "INVALID_POST" });
    const post = {
      authorId: request.user._id,
      authorName: request.user.name || "",
      text,
      mediaUrl: String(request.body.mediaUrl || "").trim() || null,
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    const result = await db().collection("posts").insertOne(post);
    post._id = result.insertedId;
    request.app.get("io")?.emit("feed:post", post);
    response.status(201).json(post);
  }),
);

router.patch(
  "/:id",
  asyncRoute(async (request, response) => {
    const id = objectId(request.params.id);
    const post = await db().collection("posts").findOne({ _id: id });
    if (!post) return response.status(404).json({ error: "POST_NOT_FOUND" });
    if (request.user.role !== "admin" && !post.authorId.equals(request.user._id)) {
      return response.status(403).json({ error: "FORBIDDEN" });
    }
    const text = String(request.body.text || "").trim();
    if (!text) return response.status(400).json({ error: "INVALID_POST" });
    const updated = await db().collection("posts").findOneAndUpdate(
      { _id: id },
      { $set: { text, mediaUrl: request.body.mediaUrl || null, updatedAt: new Date() } },
      { returnDocument: "after" },
    );
    request.app.get("io")?.emit("feed:update", updated);
    response.json(updated);
  }),
);

router.delete(
  "/:id",
  asyncRoute(async (request, response) => {
    const id = objectId(request.params.id);
    const post = await db().collection("posts").findOne({ _id: id });
    if (!post) return response.status(404).json({ error: "POST_NOT_FOUND" });
    if (request.user.role !== "admin" && !post.authorId.equals(request.user._id)) {
      return response.status(403).json({ error: "FORBIDDEN" });
    }
    await Promise.all([
      db().collection("posts").deleteOne({ _id: id }),
      db().collection("postLikes").deleteMany({ postId: id }),
      db().collection("comments").deleteMany({ postId: id }),
    ]);
    request.app.get("io")?.emit("feed:delete", { id: id.toString() });
    response.status(204).end();
  }),
);

router.post(
  "/:id/likes",
  asyncRoute(async (request, response) => {
    const postId = objectId(request.params.id);
    await db().collection("postLikes").updateOne(
      { postId, userId: request.user._id },
      { $setOnInsert: { postId, userId: request.user._id, createdAt: new Date() } },
      { upsert: true },
    );
    response.status(201).json({ liked: true });
  }),
);

router.delete(
  "/:id/likes",
  asyncRoute(async (request, response) => {
    await db().collection("postLikes").deleteOne({
      postId: objectId(request.params.id),
      userId: request.user._id,
    });
    response.status(204).end();
  }),
);

router.get(
  "/:id/comments",
  asyncRoute(async (request, response) => {
    const items = await db().collection("comments")
      .find({ postId: objectId(request.params.id) }).sort({ createdAt: 1 }).toArray();
    response.json(items);
  }),
);

router.post(
  "/:id/comments",
  asyncRoute(async (request, response) => {
    const text = String(request.body.text || "").trim();
    if (!text || text.length > 2000) return response.status(400).json({ error: "INVALID_COMMENT" });
    const comment = {
      postId: objectId(request.params.id),
      authorId: request.user._id,
      authorName: request.user.name || "",
      text,
      createdAt: new Date(),
    };
    const result = await db().collection("comments").insertOne(comment);
    comment._id = result.insertedId;
    request.app.get("io")?.emit("feed:comment", comment);
    response.status(201).json(comment);
  }),
);

router.delete(
  "/:postId/comments/:commentId",
  asyncRoute(async (request, response) => {
    const comment = await db().collection("comments").findOne({ _id: objectId(request.params.commentId) });
    if (!comment) return response.status(404).json({ error: "COMMENT_NOT_FOUND" });
    if (request.user.role !== "admin" && !comment.authorId.equals(request.user._id)) {
      return response.status(403).json({ error: "FORBIDDEN" });
    }
    await db().collection("comments").deleteOne({ _id: comment._id });
    response.status(204).end();
  }),
);

module.exports = router;
