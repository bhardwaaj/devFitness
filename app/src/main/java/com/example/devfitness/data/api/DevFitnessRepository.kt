package com.example.devfitness.data.api

import com.example.devfitness.data.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Typed, synchronous access to the DEV Fitness REST API.
 * All methods perform network I/O and must be called off Android's main thread.
 */
class DevFitnessRepository(private val client: ApiClient) {
    // Users and members
    fun getCurrentUser(): ApiResult<User> =
        client.get("/auth/me").map(ModelJson::user)

    fun updateCurrentUser(
        name: String? = null,
        phone: String? = null,
        age: Int? = null,
        heightCm: Double? = null,
        weightKg: Double? = null,
        dailyCalories: Int? = null,
        fitnessGoal: String? = null,
        aadhaarNumber: String? = null,
        aadhaarConsent: Boolean? = null,
    ): ApiResult<User> =
        client.patch(
            "/auth/me",
            JSONObject().apply {
                putIfNotNull(this, "name", name)
                putIfNotNull(this, "phone", phone)
                putIfNotNull(this, "age", age)
                putIfNotNull(this, "heightCm", heightCm)
                putIfNotNull(this, "weightKg", weightKg)
                putIfNotNull(this, "dailyCalories", dailyCalories)
                putIfNotNull(this, "fitnessGoal", fitnessGoal)
                putIfNotNull(this, "aadhaarNumber", aadhaarNumber)
                putIfNotNull(this, "aadhaarConsent", aadhaarConsent)
            },
        ).map(ModelJson::user)

    fun uploadAadhaarPhoto(bytes: ByteArray, mimeType: String): ApiResult<User> =
        client.upload("/auth/me/aadhaar-photo", bytes, mimeType).map(ModelJson::user)

    fun reviewUserVerification(
        userId: String,
        approved: Boolean,
        note: String = "",
    ): ApiResult<User> = client.patch(
        "/users/${id(userId)}/verification",
        JSONObject()
            .put("status", if (approved) "approved" else "rejected")
            .put("note", note),
    ).map(ModelJson::user)

    fun updateCurrentPassword(currentPassword: String, newPassword: String): ApiResult<Unit> =
        client.patch(
            "/auth/me/password",
            JSONObject().put("currentPassword", currentPassword).put("newPassword", newPassword),
        ).map { Unit }

    fun listUsers(role: String? = null, active: Boolean? = null): ApiResult<List<User>> =
        client.get("/users", query(role = role, status = active?.let { if (it) "active" else "inactive" }))
            .map { it.arrayPayload("users", "items").mapObjects(ModelJson::user) }

    fun getUser(userId: String): ApiResult<User> =
        client.get("/users/${id(userId)}").map { ModelJson.user(it.objectPayload("user")) }

    fun createUser(
        name: String,
        email: String,
        password: String,
        role: String,
        phone: String? = null,
        active: Boolean = true,
    ): ApiResult<User> = client.post(
        "/users",
        JSONObject()
            .put("name", name)
            .put("email", email)
            .put("password", password)
            .put("role", role)
            .put("status", if (active) "active" else "inactive")
            .apply { putIfNotNull(this, "phone", phone) },
    ).map { ModelJson.user(it.objectPayload("user")) }

    fun updateUser(
        userId: String,
        name: String? = null,
        email: String? = null,
        role: String? = null,
        phone: String? = null,
        active: Boolean? = null,
    ): ApiResult<User> = client.patch(
        "/users/${id(userId)}",
        JSONObject().apply {
            putIfNotNull(this, "name", name)
            putIfNotNull(this, "email", email)
            putIfNotNull(this, "role", role)
            putIfNotNull(this, "phone", phone)
            putIfNotNull(this, "status", active?.let { if (it) "active" else "inactive" })
        },
    ).map { ModelJson.user(it.objectPayload("user")) }

    fun deleteUser(userId: String): ApiResult<Unit> =
        client.delete("/users/${id(userId)}").map { Unit }

    fun resetUserPassword(userId: String, password: String): ApiResult<Unit> =
        client.post("/users/${id(userId)}/reset-password", JSONObject().put("password", password)).map { Unit }

