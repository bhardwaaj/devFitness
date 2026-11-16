const { ObjectId } = require("mongodb");
const { decryptSensitive } = require("./sensitive");

function asyncRoute(handler) {
  return (request, response, next) => Promise.resolve(handler(request, response, next)).catch(next);
}

function objectId(value) {
  if (!ObjectId.isValid(value)) {
    const error = new Error("Invalid resource id");
    error.status = 400;
    error.code = "INVALID_ID";
    throw error;
  }
  return new ObjectId(value);
}

function publicUser(user) {
  if (!user) return null;
  return {
    id: user._id.toString(),
    name: user.name || "",
    email: user.email,
    role: user.role || "member",
    status: user.status || "active",
    phone: user.phone || "",
    age: user.age || null,
    heightCm: user.heightCm || null,
    weightKg: user.weightKg || null,
    dailyCalories: user.dailyCalories || null,
    fitnessGoal: user.fitnessGoal || "",
    verificationStatus: user.verificationStatus || "unverified",
    verificationNote: user.verificationNote || "",
    aadhaarSubmitted: Boolean(user.aadhaarEncrypted && user.aadhaarPhotoFile),
    createdAt: user.createdAt,
    updatedAt: user.updatedAt,
  };
}

function privateUser(user) {
  return {
    ...publicUser(user),
    aadhaarNumber: decryptSensitive(user?.aadhaarEncrypted),
    aadhaarPhotoPath: user?.aadhaarPhotoFile ? "/auth/me/aadhaar-photo" : "",
  };
}

function adminUser(user) {
  return {
    ...publicUser(user),
    aadhaarNumber: decryptSensitive(user?.aadhaarEncrypted),
    aadhaarPhotoPath: user?.aadhaarPhotoFile ? `/users/${user._id}/aadhaar-photo` : "",
  };
}

function pageOptions(query) {
  const limit = Math.min(Math.max(Number(query.limit) || 30, 1), 100);
  const page = Math.max(Number(query.page) || 1, 1);
  return { limit, skip: (page - 1) * limit, page };
}

function errorHandler(error, _request, response, _next) {
  if (error?.code === 11000) return response.status(409).json({ error: "DUPLICATE_RESOURCE" });
  const status = error.status || 500;
  if (status >= 500) console.error(error);
  return response.status(status).json({ error: error.code || "SERVER_ERROR" });
}

module.exports = {
  asyncRoute,
  objectId,
  publicUser,
  privateUser,
  adminUser,
  pageOptions,
  errorHandler,
};
