const http = require("http");
const express = require("express");
const { Server } = require("socket.io");
const config = require("./config");
const { connectDatabase, closeDatabase } = require("./db");
const { errorHandler } = require("./utils/http");
const { configureSocket } = require("./socket");
const authRoutes = require("./routes/auth");
const userRoutes = require("./routes/users");
const postRoutes = require("./routes/posts");
const planRoutes = require("./routes/plans");
const paymentRoutes = require("./routes/payments");
const contentRoutes = require("./routes/content");
const attendanceRoutes = require("./routes/attendance");
const notificationRoutes = require("./routes/notifications");
const chatRoutes = require("./routes/chat");
const { mediaRoutes, uploadDirectory } = require("./routes/media");

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "256kb" }));
app.use((request, _response, next) => {
  request.body ||= {};
  next();
});

app.get("/health", (request, response) => {
  response.json({
    ok: true,
    service: "devfitness-api",
    realtimeClients: request.app.get("io")?.engine?.clientsCount || 0,
  });
});
app.use("/uploads", express.static(uploadDirectory, { maxAge: "7d", immutable: true }));

app.use("/auth", authRoutes);
app.use("/users", userRoutes);
app.use("/posts", postRoutes);
app.use(planRoutes);
app.use(paymentRoutes);
for (const path of ["/blogs", "/notices", "/offers"]) app.use(path, contentRoutes);
app.use("/attendance", attendanceRoutes);
app.use("/notifications", notificationRoutes);
app.use("/chat", chatRoutes);
app.use(mediaRoutes);
app.use((_request, response) => response.status(404).json({ error: "NOT_FOUND" }));
app.use(errorHandler);

const server = http.createServer(app);
const io = new Server(server, {
  cors: process.env.CORS_ORIGIN ? { origin: process.env.CORS_ORIGIN.split(",") } : undefined,
});
app.set("io", io);
configureSocket(io);

connectDatabase()
  .then(() => {
    server.listen(config.port, "0.0.0.0", () => {
      console.log(`DEV Fitness API listening on http://0.0.0.0:${config.port}`);
    });
  })
  .catch((error) => {
    console.error("Could not connect to Atlas:", error.message);
    process.exit(1);
  });

process.on("SIGTERM", async () => {
  await closeDatabase();
  process.exit(0);
});