    fun listMembers(active: Boolean? = null): ApiResult<List<Member>> =
        client.get("/users", query(role = "member", active = active))
            .map { it.arrayPayload("members", "users", "items").mapObjects(ModelJson::member) }

    fun getMember(memberId: String): ApiResult<Member> =
        client.get("/users/${id(memberId)}").map { ModelJson.member(it.objectPayload("member", "user")) }

    // Posts, likes, and comments
    fun listPosts(page: Int? = null, limit: Int? = null): ApiResult<List<Post>> =
        client.get("/posts", query(page = page, limit = limit))
            .map { it.arrayPayload("posts", "items").mapObjects(ModelJson::post) }

    fun getPost(postId: String): ApiResult<Post> =
        client.get("/posts/${id(postId)}").map { ModelJson.post(it.objectPayload("post")) }

    fun createPost(text: String, imageUrl: String? = null): ApiResult<Post> =
        client.post(
            "/posts",
            JSONObject().put("text", text).apply { putIfNotNull(this, "mediaUrl", imageUrl) },
        ).map { ModelJson.post(it.objectPayload("post")) }

    fun updatePost(postId: String, text: String? = null, imageUrl: String? = null): ApiResult<Post> =
        client.patch(
            "/posts/${id(postId)}",
            JSONObject().apply {
                putIfNotNull(this, "text", text)
                putIfNotNull(this, "mediaUrl", imageUrl)
            },
        ).map { ModelJson.post(it.objectPayload("post")) }

    fun deletePost(postId: String): ApiResult<Unit> =
        client.delete("/posts/${id(postId)}").map { Unit }

    fun likePost(postId: String): ApiResult<Unit> =
        client.post("/posts/${id(postId)}/likes").map { Unit }

    fun unlikePost(postId: String): ApiResult<Unit> =
        client.delete("/posts/${id(postId)}/likes").map { Unit }

    fun listComments(postId: String): ApiResult<List<Comment>> =
        client.get("/posts/${id(postId)}/comments")
            .map { it.arrayPayload("comments", "items").mapObjects(ModelJson::comment) }

    fun addComment(postId: String, text: String): ApiResult<Comment> =
        client.post("/posts/${id(postId)}/comments", JSONObject().put("text", text))
            .map { ModelJson.comment(it.objectPayload("comment")) }

    fun updateComment(postId: String, commentId: String, text: String): ApiResult<Comment> =
        client.patch(
            "/posts/${id(postId)}/comments/${id(commentId)}",
            JSONObject().put("text", text),
        ).map { ModelJson.comment(it.objectPayload("comment")) }

    fun deleteComment(postId: String, commentId: String): ApiResult<Unit> =
        client.delete("/posts/${id(postId)}/comments/${id(commentId)}").map { Unit }

    // Diet plans, assignments, and member history
    fun listDietPlans(active: Boolean? = null): ApiResult<List<DietPlan>> =
        client.get("/diet-plans", query(active = active))
            .map { it.arrayPayload("dietPlans", "plans", "items").mapObjects(ModelJson::dietPlan) }

    fun getDietPlan(planId: String): ApiResult<DietPlan> =
        client.get("/diet-plans/${id(planId)}")
            .map { ModelJson.dietPlan(it.objectPayload("dietPlan", "plan")) }

    fun createDietPlan(
        name: String,
        description: String? = null,
        meals: List<String> = emptyList(),
        calories: Int? = null,
        active: Boolean = true,
    ): ApiResult<DietPlan> = client.post(
        "/diet-plans",
        dietPlanBody(name, description, meals, calories, active),
    ).map { ModelJson.dietPlan(it.objectPayload("dietPlan", "plan")) }

    fun updateDietPlan(
        planId: String,
        name: String? = null,
        description: String? = null,
        meals: List<String>? = null,
        calories: Int? = null,
        active: Boolean? = null,
    ): ApiResult<DietPlan> = client.patch(
        "/diet-plans/${id(planId)}",
        JSONObject().apply {
            putIfNotNull(this, "title", name)
            putIfNotNull(this, "description", description)
            if (meals != null) put("meals", JSONArray(meals))
            putIfNotNull(this, "calories", calories)
            putIfNotNull(this, "status", active?.let { if (it) "active" else "inactive" })
        },
    ).map { ModelJson.dietPlan(it.objectPayload("dietPlan", "plan")) }

