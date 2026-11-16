const express = require("express");
const config = require("../config");
const { db } = require("../db");
const { authenticate, authorize } = require("../middleware/auth");
const { asyncRoute, objectId, pageOptions } = require("../utils/http");
const { haversineMeters } = require("../utils/geo");

const router = express.Router();
router.use(authenticate);

function checkedLocation(body) {
  const latitude = Number(body.latitude);
  const longitude = Number(body.longitude);
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    const error = new Error("Location required");
    error.status = 400;
    error.code = "LOCATION_REQUIRED";
    throw error;
  }
  const distanceMeters = haversineMeters(
    latitude,
    longitude,
    config.gymLocation.latitude,
    config.gymLocation.longitude,
  );
  if (distanceMeters > config.attendanceRadiusMeters) {
    const error = new Error("Outside gym radius");
    error.status = 403;
    error.code = "OUTSIDE_GYM_RADIUS";
    throw error;
  }
  return { latitude, longitude, distanceMeters: Math.round(distanceMeters * 10) / 10 };
}

router.post(
  "/check-in",
  asyncRoute(async (request, response) => {
    const open = await db().collection("attendance").findOne({
      userId: request.user._id,
      checkOutAt: null,
    });
    if (open) return response.status(409).json({ error: "ALREADY_CHECKED_IN" });
    const attendance = {
      userId: request.user._id,
      checkInAt: new Date(),
      checkInLocation: checkedLocation(request.body),
      checkOutAt: null,
    };
    const result = await db().collection("attendance").insertOne(attendance);
    attendance._id = result.insertedId;
    response.status(201).json(attendance);
  }),
);

router.post(
  "/check-out",
  asyncRoute(async (request, response) => {
    const attendance = await db().collection("attendance").findOneAndUpdate(
      { userId: request.user._id, checkOutAt: null },
      { $set: { checkOutAt: new Date(), checkOutLocation: checkedLocation(request.body) } },
      { sort: { checkInAt: -1 }, returnDocument: "after" },
    );
    if (!attendance) return response.status(409).json({ error: "NOT_CHECKED_IN" });
    response.json(attendance);
  }),
);

router.get(
  "/me",
  asyncRoute(async (request, response) => {
    const { limit, skip, page } = pageOptions(request.query);
    const items = await db().collection("attendance").find({ userId: request.user._id })
      .sort({ checkInAt: -1 }).skip(skip).limit(limit).toArray();
    response.json({ items, page, limit });
  }),
);

router.get(
  "/",
  authorize("admin"),
  asyncRoute(async (request, response) => {
    const { limit, skip, page } = pageOptions(request.query);
    const filter = {};
    if (request.query.userId) filter.userId = objectId(request.query.userId);
    if (request.query.from || request.query.to) {
      filter.checkInAt = {};
      if (request.query.from) filter.checkInAt.$gte = new Date(request.query.from);
      if (request.query.to) filter.checkInAt.$lte = new Date(request.query.to);
    }
    const items = await db().collection("attendance").find(filter)
      .sort({ checkInAt: -1 }).skip(skip).limit(limit).toArray();
    response.json({ items, page, limit });
  }),
);

module.exports = router;
