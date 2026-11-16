package com.example.devfitness.data.api

import com.example.devfitness.data.model.*
import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.string(vararg keys: String): String {
    for (key in keys) if (has(key) && !isNull(key)) return optString(key)
    return ""
}

internal fun JSONObject.nullableString(vararg keys: String): String? =
    string(*keys).takeIf { it.isNotBlank() }

internal fun JSONObject.intOrNull(vararg keys: String): Int? {
    for (key in keys) if (has(key) && !isNull(key)) return optInt(key)
    return null
}

internal fun JSONObject.doubleOrNull(vararg keys: String): Double? {
    for (key in keys) if (has(key) && !isNull(key)) return optDouble(key)
    return null
}

internal fun JSONObject.boolean(default: Boolean, vararg keys: String): Boolean {
    for (key in keys) if (has(key) && !isNull(key)) return optBoolean(key, default)
    return default
}

internal fun JSONObject.activeStatus(default: Boolean = true): Boolean {
    nullableString("status")?.let { return it != "inactive" }
    return boolean(default, "active", "isActive")
}

internal fun JSONObject.stringList(vararg keys: String): List<String> {
    for (key in keys) {
        val array = optJSONArray(key) ?: continue
        return (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }
    return emptyList()
}

internal fun JSONObject.stringMap(key: String): Map<String, String> {
    val value = optJSONObject(key) ?: return emptyMap()
    val result = linkedMapOf<String, String>()
    val keys = value.keys()
    while (keys.hasNext()) {
        val itemKey = keys.next()
        if (!value.isNull(itemKey)) result[itemKey] = value.optString(itemKey)
    }
    return result
}

/** Accepts direct objects and common `{ data: ... }` / named-resource envelopes. */
internal fun JSONObject.objectPayload(vararg names: String): JSONObject {
    names.forEach { optJSONObject(it)?.let { value -> return value } }
    optJSONObject("data")?.let { data ->
        names.forEach { data.optJSONObject(it)?.let { value -> return value } }
        return data
    }
    return this
}

/** Accepts direct arrays (wrapped by ApiClient), `{ data: [] }`, and common named lists. */
internal fun JSONObject.arrayPayload(vararg names: String): JSONArray {
    names.forEach { optJSONArray(it)?.let { value -> return value } }
    val data = opt("data")
    if (data is JSONArray) return data
    if (data is JSONObject) names.forEach { data.optJSONArray(it)?.let { value -> return value } }
    optJSONArray(ApiClient.ARRAY_WRAPPER)?.let { return it }
    return JSONArray()
}

internal inline fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> =
    (0 until length()).mapNotNull { optJSONObject(it)?.let(mapper) }

internal fun putIfNotNull(json: JSONObject, key: String, value: Any?) {
    if (value != null) json.put(key, value)
}

internal object ModelJson {
    fun user(j: JSONObject) = User(
        id = j.string("id", "_id"),
        name = j.string("name"),
        email = j.string("email"),
        role = j.string("role"),
        active = j.activeStatus(),
        phone = j.nullableString("phone"),
        age = j.intOrNull("age"),
        heightCm = j.doubleOrNull("heightCm"),
        weightKg = j.doubleOrNull("weightKg"),
        dailyCalories = j.intOrNull("dailyCalories"),
        fitnessGoal = j.nullableString("fitnessGoal"),
        verificationStatus = j.nullableString("verificationStatus") ?: "unverified",
        verificationNote = j.nullableString("verificationNote"),
        aadhaarSubmitted = j.boolean(false, "aadhaarSubmitted"),
        aadhaarNumber = j.nullableString("aadhaarNumber"),
        aadhaarPhotoPath = j.nullableString("aadhaarPhotoPath"),
        createdAt = j.nullableString("createdAt", "created_at"),
        updatedAt = j.nullableString("updatedAt", "updated_at"),
    )

    fun member(j: JSONObject) = Member(
        id = j.string("id", "_id"),
        userId = j.nullableString("userId", "user_id"),
        name = j.string("name"),
        email = j.nullableString("email"),
        phone = j.nullableString("phone"),
        active = j.activeStatus(),
        joinedAt = j.nullableString("joinedAt", "joined_at"),
        expiresAt = j.nullableString("expiresAt", "expires_at"),
    )

    fun post(j: JSONObject) = Post(
        id = j.string("id", "_id"),
        authorId = j.string("authorId", "author_id", "userId"),
        authorName = j.nullableString("authorName", "author_name"),
        text = j.string("text", "body", "content"),
        imageUrl = j.nullableString("imageUrl", "image_url", "mediaUrl"),
        likeCount = j.intOrNull("likeCount", "likesCount", "like_count") ?: 0,
        commentCount = j.intOrNull("commentCount", "commentsCount", "comment_count") ?: 0,
        likedByMe = j.boolean(false, "likedByMe", "liked"),
        createdAt = j.nullableString("createdAt", "created_at"),
        updatedAt = j.nullableString("updatedAt", "updated_at"),
    )

    fun comment(j: JSONObject) = Comment(
        id = j.string("id", "_id"),
        postId = j.string("postId", "post_id"),
        authorId = j.string("authorId", "author_id", "userId"),
        authorName = j.nullableString("authorName", "author_name"),
        text = j.string("text", "body", "content"),
        createdAt = j.nullableString("createdAt", "created_at"),
        updatedAt = j.nullableString("updatedAt", "updated_at"),
    )

    fun dietPlan(j: JSONObject) = DietPlan(
        id = j.string("id", "_id"),
        name = j.string("name", "title"),
        description = j.nullableString("description"),
        meals = j.stringList("meals", "items"),
        calories = j.intOrNull("calories"),
        active = j.boolean(true, "active", "isActive"),
        createdAt = j.nullableString("createdAt", "created_at"),
        updatedAt = j.nullableString("updatedAt", "updated_at"),
    )

    fun dietAssignment(j: JSONObject) = DietAssignment(
        id = j.string("id", "_id"),
        dietPlanId = j.string("dietPlanId", "diet_plan_id", "planId"),
        memberId = j.string("memberId", "member_id", "userId"),
        startDate = j.nullableString("startDate", "start_date"),
        endDate = j.nullableString("endDate", "end_date"),
        status = j.nullableString("status") ?: if (j.has("active")) {
            if (j.boolean(false, "active")) "active" else "inactive"
        } else null,
        assignedAt = j.nullableString("assignedAt", "assigned_at", "createdAt"),
        plan = j.optJSONObject("plan")?.let(::dietPlan),
    )

    fun subscriptionPlan(j: JSONObject) = SubscriptionPlan(
        id = j.string("id", "_id"),
        name = j.string("name", "title"),
        description = j.nullableString("description"),
        price = j.doubleOrNull("price", "amount") ?: 0.0,
        durationDays = j.intOrNull("durationDays", "duration_days", "duration") ?: 0,
        active = j.boolean(true, "active", "isActive"),
        createdAt = j.nullableString("createdAt", "created_at"),
        updatedAt = j.nullableString("updatedAt", "updated_at"),
    )

    fun memberSubscription(j: JSONObject) = MemberSubscription(
        id = j.string("id", "_id"),
        memberId = j.string("memberId", "member_id", "userId"),
        planId = j.string("planId", "plan_id", "subscriptionPlanId"),
        status = j.string("status"),
        startsAt = j.nullableString("startsAt", "starts_at", "startDate"),
        endsAt = j.nullableString("endsAt", "ends_at", "endDate"),
        createdAt = j.nullableString("createdAt", "created_at"),
        updatedAt = j.nullableString("updatedAt", "updated_at"),
    )

    fun payment(j: JSONObject) = Payment(
        id = j.string("id", "_id"),
        memberId = j.string("memberId", "member_id", "userId"),
        amount = j.doubleOrNull("amount") ?: 0.0,
        currency = j.nullableString("currency") ?: "INR",
        status = j.string("status"),
        method = j.nullableString("method"),
        reference = j.nullableString("reference", "transactionId", "transaction_id", "transactionRef"),
        submittedAt = j.nullableString("submittedAt", "submitted_at"),
        approvedAt = j.nullableString("approvedAt", "approved_at"),
        createdAt = j.nullableString("createdAt", "created_at"),
    )

    fun memberDue(j: JSONObject) = MemberDue(
        id = j.string("id", "_id"),
        memberId = j.string("memberId", "userId"),
        title = j.string("title"),
        amount = j.doubleOrNull("amount") ?: 0.0,
        dueMonth = j.string("dueMonth"),
        dueDate = j.nullableString("dueDate"),
        status = j.string("status"),
    )

    fun galleryItem(j: JSONObject) = GalleryItem(
        id = j.string("id", "_id"),
        url = j.string("url"),
        mimeType = j.nullableString("mimeType") ?: "image/jpeg",
        caption = j.nullableString("caption").orEmpty(),
        momentDate = j.nullableString("momentDate", "createdAt"),
    )

    fun paymentSettings(j: JSONObject) = PaymentSettings(
        currency = j.nullableString("currency") ?: "INR",
        upiId = j.nullableString("upiId", "upi_id"),
        phone = j.nullableString("phone"),
        qrUrl = j.nullableString("qrUrl", "qr_url"),
        bankName = j.nullableString("bankName", "bank_name"),
        accountName = j.nullableString("accountName", "account_name"),
        accountNumber = j.nullableString("accountNumber", "account_number"),
        ifscCode = j.nullableString("ifscCode", "ifsc_code"),
        instructions = j.nullableString("instructions"),
    )

    fun notice(j: JSONObject) = Notice(
        id = j.string("id", "_id"),
        title = j.string("title"),
        body = j.string("body", "content", "description"),
        published = j.nullableString("status") != "draft",
        deadline = j.nullableString("deadline"),
        audience = j.nullableString("audience") ?: "all",
        imageUrl = j.nullableString("imageUrl"),
        createdAt = j.nullableString("createdAt", "created_at"),
        updatedAt = j.nullableString("updatedAt", "updated_at"),
    )

    fun blog(j: JSONObject) = Blog(
        id = j.string("id", "_id"),
        title = j.string("title"),
        body = j.string("body", "content"),
        imageUrl = j.nullableString("imageUrl", "image_url"),
        authorName = j.nullableString("authorName", "author_name"),
        published = j.nullableString("status") != "draft",
        createdAt = j.nullableString("createdAt", "created_at"),
        updatedAt = j.nullableString("updatedAt", "updated_at"),
    )

    fun offer(j: JSONObject) = Offer(
        id = j.string("id", "_id"),
        title = j.string("title"),
        description = j.string("description", "body", "content"),
        code = j.nullableString("code"),
        discountPercent = j.doubleOrNull("discountPercent", "discount_percent", "discount"),
        startsAt = j.nullableString("startsAt", "starts_at"),
        endsAt = j.nullableString("endsAt", "ends_at", "deadline"),
        active = j.nullableString("status") != "draft",
        imageUrl = j.nullableString("imageUrl"),
    )

    fun attendance(j: JSONObject) = AttendanceRecord(
        id = j.string("id", "_id"),
        memberId = j.string("memberId", "member_id", "userId"),
        checkInAt = j.string("checkInAt", "check_in_at", "checkinAt"),
        checkOutAt = j.nullableString("checkOutAt", "check_out_at", "checkoutAt"),
        status = j.nullableString("status"),
    )

    fun notification(j: JSONObject) = NotificationItem(
        id = j.string("id", "_id"),
        title = j.string("title"),
        body = j.string("body", "message"),
        type = j.nullableString("type"),
        read = j.boolean(false, "read", "isRead") || j.has("readAt") && !j.isNull("readAt"),
        data = j.stringMap("data"),
        createdAt = j.nullableString("createdAt", "created_at"),
    )

    fun conversation(j: JSONObject) = Conversation(
        id = j.string("id", "_id"),
        participantIds = j.stringList("participantIds", "participant_ids", "participants"),
        title = j.nullableString("title", "name"),
        lastMessage = j.nullableString("lastMessage", "last_message"),
        unreadCount = j.intOrNull("unreadCount", "unread_count") ?: 0,
        updatedAt = j.nullableString("updatedAt", "updated_at"),
    )

    fun chatMessage(j: JSONObject) = ChatMessage(
        id = j.string("id", "_id"),
        conversationId = j.string("conversationId", "conversation_id", "chatId"),
        senderId = j.string("senderId", "sender_id", "userId"),
        text = j.string("text", "body", "message"),
        seenBy = j.stringList("seenBy", "seen_by"),
        createdAt = j.nullableString("createdAt", "created_at"),
    )
}