    fun deleteDietPlan(planId: String): ApiResult<Unit> =
        client.delete("/diet-plans/${id(planId)}").map { Unit }

    fun assignDietPlan(
        planId: String,
        memberId: String,
        startDate: String? = null,
        endDate: String? = null,
    ): ApiResult<List<DietAssignment>> =
        assignDietPlanToMembers(planId, listOf(memberId), startDate, endDate)

    fun assignDietPlanToMembers(
        planId: String,
        memberIds: List<String>,
        startDate: String? = null,
        endDate: String? = null,
    ): ApiResult<List<DietAssignment>> = client.post(
        "/diet-plans/${id(planId)}/assign",
        JSONObject().put("userIds", JSONArray(memberIds)).apply {
            putIfNotNull(this, "startDate", startDate)
            putIfNotNull(this, "endDate", endDate)
        },
    ).map { it.arrayPayload("assignments", "items").mapObjects(ModelJson::dietAssignment) }

    fun updateDietAssignment(
        assignmentId: String,
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null,
    ): ApiResult<DietAssignment> = client.patch(
        "/diet-plans/assignments/${id(assignmentId)}",
        JSONObject().apply {
            putIfNotNull(this, "status", status)
            putIfNotNull(this, "startDate", startDate)
            putIfNotNull(this, "endDate", endDate)
        },
    ).map { ModelJson.dietAssignment(it.objectPayload("assignment")) }

    fun deleteDietAssignment(assignmentId: String): ApiResult<Unit> =
        client.delete("/diet-plans/assignments/${id(assignmentId)}").map { Unit }

    fun dietHistory(memberId: String): ApiResult<List<DietAssignment>> =
        client.get("/diet-assignments/user/${id(memberId)}")
            .map { it.arrayPayload("history", "assignments", "items").mapObjects(ModelJson::dietAssignment) }

    fun myDietHistory(): ApiResult<List<DietAssignment>> =
        client.get("/diet-assignments/me")
            .map { it.arrayPayload("history", "assignments", "items").mapObjects(ModelJson::dietAssignment) }

    // Subscription plans and member subscriptions
    fun listSubscriptionPlans(active: Boolean? = null): ApiResult<List<SubscriptionPlan>> =
        client.get("/subscription-plans", query(active = active))
            .map { it.arrayPayload("plans", "items").mapObjects(ModelJson::subscriptionPlan) }

    fun createSubscriptionPlan(
        name: String,
        price: Double,
        durationDays: Int,
        description: String? = null,
        active: Boolean = true,
    ): ApiResult<SubscriptionPlan> = client.post(
        "/subscription-plans",
        JSONObject()
            .put("name", name)
            .put("price", price)
            .put("durationDays", durationDays)
            .put("active", active)
            .apply { putIfNotNull(this, "description", description) },
    ).map { ModelJson.subscriptionPlan(it.objectPayload("plan")) }

    fun updateSubscriptionPlan(
        planId: String,
        name: String? = null,
        price: Double? = null,
        durationDays: Int? = null,
        description: String? = null,
        active: Boolean? = null,
    ): ApiResult<SubscriptionPlan> = client.patch(
        "/subscription-plans/${id(planId)}",
        JSONObject().apply {
            putIfNotNull(this, "name", name)
            putIfNotNull(this, "price", price)
            putIfNotNull(this, "durationDays", durationDays)
            putIfNotNull(this, "description", description)
            putIfNotNull(this, "active", active)
        },
    ).map { ModelJson.subscriptionPlan(it.objectPayload("plan")) }

    fun deleteSubscriptionPlan(planId: String): ApiResult<Unit> =
        client.delete("/subscription-plans/${id(planId)}").map { Unit }

    fun listMemberSubscriptions(): ApiResult<List<MemberSubscription>> =
        client.get("/member-subscriptions")
            .map { it.arrayPayload("subscriptions", "items").mapObjects(ModelJson::memberSubscription) }

