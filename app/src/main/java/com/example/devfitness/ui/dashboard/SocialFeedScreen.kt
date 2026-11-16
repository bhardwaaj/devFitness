package com.example.devfitness.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.devfitness.R
import com.example.devfitness.BuildConfig
import com.example.devfitness.data.UserRole
import com.example.devfitness.data.api.ApiClient
import com.example.devfitness.data.api.ApiResult
import com.example.devfitness.data.api.DevFitnessRepository
import com.example.devfitness.data.model.Comment
import com.example.devfitness.data.model.Offer
import com.example.devfitness.data.model.Notice
import com.example.devfitness.data.model.User
import com.example.devfitness.ui.components.AppMark
import com.example.devfitness.ui.theme.FitnessPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class FeedPost(
    val id: String,
    val author: String,
    val time: String,
    val caption: String,
    val image: Any,
    val likes: Int,
    val comments: Int,
    val category: String,
)

@Composable
fun SocialFeedScreen(
    name: String,
    role: UserRole,
    token: String,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val context = LocalContext.current
    val noticePreferences = remember {
        context.getSharedPreferences("shown_notices", android.content.Context.MODE_PRIVATE)
    }
    val liked = remember { mutableStateMapOf<String, Boolean>() }
    val apiPosts = remember { mutableStateListOf<FeedPost>() }
    val repository = remember(token) {
        DevFitnessRepository(
            ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }),
        )
    }
    val scope = rememberCoroutineScope()
    var commentsFor by remember { mutableStateOf<FeedPost?>(null) }
    var offerPopup by remember { mutableStateOf<Offer?>(null) }
    var noticePopup by remember { mutableStateOf<Notice?>(null) }
    var dashboardUser by remember { mutableStateOf<User?>(null) }
    var attendanceVisits by remember { mutableStateOf(0) }
    var mealCalories by remember { mutableStateOf<Int?>(null) }
    var feedLoaded by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(token) {
        val result = withContext(Dispatchers.IO) { repository.listPosts(limit = 30) }
        when (result) {
            is ApiResult.Success -> {
                val fallbackImages = listOf(
                    R.drawable.feed_strength,
                    R.drawable.feed_training,
                    R.drawable.feed_nutrition,
                )
                apiPosts.clear()
                apiPosts.addAll(
                    result.value.mapIndexed { index, post ->
                        FeedPost(
                            id = post.id,
                            author = post.authorName ?: "DEV Fitness Club",
                            time = post.createdAt ?: "Recently",
                            caption = post.text,
                            image = post.imageUrl ?: fallbackImages[index % fallbackImages.size],
                            likes = post.likeCount,
                            comments = post.commentCount,
                            category = "Club update",
                        ).also { liked[post.id] = post.likedByMe }
                    },
                )
                feedError = null
            }
            is ApiResult.Failure -> feedError = result.message
        }
        if (role == UserRole.Member) {
            val user = withContext(Dispatchers.IO) { repository.getCurrentUser() }
            if (user is ApiResult.Success) dashboardUser = user.value
            val attendance = withContext(Dispatchers.IO) { repository.myAttendance() }
            if (attendance is ApiResult.Success) attendanceVisits = attendance.value.size
            val diets = withContext(Dispatchers.IO) { repository.myDietHistory() }
            if (diets is ApiResult.Success) {
                mealCalories = diets.value.firstOrNull {
                    it.status.equals("active", ignoreCase = true)
                }?.plan?.calories ?: diets.value.firstOrNull()?.plan?.calories
            }
            val notices = withContext(Dispatchers.IO) { repository.listNotices() }
            if (notices is ApiResult.Success) {
                noticePopup = notices.value.firstOrNull {
                    !noticePreferences.getBoolean(it.id, false)
                }
            }
            val offers = withContext(Dispatchers.IO) { repository.listOffers(active = true) }
            if (offers is ApiResult.Success) offerPopup = offers.value.firstOrNull()
        }
        feedLoaded = true
    }

    Box(Modifier.fillMaxSize().background(FitnessPalette.Ink)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                FeedHeader(
                    name = name,
                    role = role,
                    onNotificationsClick = onNotificationsClick,
                    onProfileClick = onProfileClick,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                )
            }
            if (role == UserRole.Member) {
                item {
                    MemberDashboardWidgets(
                        user = dashboardUser,
                        attendanceVisits = attendanceVisits,
                        mealCalories = mealCalories,
                        onProfileClick = onProfileClick,
                    )
                }
            }
            item { FeedTopics() }
            if (feedLoaded && apiPosts.isEmpty()) {
                item {
                    EmptyFeedState(
                        message = feedError ?: if (role == UserRole.Admin) {
                            "No posts yet. Create the first post from the + tab."
                        } else {
                            "No club posts have been published yet."
                        },
                    )
                }
            }
            items(apiPosts, key = { it.id }) { post ->
                FeedPostCard(
                    post = post,
                    liked = liked[post.id] == true,
                    onLike = {
                        val willLike = !(liked[post.id] ?: false)
                        liked[post.id] = willLike
                        if (post.id.length == 24) {
                            scope.launch(Dispatchers.IO) {
                                if (willLike) repository.likePost(post.id) else repository.unlikePost(post.id)
                            }
                        }
                    },
                    onComments = { commentsFor = post },
                )
            }
        }
    }

    commentsFor?.let { post ->
        CommentsDialog(post = post, repository = repository, onDismiss = { commentsFor = null })
    }
    noticePopup?.let { notice ->
        val dismissNotice = {
            noticePreferences.edit().putBoolean(notice.id, true).apply()
            noticePopup = null
        }
        AlertDialog(
            onDismissRequest = dismissNotice,
            containerColor = FitnessPalette.InkRaised,
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(26.dp),
            icon = { AppMark(Modifier.size(38.dp), animated = true) },
            title = {
                Text(notice.title, color = FitnessPalette.Label, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    notice.imageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = notice.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(16.dp)),
                        )
                    }
                    Text(notice.body, color = FitnessPalette.Secondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = dismissNotice) { Text("Got it", color = FitnessPalette.Lime) }
            },
        )
    }
    if (noticePopup == null) offerPopup?.let { offer ->
        AlertDialog(
            onDismissRequest = { offerPopup = null },
            containerColor = FitnessPalette.InkRaised,
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(26.dp),
            icon = { AppMark(Modifier.size(38.dp), animated = true) },
            title = {
                Text(
                    offer.title,
                    color = FitnessPalette.Label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    offer.imageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = offer.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(16.dp)),
                        )
                    }
                    Text(
                        offer.description,
                        color = FitnessPalette.Secondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { offerPopup = null }) {
                    Text("Got it", color = FitnessPalette.Lime)
                }
            },
        )
    }
}

