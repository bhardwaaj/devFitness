# DEV Fitness Club backend

Prototype Express, MongoDB and Socket.IO API for the DEV Fitness Club apps.

## Run

Requires Node.js 18+ and MongoDB.

```bash
npm install
MONGODB_URI='mongodb://...' JWT_SECRET='use-a-long-random-value' npm start
```

Configuration:

- `MONGODB_URI` (recommended) or the existing `../secrets.properties` MongoDB values
- `MONGODB_DB` (default `devfitness`)
- `JWT_SECRET` (required for stable sessions; never commit it)
- `PORT` (default `3000`)
- `CORS_ORIGIN` (optional comma-separated Socket.IO origins)

Seed or update the compatible PBKDF2-HMAC-SHA256 admin (12,000 iterations):

```bash
ADMIN_EMAIL='admin@example.com' ADMIN_PASSWORD='strong-password' npm run seed:admin
```

All protected requests use `Authorization: Bearer <token>`. Collection endpoints support
`page` and `limit` where applicable. Dates are ISO-8601 strings.

## REST API

### Authentication and members

- `POST /auth/register`, `POST /auth/login`, `GET /auth/me`
- Admin: `GET|POST /users`, `GET|PATCH|DELETE /users/:id`
- Admin: `POST /users/:id/reset-password`

Deleting a user deactivates the account rather than erasing history.

### Community posts

- `GET|POST /posts`, `PATCH|DELETE /posts/:id`
- `POST|DELETE /posts/:id/likes`
- `GET|POST /posts/:id/comments`
- `DELETE /posts/:postId/comments/:commentId`

### Diets and subscriptions

- `GET|POST /diet-plans`, `PATCH|DELETE /diet-plans/:id` (writes are admin-only)
- Admin: `POST /diet-plans/:id/assign`, `GET /diet-assignments/user/:userId`
- Member: `GET /diet-assignments/me`
- `GET|POST /subscription-plans`, `PATCH|DELETE /subscription-plans/:id`
- Member: `GET /member-subscriptions/me`
- Admin: `GET|POST /member-subscriptions`, `PATCH|DELETE /member-subscriptions/:id`

Diet plans use `audience: "all"` or `"selected"` with `userIds`. Assignments retain history.

### Payments

- `GET /payment-settings`; admin `PUT /payment-settings`
- Member: `POST /payments`, `GET /payments/me`
- Admin: `GET /payments`, `PATCH /payments/:id/status` with `approved` or `rejected`

Payment settings contain `upiId`, `phone`, and `qrUrl`; no credentials are stored there.

### Published content

The same CRUD shape is available at `/blogs`, `/notices`, and `/offers`:

- `GET /<type>`
- Admin: `POST /<type>`, `PATCH|DELETE /<type>/:id`

Content supports `audience`, selected `userIds`, `deadline`, and draft/published status.

### Attendance

- Member: `POST /attendance/check-in`, `POST /attendance/check-out`, `GET /attendance/me`
- Admin: `GET /attendance` (optional `userId`, `from`, and `to`)

The server calculates Haversine distance from `26.882172,75.904685`; both check-in and
check-out must be within 50 metres. Client-reported distance is ignored.

### Notifications and chat

- `GET /notifications`, `PATCH /notifications/:id/read`
- Admin: `POST /notifications` for selected `userIds` or `audience: "all"`
- `GET|POST /chat/conversations`
- `GET|POST /chat/conversations/:id/messages`
- `PATCH /chat/conversations/:id/seen`

## Socket.IO

Connect with the JWT in `auth.token` (or a Bearer header). Events:

- Client: `chat:join { conversationId }`
- Client: `chat:send { conversationId, text }`
- Server: `chat:message`, `chat:seen`, `notification`
- Feed broadcasts: `feed:post`, `feed:update`, `feed:delete`, `feed:comment`

The server verifies conversation membership before joining rooms or accepting messages.

`GET /health` returns the process health response.
