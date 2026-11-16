package com.example.devfitness.data.model

/**
 * API timestamps are kept as ISO-8601 strings so this layer has no java.time/API-level coupling.
 * Unknown server fields are intentionally ignored by the JSON mappers.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val active: Boolean = true,
    val phone: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val dailyCalories: Int? = null,
    val fitnessGoal: String? = null,
    val verificationStatus: String = "unverified",
    val verificationNote: String? = null,
    val aadhaarSubmitted: Boolean = false,
    val aadhaarNumber: String? = null,
    val aadhaarPhotoPath: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class Member(
    val id: String,
    val userId: String? = null,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val active: Boolean = true,
    val joinedAt: String? = null,
    val expiresAt: String? = null,
)

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String? = null,
    val text: String,
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedByMe: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String? = null,
    val text: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class DietPlan(
    val id: String,
    val name: String,
    val description: String? = null,
    val meals: List<String> = emptyList(),
    val calories: Int? = null,
    val active: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class DietAssignment(
    val id: String,
    val dietPlanId: String,
    val memberId: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String? = null,
    val assignedAt: String? = null,
    val plan: DietPlan? = null,
)

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val durationDays: Int,
    val active: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class MemberSubscription(
    val id: String,
    val memberId: String,
    val planId: String,
    val status: String,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class Payment(
    val id: String,
    val memberId: String,
    val amount: Double,
    val currency: String = "INR",
    val status: String,
    val method: String? = null,
    val reference: String? = null,
    val submittedAt: String? = null,
    val approvedAt: String? = null,
    val createdAt: String? = null,
)

data class MemberDue(
    val id: String,
    val memberId: String,
    val title: String,
    val amount: Double,
    val dueMonth: String,
    val dueDate: String? = null,
    val status: String,
)

data class GalleryItem(
    val id: String,
    val url: String,
    val mimeType: String,
    val caption: String,
    val momentDate: String? = null,
)

data class UploadedMedia(
    val url: String,
    val mimeType: String,
)

data class PaymentSettings(
    val currency: String = "INR",
    val upiId: String? = null,
    val phone: String? = null,
    val qrUrl: String? = null,
    val bankName: String? = null,
    val accountName: String? = null,
    val accountNumber: String? = null,
    val ifscCode: String? = null,
    val instructions: String? = null,
)

data class Notice(
    val id: String,
    val title: String,
    val body: String,
    val published: Boolean = true,
    val deadline: String? = null,
    val audience: String = "all",
    val imageUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class Blog(
    val id: String,
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val authorName: String? = null,
    val published: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class Offer(
    val id: String,
    val title: String,
    val description: String,
    val code: String? = null,
    val discountPercent: Double? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val active: Boolean = true,
    val imageUrl: String? = null,
)

data class AttendanceRecord(
    val id: String,
    val memberId: String,
    val checkInAt: String,
    val checkOutAt: String? = null,
    val status: String? = null,
)

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val type: String? = null,
    val read: Boolean = false,
    val data: Map<String, String> = emptyMap(),
    val createdAt: String? = null,
)

data class Conversation(
    val id: String,
    val participantIds: List<String> = emptyList(),
    val title: String? = null,
    val lastMessage: String? = null,
    val unreadCount: Int = 0,
    val updatedAt: String? = null,
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val seenBy: List<String> = emptyList(),
    val createdAt: String? = null,
)
