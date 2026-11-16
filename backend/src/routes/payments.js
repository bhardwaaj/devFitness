const express = require("express");
const { db } = require("../db");
const { authenticate, authorize } = require("../middleware/auth");
const { asyncRoute, objectId, pageOptions } = require("../utils/http");

const router = express.Router();
router.use(authenticate);

router.get(
  "/dues/me",
  asyncRoute(async (request, response) => {
    response.json(await db().collection("memberDues")
      .find({ userId: request.user._id }).sort({ dueMonth: -1 }).toArray());
  }),
);

router.get(
  "/dues",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const filter = request.query.userId ? { userId: objectId(request.query.userId) } : {};
    response.json(await db().collection("memberDues").find(filter).sort({ dueMonth: -1 }).toArray());
  }),
);

router.post(
  "/dues",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const amount = Number(request.body.amount);
    const dueMonth = String(request.body.dueMonth || "").trim();
    if (!(amount > 0) || !/^\d{4}-\d{2}$/.test(dueMonth)) {
      return response.status(400).json({ error: "INVALID_DUE" });
    }
    let userIds = (request.body.userIds || []).map(objectId);
    if (request.body.audience === "all") {
      userIds = await db().collection("users").distinct("_id", { role: "member", status: "active" });
    }
    if (!userIds.length) return response.status(400).json({ error: "RECIPIENTS_REQUIRED" });
    const now = new Date();
    const dueDate = new Date(request.body.dueDate || `${dueMonth}-10T23:59:59.000Z`);
    const operations = userIds.map((userId) => ({
      updateOne: {
        filter: { userId, dueMonth },
        update: {
          $set: {
            amount,
            title: String(request.body.title || "Monthly gym fee"),
            dueDate,
            status: "unpaid",
            updatedAt: now,
            assignedBy: request.user._id,
          },
          $setOnInsert: { userId, dueMonth, createdAt: now },
        },
        upsert: true,
      },
    }));
    await db().collection("memberDues").bulkWrite(operations);
    const notifications = userIds.map((userId) => ({
      userId,
      title: "Monthly payment due",
      body: `${String(request.body.title || "Monthly gym fee")}: ₹${amount} for ${dueMonth}`,
      type: "payment_due",
      data: { dueMonth, amount: String(amount) },
      readAt: null,
      createdAt: now,
    }));
    if (notifications.length) {
      await db().collection("notifications").insertMany(notifications);
      for (const notification of notifications) {
        request.app.get("io")?.to(`user:${notification.userId}`).emit("notification", notification);
      }
    }
    response.status(201).json({ assigned: userIds.length });
  }),
);

router.get(
  "/payment-settings",
  asyncRoute(async (_request, response) => {
    const settings = await db().collection("settings").findOne({ key: "payments" });
    response.json(settings || { key: "payments", upiId: "", phone: "", qrUrl: "" });
  }),
);

router.put(
  "/payment-settings",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const settings = await db().collection("settings").findOneAndUpdate(
      { key: "payments" },
      {
        $set: {
          upiId: String(request.body.upiId || "").trim(),
          phone: String(request.body.phone || "").trim(),
          qrUrl: String(request.body.qrUrl || "").trim(),
          updatedAt: new Date(),
          updatedBy: request.user._id,
        },
      },
      { upsert: true, returnDocument: "after" },
    );
    response.json(settings);
  }),
);

router.post(
  "/payments",
  asyncRoute(async (request, response) => {
    const amount = Number(request.body.amount);
    if (!(amount > 0)) return response.status(400).json({ error: "INVALID_AMOUNT" });
    const payment = {
      userId: request.user._id,
      amount,
      transactionRef: String(request.body.transactionRef || "").trim(),
      proofUrl: String(request.body.proofUrl || "").trim(),
      subscriptionPlanId: request.body.subscriptionPlanId ? objectId(request.body.subscriptionPlanId) : null,
      dueId: request.body.dueId ? objectId(request.body.dueId) : null,
      status: "pending",
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    const result = await db().collection("payments").insertOne(payment);
    payment._id = result.insertedId;
    response.status(201).json(payment);
  }),
);

router.get(
  "/payments/me",
  asyncRoute(async (request, response) => {
    response.json(await db().collection("payments")
      .find({ userId: request.user._id }).sort({ createdAt: -1 }).toArray());
  }),
);

router.get(
  "/payments",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const { limit, skip, page } = pageOptions(request.query);
    const filter = request.query.status ? { status: request.query.status } : {};
    const items = await db().collection("payments").find(filter)
      .sort({ createdAt: -1 }).skip(skip).limit(limit).toArray();
    response.json({ items, page, limit });
  }),
);

router.patch(
  "/payments/:id/status",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    if (!["approved", "rejected"].includes(request.body.status)) {
      return response.status(400).json({ error: "INVALID_PAYMENT_STATUS" });
    }
    const payment = await db().collection("payments").findOneAndUpdate(
      { _id: objectId(request.params.id), status: "pending" },
      {
        $set: {
          status: request.body.status,
          adminNote: String(request.body.adminNote || ""),
          reviewedBy: request.user._id,
          reviewedAt: new Date(),
          updatedAt: new Date(),
        },
      },
      { returnDocument: "after" },
    );
    if (!payment) return response.status(404).json({ error: "PENDING_PAYMENT_NOT_FOUND" });
    if (payment.status === "approved" && payment.dueId) {
      await db().collection("memberDues").updateOne(
        { _id: payment.dueId },
        { $set: { status: "paid", paidAt: new Date(), paymentId: payment._id } },
      );
    }
    const notification = {
      userId: payment.userId,
      title: payment.status === "approved" ? "Payment approved" : "Payment rejected",
      body: payment.status === "approved"
        ? `Your payment of ₹${payment.amount} was verified.`
        : "Your payment could not be verified. Please contact the gym admin.",
      type: "payment",
      data: { paymentId: payment._id.toString(), status: payment.status },
      readAt: null,
      createdAt: new Date(),
    };
    await db().collection("notifications").insertOne(notification);
    request.app.get("io")?.to(`user:${payment.userId}`).emit("notification", notification);
    response.json(payment);
  }),
);

module.exports = router;
