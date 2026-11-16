const jwt = require("jsonwebtoken");
const config = require("../config");
const { db } = require("../db");
const { objectId } = require("../utils/http");

async function authenticate(request, response, next) {
  try {
    const [scheme, token] = String(request.headers.authorization || "").split(" ");
    if (scheme !== "Bearer" || !token) {
      return response.status(401).json({ error: "AUTH_REQUIRED" });
    }
    const payload = jwt.verify(token, config.jwtSecret, { issuer: config.jwtIssuer });
    const user = await db().collection("users").findOne({ _id: objectId(payload.sub) });
    if (!user || user.status === "inactive") {
      return response.status(401).json({ error: "ACCOUNT_UNAVAILABLE" });
    }
    request.user = user;
    next();
  } catch (_error) {
    response.status(401).json({ error: "INVALID_TOKEN" });
  }
}

function authorize(...roles) {
  return (request, response, next) => {
    if (!roles.includes(request.user?.role)) {
      return response.status(403).json({ error: "FORBIDDEN" });
    }
    next();
  };
}

function verifySocketToken(token) {
  return jwt.verify(token, config.jwtSecret, { issuer: config.jwtIssuer });
}

module.exports = { authenticate, authorize, verifySocketToken };
