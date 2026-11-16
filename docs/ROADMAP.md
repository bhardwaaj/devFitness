# DEV Fitness Club — Roadmap

One Android app for both admins and gym members. After sign-in, the account's `role` (`admin` or `member`) decides which dashboard and bottom tabs appear.

Status key: `[x]` done, `[ ]` to do. UI screens wait for the Figma designs; backend and data work can start before them.

---

## Checkpoint 0 — Foundation (done)

- [x] Splash, sign-in, and create-account screens with iOS-style motion and gym loaders
- [x] Club emblem used as app logo and launcher icon
- [x] Atlas cluster `cluster0.wmsp4rr.mongodb.net`, database `devfitness`, collection `users`
- [x] PBKDF2 password hashing, unique email index
- [x] Admin seeded: `abhay.singh191103@gmail.com` (role `admin`) via `backend/scripts/seed-admin.js`
- [x] `role` returned on sign-in (`Admin` / `Member`); new accounts default to `member`

## Checkpoint 1 — Backend API and security (blocks everything below)

The app currently talks to Atlas directly with database credentials inside the APK. That's acceptable for a prototype, but anyone who unpacks the APK gets full database access, and chat over sockets needs a server anyway. Before members install the app:

- [ ] Node.js + Express API in `backend/` (same MongoDB Atlas database)
- [ ] Socket.IO server on the same backend for chat and live updates
- [ ] JWT sign-in: `POST /auth/login` returns token + role; app stores token in encrypted storage
- [ ] Remove Atlas credentials from the Android app; app calls the API only
- [ ] Role middleware: admin-only routes rejected for members
- [ ] Media storage for post images, QR code, profile photos (decision needed — see Open decisions)
- [ ] Firebase Cloud Messaging for push notifications (needs a Firebase project)
- [ ] Deploy backend (decision needed — see Open decisions); restrict Atlas Network Access to the server IP
- [ ] Rotate any Atlas credentials previously shared outside the server environment

## Checkpoint 2 — Accounts, roles, and app shell

- [ ] Turn off public sign-up; only admins create members (remove or hide the Create account screen)
- [ ] Role-based navigation after sign-in: admin shell vs member shell
- [ ] Bottom tab bar for each role; Chats tab on both
- [ ] Session persistence (stay signed in) and Logout on both roles
- [ ] Force password change on a member's first sign-in (recommended, since admin shares the initial password)

## Checkpoint 3 — Admin: User management (CRUD)

- [ ] Create member: name, phone, email, initial password, joining date, plan
- [ ] Share credentials to the member (WhatsApp / SMS share sheet with app download link + login details)
- [ ] List and search members, filter by paid / unpaid / active / inactive
- [ ] Edit member details, reset password
- [ ] Deactivate / delete member

## Checkpoint 4 — Home feed (posts like Instagram)

Admin
- [ ] Home dashboard feed of all posts
- [ ] Posts management CRUD: create (image(s) + caption), edit, delete
- [ ] Post options menu (edit, delete, hide comments)

Member
- [ ] Dashboard feed with all posts
- [ ] Like / unlike, comment, see like and comment counts
- [ ] Live like/comment updates via Socket.IO

## Checkpoint 5 — Blogs, notices, and offers (popups)

Admin
- [ ] Create / edit / delete blog, notice, or offer with a start date and a deadline
- [ ] Choose audience: all members (selective audience optional)

Member
- [ ] Active notices and offers show as a popup on app open until their deadline
- [ ] "Don't show again today" / dismiss behaviour (decision needed)
- [ ] Blogs readable from the dashboard

## Checkpoint 6 — Diet plans

Admin
- [ ] Broadcast diet plan to all members
- [ ] Assign diet plan to selected members
- [ ] Diet plan history per member (every plan ever assigned, with dates)

Member
- [ ] Current diet plan screen
- [ ] Past diet plans (history)
- [ ] Notification when a new plan is assigned

## Checkpoint 7 — Subscriptions and payments

Admin
- [ ] Subscription plans CRUD (name, duration, price)
- [ ] Paid vs unpaid members list, expiry dates, expiring-soon filter
- [ ] Payment settings: upload UPI QR code, UPI ID / phone number
- [ ] Payment verification queue: approve or reject member payments
- [ ] Manually mark a member as paid (cash at the desk)

Member
- [ ] Subscription plan screen: current plan, expiry, status
- [ ] Renew subscription: opens the Paytm app directly with a UPI payment link (`upi://pay?pa=…&am=…&tn=…`, package `net.one97.paytm`), no payment gateway
- [ ] Show the result Paytm returns (success / failure / pending) and save it as a pending payment with the UPI transaction ID
- [ ] Transaction history with status (pending, verified, rejected)

Limitation: without a payment gateway, Paytm's response comes back through the phone and can't prove the money arrived. The status the member sees is "pending" until the admin confirms the payment in their Paytm/bank account and approves it in the verification queue.

## Checkpoint 8 — Attendance (location-based check-in / check-out)

Member
- [ ] Check-in / Check-out button appears only within 50 m of the gym: 26.882172, 75.904685
- [ ] Location permission flow (fine location) and clear message when outside the radius
- [ ] Reject mock/fake GPS locations
- [ ] Attendance sheet: monthly calendar of check-ins, time in / time out, total hours

Admin
- [ ] Attendance dashboard: who is checked in now, daily list, per-member history, date filters
- [ ] Export attendance (CSV) (optional)

The server re-checks the coordinates and timestamp on every check-in; the phone's distance check alone can be bypassed.

## Checkpoint 9 — Chats

- [ ] Chats tab at the bottom for admin and members
- [ ] One-to-one chat with anyone (admin ↔ member, member ↔ member — confirm whether member ↔ member is allowed)
- [ ] Real-time delivery over Socket.IO
- [ ] Sent / delivered / seen ticks, typing indicator, online status
- [ ] Chat list with unread counts; push notification when the app is closed

## Checkpoint 10 — Profile and notifications

Member
- [ ] Profile: photo, name, phone, plan, expiry, change password
- [ ] Notifications screen: new posts, notices, offers, diet plans, payment approved/rejected, subscription expiring, chat messages

Admin
- [ ] Notifications for new payments awaiting verification and new chat messages

## Checkpoint 11 — Release

- [ ] Signed release APK and shareable download link
- [ ] Crash reporting
- [ ] End-to-end test pass on a real device for both roles

---

## Open decisions

1. **Backend hosting:** Render, Railway, or a VPS.
2. **Image storage:** MongoDB GridFS (simplest, same database) or Cloudinary/S3 (faster, cheaper at scale).
3. **Member ↔ member chat:** allowed, or members chat with admin only.
4. **Popups:** show on every app open until the deadline, or once per day.
5. **App distribution:** direct APK download link, or Play Store.