    fun mySubscriptions(): ApiResult<List<MemberSubscription>> =
        client.get("/member-subscriptions/me")
            .map { it.arrayPayload("subscriptions", "items").mapObjects(ModelJson::memberSubscription) }

    fun createMemberSubscription(
        memberId: String,
        planId: String,
        startsAt: String? = null,
        endsAt: String,
        status: String = "active",
    ): ApiResult<MemberSubscription> = client.post(
        "/member-subscriptions",
        JSONObject().put("userId", memberId).put("planId", planId).apply {
            putIfNotNull(this, "startDate", startsAt)
            put("endDate", endsAt)
            put("status", status)
        },
    ).map { ModelJson.memberSubscription(it.objectPayload("subscription")) }

    fun updateMemberSubscription(
        subscriptionId: String,
        status: String? = null,
        startsAt: String? = null,
        endsAt: String? = null,
    ): ApiResult<MemberSubscription> = client.patch(
        "/member-subscriptions/${id(subscriptionId)}",
        JSONObject().apply {
            putIfNotNull(this, "status", status)
            putIfNotNull(this, "startDate", startsAt)
            putIfNotNull(this, "endDate", endsAt)
        },
    ).map { ModelJson.memberSubscription(it.objectPayload("subscription")) }

    fun deleteMemberSubscription(subscriptionId: String): ApiResult<Unit> =
        client.delete("/member-subscriptions/${id(subscriptionId)}").map { Unit }

    // Payments
    fun getPaymentSettings(): ApiResult<PaymentSettings> =
        client.get("/payment-settings")
            .map { ModelJson.paymentSettings(it.objectPayload("settings")) }

    fun updatePaymentSettings(settings: PaymentSettings): ApiResult<PaymentSettings> =
        client.put("/payment-settings", paymentSettingsBody(settings))
            .map { ModelJson.paymentSettings(it.objectPayload("settings")) }

    fun listPayments(
        memberId: String? = null,
        status: String? = null,
    ): ApiResult<List<Payment>> = client.get(
        "/payments",
        query(memberId = memberId, status = status),
    ).map { it.arrayPayload("payments", "items").mapObjects(ModelJson::payment) }

    fun myPayments(): ApiResult<List<Payment>> =
        client.get("/payments/me")
            .map { it.arrayPayload("payments", "items").mapObjects(ModelJson::payment) }

    fun listMemberDues(): ApiResult<List<MemberDue>> =
        client.get("/dues/me")
            .map { it.arrayPayload("dues", "items").mapObjects(ModelJson::memberDue) }

    fun listAllDues(): ApiResult<List<MemberDue>> =
        client.get("/dues")
            .map { it.arrayPayload("dues", "items").mapObjects(ModelJson::memberDue) }

    fun assignMonthlyDue(
        memberIds: List<String>,
        allMembers: Boolean,
        title: String,
        amount: Double,
        dueMonth: String,
        dueDate: String,
    ): ApiResult<Int> = client.post(
        "/dues",
        JSONObject()
            .put("userIds", JSONArray(memberIds))
            .put("audience", if (allMembers) "all" else "selected")
            .put("title", title)
            .put("amount", amount)
            .put("dueMonth", dueMonth)
            .put("dueDate", dueDate),
    ).map { it.optInt("assigned", 0) }

    fun submitPayment(
        amount: Double,
        method: String? = null,
        reference: String? = null,
        subscriptionPlanId: String? = null,
        dueId: String? = null,
        currency: String = "INR",
    ): ApiResult<Payment> = client.post(
        "/payments",
        JSONObject()
            .put("amount", amount)
            .put("currency", currency)
            .apply {
                putIfNotNull(this, "method", method)
                putIfNotNull(this, "transactionRef", reference)
                putIfNotNull(this, "subscriptionPlanId", subscriptionPlanId)
                putIfNotNull(this, "dueId", dueId)
            },
    ).map { ModelJson.payment(it.objectPayload("payment")) }

    fun approvePayment(paymentId: String): ApiResult<Payment> =
        client.patch("/payments/${id(paymentId)}/status", JSONObject().put("status", "approved"))
            .map { ModelJson.payment(it.objectPayload("payment")) }

