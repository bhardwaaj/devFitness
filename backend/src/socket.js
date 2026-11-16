const { ObjectId } = require("mongodb");
const { db } = require("./db");
const { verifySocketToken } = require("./middleware/auth");

function configureSocket(io) {
  io.use(async (socket, next) => {
    try {
      const token = socket.handshake.auth?.token ||
        String(socket.handshake.headers.authorization || "").replace(/^Bearer\s+/i, "");
      const payload = verifySocketToken(token);
      const user = await db().collection("users").findOne({ _id: new ObjectId(payload.sub), status: "active" });
      if (!user) return next(new Error("unauthorized"));
      socket.user = user;
      next();
    } catch (_error) {
      next(new Error("unauthorized"));
    }
  });

  io.on("connection", (socket) => {
    socket.join(`user:${socket.user._id}`);

    socket.on("chat:join", async ({ conversationId } = {}, acknowledge = () => {}) => {
      try {
        if (!ObjectId.isValid(conversationId)) return acknowledge({ error: "INVALID_ID" });
        const id = new ObjectId(conversationId);
        const conversation = await db().collection("conversations").findOne({
          _id: id,
          participantIds: socket.user._id,
        });
        if (!conversation) return acknowledge({ error: "CONVERSATION_NOT_FOUND" });
        await socket.join(`conversation:${id}`);
        acknowledge({ ok: true });
      } catch (_error) {
        acknowledge({ error: "SERVER_ERROR" });
      }
    });

    socket.on("chat:send", async ({ conversationId, text } = {}, acknowledge = () => {}) => {
      try {
        const cleanText = String(text || "").trim();
        if (!ObjectId.isValid(conversationId) || !cleanText || cleanText.length > 5000) {
          return acknowledge({ error: "INVALID_MESSAGE" });
        }
        const id = new ObjectId(conversationId);
        const conversation = await db().collection("conversations").findOne({
          _id: id,
          participantIds: socket.user._id,
        });
        if (!conversation) return acknowledge({ error: "CONVERSATION_NOT_FOUND" });
        const message = {
          conversationId: id,
          senderId: socket.user._id,
          text: cleanText,
          seenBy: [socket.user._id],
          createdAt: new Date(),
        };
        const result = await db().collection("messages").insertOne(message);
        message._id = result.insertedId;
        await db().collection("conversations").updateOne(
          { _id: id },
          { $set: { lastMessage: cleanText, updatedAt: new Date() } },
        );
        io.to(`conversation:${id}`).emit("chat:message", message);
        for (const userId of conversation.participantIds) {
          if (!userId.equals(socket.user._id)) {
            const notification = {
              userId,
              title: socket.user.name || "New message",
              body: cleanText,
              type: "chat",
              data: { conversationId: id.toString() },
              readAt: null,
              createdAt: new Date(),
            };
            await db().collection("notifications").insertOne(notification);
            io.to(`user:${userId}`).emit("notification", notification);
          }
        }
        acknowledge({ ok: true, message });
      } catch (_error) {
        acknowledge({ error: "SERVER_ERROR" });
      }
    });
  });
}

module.exports = { configureSocket };
