const express = require("express");
const { db } = require("../db");
const { authenticate, authorize } = require("../middleware/auth");
const { asyncRoute, objectId } = require("../utils/http");

const router = express.Router();
router.use(authenticate);

router.get(
  "/diet-plans",
  asyncRoute(async (request, response) => {
    const filter =
      request.user.role === "admin"
        ? {}
        : {
            status: "active",
            $or: [{ audience: "all" }, { userIds: request.user._id }],
          };
    response.json(await db().collection("dietPlans").find(filter).sort({ createdAt: -1 }).toArray());
  }),
);

router.post(
  "/diet-plans",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const title = String(request.body.title || "").trim();
    if (!title) return response.status(400).json({ error: "TITLE_REQUIRED" });
    const plan = {
      title,
      description: String(request.body.description || ""),
      meals: Array.isArray(request.body.meals) ? request.body.meals : [],
      audience: request.body.audience === "selected" ? "selected" : "all",
      userIds: (request.body.userIds || []).map(objectId),
      status: request.body.status === "inactive" ? "inactive" : "active",
      createdBy: request.user._id,
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    const result = await db().collection("dietPlans").insertOne(plan);
    plan._id = result.insertedId;
    response.status(201).json(plan);
  }),
);

router.patch(
  "/diet-plans/:id",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const allowed = {};
    for (const key of ["title", "description", "meals", "audience", "status"]) {
      if (request.body[key] !== undefined) allowed[key] = request.body[key];
    }
    if (request.body.userIds) allowed.userIds = request.body.userIds.map(objectId);
    allowed.updatedAt = new Date();
    const item = await db().collection("dietPlans").findOneAndUpdate(
      { _id: objectId(request.params.id) },
      { $set: allowed },
      { returnDocument: "after" },
    );
    if (!item) return response.status(404).json({ error: "DIET_PLAN_NOT_FOUND" });
    response.json(item);
  }),
);

router.delete(
  "/diet-plans/:id",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const result = await db().collection("dietPlans").deleteOne({ _id: objectId(request.params.id) });
    if (!result.deletedCount) return response.status(404).json({ error: "DIET_PLAN_NOT_FOUND" });
    response.status(204).end();
  }),
);

router.post(
  "/diet-plans/:id/assign",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const planId = objectId(request.params.id);
    const userIds = (request.body.userIds || []).map(objectId);
    if (!userIds.length) return response.status(400).json({ error: "USER_IDS_REQUIRED" });
    const now = new Date();
    const assignments = userIds.map((userId) => ({
      planId, userId, assignedBy: request.user._id, assignedAt: now, active: true,
    }));
    await db().collection("dietAssignments").updateMany(
      { userId: { $in: userIds }, active: true },
      { $set: { active: false, endedAt: now } },
    );
    await db().collection("dietAssignments").insertMany(assignments);
    response.status(201).json(assignments);
  }),
);

router.get(
  "/diet-assignments/me",
  asyncRoute(async (request, response) => {
    response.json(await db().collection("dietAssignments").aggregate([
      { $match: { userId: request.user._id } },
      { $sort: { assignedAt: -1 } },
      { $lookup: { from: "dietPlans", localField: "planId", foreignField: "_id", as: "plan" } },
      { $unwind: "$plan" },
    ]).toArray());
  }),
);

router.get(
  "/diet-assignments/user/:userId",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    response.json(await db().collection("dietAssignments")
      .find({ userId: objectId(request.params.userId) }).sort({ assignedAt: -1 }).toArray());
  }),
);

router.get(
  "/subscription-plans",
  asyncRoute(async (request, response) => {
    const filter = request.user.role === "admin" ? {} : { active: true };
    response.json(await db().collection("subscriptionPlans").find(filter).sort({ price: 1 }).toArray());
  }),
);

router.post(
  "/subscription-plans",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const name = String(request.body.name || "").trim();
    const price = Number(request.body.price);
    const durationDays = Number(request.body.durationDays);
    if (!name || price < 0 || durationDays < 1) return response.status(400).json({ error: "INVALID_PLAN" });
    const plan = {
      name, price, durationDays,
      description: String(request.body.description || ""),
      active: request.body.active !== false,
      createdAt: new Date(), updatedAt: new Date(),
    };
    const result = await db().collection("subscriptionPlans").insertOne(plan);
    plan._id = result.insertedId;
    response.status(201).json(plan);
  }),
);

router.patch(
  "/subscription-plans/:id",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const update = { updatedAt: new Date() };
    for (const key of ["name", "price", "durationDays", "description", "active"]) {
      if (request.body[key] !== undefined) update[key] = request.body[key];
    }
    const item = await db().collection("subscriptionPlans").findOneAndUpdate(
      { _id: objectId(request.params.id) }, { $set: update }, { returnDocument: "after" },
    );
    if (!item) return response.status(404).json({ error: "PLAN_NOT_FOUND" });
    response.json(item);
  }),
);

router.delete(
  "/subscription-plans/:id",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    await db().collection("subscriptionPlans").updateOne(
      { _id: objectId(request.params.id) },
      { $set: { active: false, updatedAt: new Date() } },
    );
    response.status(204).end();
  }),
);

router.get(
  "/member-subscriptions/me",
  asyncRoute(async (request, response) => {
    response.json(await db().collection("memberSubscriptions")
      .find({ userId: request.user._id }).sort({ startDate: -1 }).toArray());
  }),
);

router.get(
  "/member-subscriptions",
  authorize("admin"),
  asyncRoute(async (_request, response) => {
    response.json(await db().collection("memberSubscriptions").find().sort({ startDate: -1 }).toArray());
  }),
);

router.post(
  "/member-subscriptions",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const subscription = {
      userId: objectId(request.body.userId),
      planId: objectId(request.body.planId),
      startDate: new Date(request.body.startDate || Date.now()),
      endDate: new Date(request.body.endDate),
      status: request.body.status || "active",
      createdBy: request.user._id,
      createdAt: new Date(),
    };
    if (Number.isNaN(subscription.endDate.valueOf())) {
      return response.status(400).json({ error: "END_DATE_REQUIRED" });
    }
    const result = await db().collection("memberSubscriptions").insertOne(subscription);
    subscription._id = result.insertedId;
    response.status(201).json(subscription);
  }),
);

router.patch(
  "/member-subscriptions/:id",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const update = { updatedAt: new Date() };
    for (const key of ["status", "startDate", "endDate"]) {
      if (request.body[key] !== undefined) {
        update[key] = key.endsWith("Date") ? new Date(request.body[key]) : request.body[key];
      }
    }
    const item = await db().collection("memberSubscriptions").findOneAndUpdate(
      { _id: objectId(request.params.id) }, { $set: update }, { returnDocument: "after" },
    );
    if (!item) return response.status(404).json({ error: "SUBSCRIPTION_NOT_FOUND" });
    response.json(item);
  }),
);

router.delete(
  "/member-subscriptions/:id",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    await db().collection("memberSubscriptions").updateOne(
      { _id: objectId(request.params.id) },
      { $set: { status: "cancelled", updatedAt: new Date() } },
    );
    response.status(204).end();
  }),
);

module.exports = router;