    fun reviewPayment(paymentId: String, approved: Boolean, adminNote: String? = null): ApiResult<Payment> =
        client.patch(
            "/payments/${id(paymentId)}/status",
            JSONObject()
                .put("status", if (approved) "approved" else "rejected")
                .apply { putIfNotNull(this, "adminNote", adminNote) },
        ).map { ModelJson.payment(it.objectPayload("payment")) }

    fun deletePayment(paymentId: String): ApiResult<Unit> =
        client.delete("/payments/${id(paymentId)}").map { Unit }

    // Notices, blogs, and offers
    fun listNotices(): ApiResult<List<Notice>> =
        client.get("/notices")
            .map { it.arrayPayload("notices", "items").mapObjects(ModelJson::notice) }

    fun createNotice(
        title: String,
        body: String,
        published: Boolean = true,
        deadline: String? = null,
        audience: String = "all",
        imageUrl: String? = null,
    ): ApiResult<Notice> =
        client.post(
            "/notices",
            JSONObject().put("title", title).put("body", body)
                .put("status", if (published) "published" else "draft")
                .put("audience", audience)
                .apply {
                    putIfNotNull(this, "deadline", deadline)
                    putIfNotNull(this, "imageUrl", imageUrl)
                },
        ).map { ModelJson.notice(it.objectPayload("notice")) }

    fun updateNotice(
        noticeId: String,
        title: String? = null,
        body: String? = null,
        published: Boolean? = null,
        deadline: String? = null,
        audience: String? = null,
        imageUrl: String? = null,
    ): ApiResult<Notice> = client.patch(
        "/notices/${id(noticeId)}",
        contentBody(title, body, published).apply {
            putIfNotNull(this, "deadline", deadline)
            putIfNotNull(this, "audience", audience)
            putIfNotNull(this, "imageUrl", imageUrl)
        },
    ).map { ModelJson.notice(it.objectPayload("notice")) }

    fun deleteNotice(noticeId: String): ApiResult<Unit> =
        client.delete("/notices/${id(noticeId)}").map { Unit }

    fun listBlogs(): ApiResult<List<Blog>> =
        client.get("/blogs")
            .map { it.arrayPayload("blogs", "items").mapObjects(ModelJson::blog) }

    fun createBlog(
        title: String,
        body: String,
        imageUrl: String? = null,
        published: Boolean = true,
    ): ApiResult<Blog> = client.post(
        "/blogs",
        JSONObject().put("title", title).put("body", body)
            .put("status", if (published) "published" else "draft").apply {
            putIfNotNull(this, "imageUrl", imageUrl)
        },
    ).map { ModelJson.blog(it.objectPayload("blog")) }

    fun updateBlog(
        blogId: String,
        title: String? = null,
        body: String? = null,
        imageUrl: String? = null,
        published: Boolean? = null,
    ): ApiResult<Blog> = client.patch(
        "/blogs/${id(blogId)}",
        contentBody(title, body, published).apply { putIfNotNull(this, "imageUrl", imageUrl) },
    ).map { ModelJson.blog(it.objectPayload("blog")) }

    fun deleteBlog(blogId: String): ApiResult<Unit> =
        client.delete("/blogs/${id(blogId)}").map { Unit }

    fun listOffers(active: Boolean? = null): ApiResult<List<Offer>> =
        client.get("/offers", query(active = active))
            .map { it.arrayPayload("offers", "items").mapObjects(ModelJson::offer) }

    fun createOffer(
        title: String,
        description: String,
        code: String? = null,
        discountPercent: Double? = null,
        startsAt: String? = null,
        endsAt: String? = null,
        active: Boolean = true,
        imageUrl: String? = null,
    ): ApiResult<Offer> = client.post(
        "/offers",
        offerBody(title, description, code, discountPercent, startsAt, endsAt, active, imageUrl),
    ).map { ModelJson.offer(it.objectPayload("offer")) }

    fun updateOffer(
        offerId: String,
        title: String? = null,
        description: String? = null,
        code: String? = null,
        discountPercent: Double? = null,
        startsAt: String? = null,
        endsAt: String? = null,
        active: Boolean? = null,
        imageUrl: String? = null,
    ): ApiResult<Offer> = client.patch(
        "/offers/${id(offerId)}",
        offerBody(title, description, code, discountPercent, startsAt, endsAt, active, imageUrl),
    ).map { ModelJson.offer(it.objectPayload("offer")) }

