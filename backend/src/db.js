const { MongoClient } = require("mongodb");
const config = require("./config");

let client;
let database;

async function connectDatabase() {
  if (!config.mongoUri) {
    throw new Error("Set MONGODB_URI or MongoDB values in secrets.properties");
  }
  client = new MongoClient(config.mongoUri, { serverSelectionTimeoutMS: 12000 });
  await client.connect();
  database = client.db(config.mongoDb);
  await createIndexes(database);
  return database;
}

function db() {
  if (!database) throw new Error("Database has not been connected");
  return database;
}

async function closeDatabase() {
  await client?.close();
}

async function createIndexes(database) {
  await Promise.all([
    database.collection("users").createIndex({ email: 1 }, { unique: true }),
    database.collection("users").createIndex({ role: 1, status: 1 }),
    database.collection("posts").createIndex({ createdAt: -1 }),
    database.collection("comments").createIndex({ postId: 1, createdAt: 1 }),
    database.collection("postLikes").createIndex({ postId: 1, userId: 1 }, { unique: true }),
    database.collection("dietPlans").createIndex({ status: 1, audience: 1 }),
    database.collection("dietAssignments").createIndex({ userId: 1, assignedAt: -1 }),
    database.collection("subscriptionPlans").createIndex({ active: 1, price: 1 }),
    database.collection("memberSubscriptions").createIndex({ userId: 1, startDate: -1 }),
    database.collection("payments").createIndex({ userId: 1, createdAt: -1 }),
    database.collection("payments").createIndex({ status: 1, createdAt: -1 }),
    database.collection("memberDues").createIndex({ userId: 1, dueMonth: 1 }, { unique: true }),
    database.collection("gallery").createIndex({ momentDate: -1 }),
    database.collection("content").createIndex({ type: 1, publishedAt: -1 }),
    database.collection("content").createIndex({ type: 1, deadline: 1 }),
    database.collection("attendance").createIndex({ userId: 1, checkInAt: -1 }),
    database.collection("attendance").createIndex(
      { userId: 1 },
      { unique: true, partialFilterExpression: { checkOutAt: null }, name: "one_open_checkin_per_user" },
    ),
    database.collection("attendance").createIndex({ checkInAt: -1 }),
    database.collection("notifications").createIndex({ userId: 1, createdAt: -1 }),
    database.collection("conversations").createIndex({ participantIds: 1, updatedAt: -1 }),
    database.collection("messages").createIndex({ conversationId: 1, createdAt: 1 }),
  ]);
}

module.exports = { connectDatabase, db, closeDatabase };