@Composable
private fun EmptyFeedState(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppMark(Modifier.size(78.dp))
        Spacer(Modifier.height(18.dp))
        Text("Your feed is ready", color = FitnessPalette.Label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(
            message,
            color = FitnessPalette.Secondary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun FeedHeader(
    name: String,
    role: UserRole,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark(Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "DEV Fitness",
                color = FitnessPalette.Label,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
            Text(
                if (role == UserRole.Admin) "Admin feed" else "Ready to train, ${name.substringBefore(' ')}?",
                color = FitnessPalette.Secondary,
                fontSize = 12.sp,
            )
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(FitnessPalette.InkRaised)
                .clickable(onClick = onNotificationsClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.NotificationsNone, "Notifications", tint = FitnessPalette.Label)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(FitnessPalette.Danger),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(FitnessPalette.Lime)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Person, "Profile", tint = FitnessPalette.OnLime)
        }
    }
}

@Composable
private fun MemberDashboardWidgets(
    user: User?,
    attendanceVisits: Int,
    mealCalories: Int?,
    onProfileClick: () -> Unit,
) {
    val widgets = listOf(
        Triple("Attendance", "$attendanceVisits", "visits"),
        Triple("Meal calories", "${mealCalories ?: user?.dailyCalories ?: "—"}", "kcal"),
        Triple("Weight", user?.weightKg?.let { "${it.toInt()}" } ?: "—", "kg"),
        Triple("Age", user?.age?.toString() ?: "—", "years"),
        Triple(
            "Verification",
            if (user?.verificationStatus == "approved") "Verified" else "Pending",
            user?.verificationStatus?.replaceFirstChar(Char::uppercase) ?: "Unverified",
        ),
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Your dashboard",
                color = FitnessPalette.Label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Edit profile",
                color = FitnessPalette.Lime,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onProfileClick),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(widgets) { (title, value, unit) ->
                Column(
                    Modifier
                        .width(126.dp)
                        .height(92.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (title == "Verification" && user?.verificationStatus == "approved") {
                                FitnessPalette.Lime.copy(alpha = 0.14f)
                            } else {
                                FitnessPalette.InkRaised
                            },
                        )
                        .clickable(onClick = onProfileClick)
                        .padding(13.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(title, color = FitnessPalette.Secondary, fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(value, color = FitnessPalette.Label, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Text(unit, color = FitnessPalette.Lime, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedTopics() {
    val topics = listOf("All", "Strength", "Cardio", "Nutrition", "Offers")
    var selected by remember { mutableStateOf("All") }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(topics) { topic ->
            val active = topic == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) FitnessPalette.Lime else FitnessPalette.InkRaised)
                    .clickable { selected = topic }
                    .padding(horizontal = 17.dp, vertical = 9.dp),
            ) {
                Text(
                    topic,
                    color = if (active) FitnessPalette.OnLime else FitnessPalette.Secondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun FeedPostCard(
    post: FeedPost,
    liked: Boolean,
    onLike: () -> Unit,
    onComments: () -> Unit,
) {
    val heartScale by animateFloatAsState(if (liked) 1.12f else 1f, label = "heart")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(FitnessPalette.InkRaised),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMark(Modifier.size(42.dp))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(post.author, color = FitnessPalette.Label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(post.time, color = FitnessPalette.Tertiary, fontSize = 11.sp)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Rounded.MoreHoriz, "Post options", tint = FitnessPalette.Secondary)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.9f),
        ) {
            AsyncImage(
                model = post.image,
                contentDescription = post.category,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                        ),
                    ),
            )
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(post.category, color = FitnessPalette.Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                post.caption,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedAction(
                icon = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = "${post.likes + if (liked) 1 else 0}",
                tint = if (liked) FitnessPalette.Lime else FitnessPalette.Secondary,
                modifier = Modifier.scale(heartScale),
                onClick = onLike,
            )
            FeedAction(Icons.Rounded.ChatBubbleOutline, "${post.comments}", onClick = onComments)
            FeedAction(Icons.Rounded.Send, "Share", onClick = {})
            Spacer(Modifier.weight(1f))
            Text("View post", color = FitnessPalette.Lime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FeedAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = FitnessPalette.Secondary,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = tint, fontSize = 12.sp)
    }
}

@Composable
private fun CommentsDialog(
    post: FeedPost,
    repository: DevFitnessRepository,
    onDismiss: () -> Unit,
) {
    val comments = remember(post.id) { mutableStateListOf<Comment>() }
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Loading comments…") }
    LaunchedEffect(post.id) {
        when (val result = withContext(Dispatchers.IO) { repository.listComments(post.id) }) {
            is ApiResult.Success -> {
                comments.clear()
                comments.addAll(result.value)
                message = if (comments.isEmpty()) "No comments yet." else ""
            }
            is ApiResult.Failure -> message = result.message
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitnessPalette.InkRaised,
        titleContentColor = FitnessPalette.Label,
        textContentColor = FitnessPalette.Secondary,
        title = { Text("Comments", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                comments.forEach { comment ->
                    Column {
                        Text(comment.authorName ?: "Member", color = FitnessPalette.Lime, fontSize = 11.sp)
                        Text(comment.text, color = FitnessPalette.Label)
                    }
                }
                if (message.isNotBlank()) {
                    Text(message, color = FitnessPalette.Tertiary, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Add comment") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = draft.isNotBlank(),
                onClick = {
                    val text = draft.trim()
                    draft = ""
                    scope.launch {
                        when (val result = withContext(Dispatchers.IO) { repository.addComment(post.id, text) }) {
                            is ApiResult.Success -> {
                                comments.add(result.value)
                                message = ""
                            }
                            is ApiResult.Failure -> {
                                draft = text
                                message = result.message
                            }
                        }
                    }
                },
            ) { Text("Post", color = FitnessPalette.Lime) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = FitnessPalette.Secondary) } },
    )
}