    fun deleteOffer(offerId: String): ApiResult<Unit> =
        client.delete("/offers/${id(offerId)}").map { Unit }

    // Attendance
    fun checkIn(latitude: Double, longitude: Double): ApiResult<AttendanceRecord> =
        client.post(
            "/attendance/check-in",
            JSONObject().put("latitude", latitude).put("longitude", longitude),
        )
            .map { ModelJson.attendance(it.objectPayload("attendance", "record")) }

    fun checkOut(latitude: Double, longitude: Double): ApiResult<AttendanceRecord> =
        client.post(
            "/attendance/check-out",
            JSONObject().put("latitude", latitude).put("longitude", longitude),
        )
            .map { ModelJson.attendance(it.objectPayload("attendance", "record")) }

    fun myAttendance(): ApiResult<List<AttendanceRecord>> =
        client.get("/attendance/me")
            .map { it.arrayPayload("attendance", "records", "items").mapObjects(ModelJson::attendance) }

    fun attendanceHistory(
        memberId: String? = null,
        from: String? = null,
        to: String? = null,
    ): ApiResult<List<AttendanceRecord>> = client.get(
        "/attendance",
        query(memberId = memberId, from = from, to = to),
    ).map { it.arrayPayload("attendance", "records", "items").mapObjects(ModelJson::attendance) }

    // Notifications
    fun listNotifications(unreadOnly: Boolean? = null): ApiResult<List<NotificationItem>> =
        client.get("/notifications", query(unread = unreadOnly))
            .map { it.arrayPayload("notifications", "items").mapObjects(ModelJson::notification) }

    fun markNotificationRead(notificationId: String): ApiResult<NotificationItem> =
        client.patch("/notifications/${id(notificationId)}/read")
            .map { ModelJson.notification(it.objectPayload("notification")) }

    fun markAllNotificationsRead(): ApiResult<Unit> =
        client.patch("/notifications/read").map { Unit }

    fun deleteNotification(notificationId: String): ApiResult<Unit> =
        client.delete("/notifications/${id(notificationId)}").map { Unit }

    fun sendNotification(
        title: String,
        body: String,
        memberIds: List<String>,
        allMembers: Boolean,
    ): ApiResult<Int> = client.post(
        "/notifications",
        JSONObject()
            .put("title", title)
            .put("body", body)
            .put("audience", if (allMembers) "all" else "selected")
            .put("userIds", JSONArray(memberIds)),
    ).map { it.optInt("created", 0) }

    fun uploadMedia(bytes: ByteArray, mimeType: String): ApiResult<UploadedMedia> =
        client.upload("/uploads", bytes, mimeType).map {
            UploadedMedia(it.string("url"), it.nullableString("mimeType") ?: mimeType)
        }

    fun listGallery(): ApiResult<List<GalleryItem>> =
        client.get("/gallery")
            .map { it.arrayPayload("gallery", "items").mapObjects(ModelJson::galleryItem) }

    fun createGalleryItem(
        url: String,
        mimeType: String,
        caption: String,
        momentDate: String,
    ): ApiResult<GalleryItem> = client.post(
        "/gallery",
        JSONObject()
            .put("url", url)
            .put("mimeType", mimeType)
            .put("caption", caption)
            .put("momentDate", momentDate),
    ).map { ModelJson.galleryItem(it.objectPayload("item")) }

    fun deleteGalleryItem(itemId: String): ApiResult<Unit> =
        client.delete("/gallery/${id(itemId)}").map { Unit }

    // Conversations and messages
    fun listChatContacts(): ApiResult<List<User>> =
        client.get("/chat/contacts")
            .map { it.arrayPayload("contacts", "users", "items").mapObjects(ModelJson::user) }

    fun listConversations(): ApiResult<List<Conversation>> =
        client.get("/chat/conversations")
            .map { it.arrayPayload("chats", "conversations", "items").mapObjects(ModelJson::conversation) }

    fun createConversation(
        participantIds: List<String>,
        title: String? = null,
    ): ApiResult<Conversation> = client.post(
        "/chat/conversations",
        JSONObject().put("participantIds", JSONArray(participantIds)).apply {
            putIfNotNull(this, "title", title)
        },
    ).map { ModelJson.conversation(it.objectPayload("chat", "conversation")) }

    fun deleteConversation(conversationId: String): ApiResult<Unit> =
        client.delete("/chat/conversations/${id(conversationId)}").map { Unit }

    fun listMessages(
        conversationId: String,
        before: String? = null,
        limit: Int? = null,
    ): ApiResult<List<ChatMessage>> = client.get(
        "/chat/conversations/${id(conversationId)}/messages",
        query(before = before, limit = limit),
    ).map { it.arrayPayload("messages", "items").mapObjects(ModelJson::chatMessage) }

    fun sendMessage(conversationId: String, text: String): ApiResult<ChatMessage> =
        client.post(
            "/chat/conversations/${id(conversationId)}/messages",
            JSONObject().put("text", text),
        ).map { ModelJson.chatMessage(it.objectPayload("message")) }

    fun deleteMessage(conversationId: String, messageId: String): ApiResult<Unit> =
        client.delete("/chat/conversations/${id(conversationId)}/messages/${id(messageId)}").map { Unit }

    fun markMessagesSeen(conversationId: String, messageId: String? = null): ApiResult<Unit> =
        client.patch(
            "/chat/conversations/${id(conversationId)}/seen",
            JSONObject().apply { putIfNotNull(this, "messageId", messageId) },
        ).map { Unit }

    private fun id(value: String): String = ApiClient.pathSegment(value)

    private fun query(
        role: String? = null,
        active: Boolean? = null,
        page: Int? = null,
        limit: Int? = null,
        memberId: String? = null,
        status: String? = null,
        from: String? = null,
        to: String? = null,
        unread: Boolean? = null,
        before: String? = null,
    ): Map<String, Any?> = mapOf(
        "role" to role,
        "active" to active,
        "page" to page,
        "limit" to limit,
        "memberId" to memberId,
        "status" to status,
        "from" to from,
        "to" to to,
        "unread" to unread,
        "before" to before,
    )

    private fun dietPlanBody(
        name: String,
        description: String?,
        meals: List<String>,
        calories: Int?,
        active: Boolean,
    ) = JSONObject()
        .put("title", name)
        .put("meals", JSONArray(meals))
        .put("status", if (active) "active" else "inactive")
        .apply {
            putIfNotNull(this, "description", description)
            putIfNotNull(this, "calories", calories)
        }

    private fun paymentSettingsBody(value: PaymentSettings) = JSONObject()
        .put("currency", value.currency)
        .apply {
            putIfNotNull(this, "upiId", value.upiId)
            putIfNotNull(this, "phone", value.phone)
            putIfNotNull(this, "qrUrl", value.qrUrl)
            putIfNotNull(this, "bankName", value.bankName)
            putIfNotNull(this, "accountName", value.accountName)
            putIfNotNull(this, "accountNumber", value.accountNumber)
            putIfNotNull(this, "ifscCode", value.ifscCode)
            putIfNotNull(this, "instructions", value.instructions)
        }

    private fun contentBody(
        title: String?,
        body: String?,
        published: Boolean?,
    ) = JSONObject().apply {
        putIfNotNull(this, "title", title)
        putIfNotNull(this, "body", body)
        putIfNotNull(this, "status", published?.let { if (it) "published" else "draft" })
    }

    private fun offerBody(
        title: String?,
        description: String?,
        code: String?,
        discountPercent: Double?,
        startsAt: String?,
        endsAt: String?,
        active: Boolean?,
        imageUrl: String?,
    ) = JSONObject().apply {
        putIfNotNull(this, "title", title)
        putIfNotNull(this, "body", description)
        putIfNotNull(this, "code", code)
        putIfNotNull(this, "discountPercent", discountPercent)
        putIfNotNull(this, "startsAt", startsAt)
        putIfNotNull(this, "deadline", endsAt)
        putIfNotNull(this, "status", active?.let { if (it) "published" else "draft" })
        putIfNotNull(this, "imageUrl", imageUrl)
    }
}
