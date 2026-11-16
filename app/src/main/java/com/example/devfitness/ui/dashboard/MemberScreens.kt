package com.example.devfitness.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import com.example.devfitness.data.UserRole
import com.example.devfitness.BuildConfig
import com.example.devfitness.data.api.ApiClient
import com.example.devfitness.data.api.ApiResult
import com.example.devfitness.data.api.DevFitnessRepository
import com.example.devfitness.data.api.RealtimeChatClient
import com.example.devfitness.data.model.ChatMessage
import com.example.devfitness.data.model.Conversation
import com.example.devfitness.data.model.User
import com.example.devfitness.data.model.AttendanceRecord
import com.example.devfitness.data.model.DietAssignment
import com.example.devfitness.data.model.DietPlan
import com.example.devfitness.data.model.MemberSubscription
import com.example.devfitness.data.model.MemberDue
import com.example.devfitness.data.model.NotificationItem
import com.example.devfitness.data.model.Payment
import com.example.devfitness.data.model.SubscriptionPlan
import com.example.devfitness.ui.components.AppMark
import com.example.devfitness.ui.components.IosBackButton
import com.example.devfitness.ui.components.IosButton
import com.example.devfitness.ui.theme.FitnessPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val GYM_LATITUDE = 26.882172
private const val GYM_LONGITUDE = 75.904685
private const val CHECK_IN_RADIUS_METRES = 50f

@Composable
fun DietPlanScreen(token: String) {
    val repository = remember(token) {
        DevFitnessRepository(ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }))
    }
    val assignments = remember { mutableStateListOf<DietAssignment>() }
    val availablePlans = remember { mutableStateListOf<DietPlan>() }
    var message by remember { mutableStateOf("Loading your diet plans…") }
    LaunchedEffect(token) {
        val history = withContext(Dispatchers.IO) { repository.myDietHistory() }
        val plans = withContext(Dispatchers.IO) { repository.listDietPlans(active = true) }
        when (history) {
            is ApiResult.Success -> {
                assignments.clear()
                assignments.addAll(history.value)
            }
            is ApiResult.Failure -> message = history.message
        }
        when (plans) {
            is ApiResult.Success -> {
                availablePlans.clear()
                availablePlans.addAll(plans.value)
                if (assignments.isEmpty() && availablePlans.isEmpty()) {
                    message = "No diet plan has been assigned yet."
                } else {
                    message = ""
                }
            }
            is ApiResult.Failure -> if (assignments.isEmpty()) message = plans.message
        }
    }
    val currentAssignment =
        assignments.firstOrNull { it.status.equals("active", true) } ?: assignments.firstOrNull()
    val currentPlan = currentAssignment?.plan ?: availablePlans.firstOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FitnessPalette.Ink),
        contentPadding = PaddingValues(bottom = 30.dp),
    ) {
        item {
            MemberPageHeader(
                "My diet plan",
                currentPlan?.name ?: "Your assigned nutrition plan",
                Icons.Rounded.Restaurant,
            )
        }
        currentPlan?.let { plan ->
            item {
                HighlightCard(
                    title = plan.name,
                    value = plan.calories?.let { "$it kcal" } ?: "Active plan",
                    detail = plan.description.orEmpty(),
                )
            }
            items(plan.meals) { meal ->
                TimelineRow(time = "Meal", title = meal, detail = plan.description.orEmpty())
            }
        }
        if (message.isNotBlank()) item { DataMessage(message) }
        if (assignments.isNotEmpty()) {
            item { SectionTitle("Assignment history") }
            items(assignments, key = { it.id }) { assignment ->
                HistoryCard(
                    assignment.plan?.name ?: "Diet plan",
                    assignment.assignedAt ?: assignment.startDate.orEmpty(),
                    assignment.status?.uppercase() ?: if (assignment === currentAssignment) "CURRENT" else "PAST",
                )
            }
        }
    }
}

@Composable
fun AttendanceCheckScreen(token: String) {
    val context = LocalContext.current
    val repository = remember(token) {
        DevFitnessRepository(
            ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }),
        )
    }
    val scope = rememberCoroutineScope()
    var location by remember { mutableStateOf<Location?>(null) }
    var message by remember { mutableStateOf("Location is required to verify the 50 m gym radius.") }
    var checkedIn by remember { mutableStateOf(false) }
    var checkTime by remember { mutableStateOf<String?>(null) }
    val records = remember { mutableStateListOf<AttendanceRecord>() }
    LaunchedEffect(token) {
        when (val result = withContext(Dispatchers.IO) { repository.myAttendance() }) {
            is ApiResult.Success -> {
                records.clear()
                records.addAll(result.value)
                val open = records.firstOrNull { it.checkOutAt == null }
                checkedIn = open != null
                checkTime = open?.checkInAt
            }
            is ApiResult.Failure -> message = result.message
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            location = latestLocation(context)
            message = if (location == null) "No location fix yet. Turn on GPS and refresh." else "Location updated."
        } else {
            message = "Location permission was denied. Check-in stays unavailable."
        }
    }
    val distance = remember(location) {
        location?.let {
            val result = FloatArray(1)
            Location.distanceBetween(it.latitude, it.longitude, GYM_LATITUDE, GYM_LONGITUDE, result)
            result[0]
        }
    }
    val inRange = distance != null && distance <= CHECK_IN_RADIUS_METRES &&
        location?.let { !LocationCompat.isMock(it) } == true

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FitnessPalette.Ink),
        contentPadding = PaddingValues(bottom = 30.dp),
    ) {
        item { MemberPageHeader("Gym attendance", "Location-verified check-in", Icons.Rounded.LocationOn) }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                FitnessPalette.Lime.copy(alpha = 0.20f),
                                FitnessPalette.InkRaised,
                            ),
                        ),
                    )
                    .padding(24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.size(92.dp).clip(CircleShape).background(FitnessPalette.Lime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, tint = FitnessPalette.OnLime, modifier = Modifier.size(46.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        when {
                            location?.let(LocationCompat::isMock) == true -> "Mock location rejected"
                            distance == null -> "Location not verified"
                            inRange -> "You’re at the gym"
                            else -> "${distance.roundToInt()} m away"
                        },
                        color = FitnessPalette.Label,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (inRange) "Inside the permitted 50 m radius" else message,
                        color = FitnessPalette.Secondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))
                    IosButton(
                        text = if (checkedIn) "Check out" else "Check in",
                        enabled = inRange,
                        onClick = {
                            val current = location ?: return@IosButton
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    if (checkedIn) {
                                        repository.checkOut(current.latitude, current.longitude)
                                    } else {
                                        repository.checkIn(current.latitude, current.longitude)
                                    }
                                }
                                when (result) {
                                    is ApiResult.Success -> {
                                        val record = result.value
                                        val existing = records.indexOfFirst { it.id == record.id }
                                        if (existing >= 0) records[existing] = record else records.add(0, record)
                                        checkedIn = record.checkOutAt == null
                                        checkTime = if (checkedIn) record.checkInAt else record.checkOutAt
                                        message = if (checkedIn) "Check-in recorded." else "Check-out recorded."
                                    }
                                    is ApiResult.Failure -> message = result.message
                                }
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                location = latestLocation(context)
                                message = if (location == null) "Waiting for a GPS fix." else "Location updated."
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Refresh, null, tint = FitnessPalette.Lime)
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh location", color = FitnessPalette.Lime)
                    }
                }
            }
        }
        if (checkTime != null) {
            item {
                HighlightCard(
                    title = if (checkedIn) "Checked in" else "Checked out",
                    value = checkTime.orEmpty(),
                    detail = "Today · DEV Fitness Club",
                )
            }
        }
        item { SectionTitle("Recent attendance") }
        items(records, key = { it.id }) { record ->
            TimelineRow(
                record.checkInAt,
                if (record.checkOutAt == null) "Checked in" else "Completed session",
                record.checkOutAt ?: "Currently at gym",
            )
        }
    }
}

@Composable
fun ChatListScreen(token: String, role: UserRole) {
    val repository = remember(token) {
        DevFitnessRepository(
            ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }),
        )
    }
    val chats = remember { mutableStateListOf<Conversation>() }
    val availableMembers = remember { mutableStateListOf<User>() }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Conversation?>(null) }
    var creating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Loading conversations…") }
    LaunchedEffect(token) {
        when (val result = withContext(Dispatchers.IO) { repository.listConversations() }) {
            is ApiResult.Success -> {
                chats.clear()
                chats.addAll(result.value)
                message = if (chats.isEmpty()) "No conversations yet." else ""
            }
            is ApiResult.Failure -> message = result.message
        }
    }
    if (selected != null) {
        ConversationScreen(
            conversation = selected!!,
            repository = repository,
            token = token,
            onBack = { selected = null },
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FitnessPalette.Ink),
        contentPadding = PaddingValues(bottom = 26.dp),
    ) {
        item { MemberPageHeader("Chats", "Messages from your club", Icons.Rounded.ChatBubble) }
        item {
            TextButton(onClick = { creating = true }, modifier = Modifier.padding(horizontal = 14.dp)) {
                Text(
                    if (role == UserRole.Admin) "Start member conversation" else "Contact gym admin",
                    color = FitnessPalette.Lime,
                )
            }
        }
        item { SearchField(query = "", onQueryChange = {}, placeholder = "Search conversations") }
        items(chats, key = { it.id }) { chat ->
            val name = chat.title?.takeIf { it.isNotBlank() } ?: "Club conversation"
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { selected = chat }
                    .padding(horizontal = 20.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(FitnessPalette.Lime),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(name.take(1), color = FitnessPalette.OnLime, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = if (chat.unreadCount > 0) FitnessPalette.Lime else FitnessPalette.Label,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    )
                    Text(
                        chat.lastMessage.orEmpty(),
                        color = if (chat.unreadCount > 0) FitnessPalette.Label else FitnessPalette.Secondary,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(chat.updatedAt.orEmpty(), color = FitnessPalette.Tertiary, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    if (chat.unreadCount > 0) {
                        Box(
                            Modifier.clip(CircleShape).background(FitnessPalette.Lime)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(chat.unreadCount.toString(), color = FitnessPalette.OnLime, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (message.isNotBlank()) {
            item {
                Text(
                    message,
                    color = FitnessPalette.Secondary,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                )
            }
        }
    }
    if (creating) {
        LaunchedEffect(Unit) {
            when (val result = withContext(Dispatchers.IO) { repository.listChatContacts() }) {
                is ApiResult.Success -> {
                    availableMembers.clear()
                    availableMembers.addAll(result.value)
                }
                is ApiResult.Failure -> message = result.message
            }
        }
        AlertDialog(
            onDismissRequest = { creating = false },
            containerColor = FitnessPalette.InkRaised,
            title = { Text("Start conversation", color = FitnessPalette.Label) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableMembers, key = { it.id }) { member ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    scope.launch {
                                        when (val result = withContext(Dispatchers.IO) {
                                            repository.createConversation(listOf(member.id), member.name)
                                        }) {
                                            is ApiResult.Success -> {
                                                if (chats.none { it.id == result.value.id }) chats.add(0, result.value)
                                                selected = result.value
                                                creating = false
                                            }
                                            is ApiResult.Failure -> message = result.message
                                        }
                                    }
                                }
                                .background(FitnessPalette.Ink).padding(12.dp),
                        ) {
                            Text(member.name, color = FitnessPalette.Label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { creating = false }) { Text("Close", color = FitnessPalette.Lime) } },
        )
    }
}

@Composable
fun ProfileScreen(
    name: String,
    email: String,
    role: UserRole,
    token: String,
    openNotifications: Boolean = false,
    onLogout: () -> Unit,
) {
    var page by remember(openNotifications) {
        mutableStateOf(if (openNotifications) ProfilePage.Notifications else null)
    }
    var adminOverview by remember { mutableStateOf("Loading club data…") }
    var memberPlanName by remember { mutableStateOf("Loading subscription…") }
    var memberPlanDetail by remember { mutableStateOf("") }
    val repository = remember(token) {
        DevFitnessRepository(ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }))
    }
    LaunchedEffect(token, role) {
        if (role == UserRole.Admin) {
            val users = withContext(Dispatchers.IO) { repository.listUsers(role = "member") }
            val payments = withContext(Dispatchers.IO) { repository.listPayments() }
            if (users is ApiResult.Success && payments is ApiResult.Success) {
                val active = users.value.count { it.active }
                val paid = payments.value.filter { it.status.equals("approved", true) }
                    .map { it.memberId }.distinct().size
                adminOverview = "$active active members · $paid with approved payments"
            } else {
                adminOverview = "Club data unavailable"
            }
        } else {
            val subscriptions = withContext(Dispatchers.IO) { repository.mySubscriptions() }
            val plans = withContext(Dispatchers.IO) { repository.listSubscriptionPlans() }
            if (subscriptions is ApiResult.Success && plans is ApiResult.Success) {
                val current = subscriptions.value.firstOrNull {
                    it.status.equals("active", ignoreCase = true)
                } ?: subscriptions.value.firstOrNull()
                val plan = plans.value.firstOrNull { it.id == current?.planId }
                memberPlanName = plan?.name ?: if (current == null) "No active subscription" else "Subscription"
                memberPlanDetail = current?.let {
                    "${it.status.replaceFirstChar(Char::uppercase)} · Expires ${it.endsAt.orEmpty()}"
                }.orEmpty()
            } else {
                memberPlanName = "Subscription unavailable"
            }
        }
    }
    if (page != null) {
        ProfileDetailPage(page = page!!, token = token, role = role, onBack = { page = null })
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FitnessPalette.Ink),
        contentPadding = PaddingValues(bottom = 30.dp),
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppMark(Modifier.size(92.dp))
                Spacer(Modifier.height(14.dp))
                Text(name, color = FitnessPalette.Label, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(email, color = FitnessPalette.Secondary, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(FitnessPalette.Lime.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (role == UserRole.Admin) "CLUB ADMIN" else "ACTIVE MEMBER",
                        color = FitnessPalette.Lime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (role == UserRole.Member) {
            item {
                HighlightCard("Current subscription", memberPlanName, memberPlanDetail)
            }
            items(ProfilePage.entries.filter { it.memberVisible }) { item ->
                ActionListItem(item.icon, item.title, item.subtitle) { page = item }
            }
        } else {
            item {
                HighlightCard("Club overview", adminOverview, "Live data from DEV Fitness")
            }
            items(ProfilePage.entries.filter { !it.memberOnly }) { item ->
                ActionListItem(item.icon, item.title, item.subtitle) { page = item }
            }
        }
        item {
            ActionListItem(Icons.Rounded.Logout, "Log out", "End this session", danger = true, onClick = onLogout)
        }
    }
}

private enum class ProfilePage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val memberVisible: Boolean = true,
    val memberOnly: Boolean = false,
) {
    EditProfile("Profile details", "Photo, phone and password", Icons.Rounded.Person),
    Subscription("Subscription plan", "Plan, expiry and renewal", Icons.Rounded.Subscriptions, memberOnly = true),
    Renew("Renew with PhonePe", "Direct UPI payment", Icons.Rounded.Payment, memberOnly = true),
    Transactions("Transaction history", "Pending and verified payments", Icons.Rounded.CreditCard),
    Attendance("Attendance sheet", "Monthly check-in history", Icons.Rounded.CalendarMonth),
    Notifications("Notifications", "Club updates and alerts", Icons.Rounded.Notifications),
}

@Composable
private fun ProfileDetailPage(page: ProfilePage, token: String, role: UserRole, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(token) {
        DevFitnessRepository(
            ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }),
        )
    }
    val scope = rememberCoroutineScope()
    var paymentUpiId by remember { mutableStateOf("") }
    var paymentPhone by remember { mutableStateOf("") }
    var paymentQrUrl by remember { mutableStateOf("") }
    val payments = remember { mutableStateListOf<Payment>() }
    val attendance = remember { mutableStateListOf<AttendanceRecord>() }
    val notifications = remember { mutableStateListOf<NotificationItem>() }
    val subscriptions = remember { mutableStateListOf<MemberSubscription>() }
    val memberDues = remember { mutableStateListOf<MemberDue>() }
    val subscriptionPlans = remember { mutableStateListOf<SubscriptionPlan>() }
    var dataMessage by remember { mutableStateOf("") }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }
    var editHeight by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("") }
    var editCalories by remember { mutableStateOf("") }
    var editGoal by remember { mutableStateOf("") }
    var editAadhaar by remember { mutableStateOf("") }
    var aadhaarConsent by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }
    var paymentReference by remember { mutableStateOf("") }
    val phonePeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val response = result.data?.getStringExtra("response")
            ?: result.data?.data?.encodedQuery
            ?: ""
        val fields = response.split("&")
            .mapNotNull { value ->
                val parts = value.split("=", limit = 2)
                if (parts.size == 2) parts[0].lowercase() to Uri.decode(parts[1]) else null
            }
            .toMap()
        val reference = fields["txnid"] ?: fields["approvalrefno"] ?: fields["txnref"]
        if (!reference.isNullOrBlank()) paymentReference = reference
        dataMessage = when (fields["status"]?.lowercase()) {
            "success" -> "PhonePe reported success. Submit the transaction reference for verification."
            "failure", "failed" -> "PhonePe reported that the payment failed."
            else -> if (response.isBlank()) "Payment was not completed." else "Payment response received. Verify the reference before submitting."
        }
    }
    val aadhaarPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                dataMessage = "Uploading Aadhaar photo securely…"
                val result = withContext(Dispatchers.IO) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext ApiResult.Failure(message = "Could not read selected photo")
                    repository.uploadAadhaarPhoto(bytes, mimeType)
                }
                when (result) {
                    is ApiResult.Success -> {
                        currentUser = result.value
                        dataMessage = "Aadhaar photo uploaded. Submit all details for admin review."
                    }
                    is ApiResult.Failure -> dataMessage = result.message
                }
            }
        }
    }
    LaunchedEffect(page, token) {
        if (page == ProfilePage.EditProfile) {
            when (val result = withContext(Dispatchers.IO) { repository.getCurrentUser() }) {
                is ApiResult.Success -> {
                    currentUser = result.value
                    editName = result.value.name
                    editPhone = result.value.phone.orEmpty()
                    editAge = result.value.age?.toString().orEmpty()
                    editHeight = result.value.heightCm?.toString().orEmpty()
                    editWeight = result.value.weightKg?.toString().orEmpty()
                    editCalories = result.value.dailyCalories?.toString().orEmpty()
                    editGoal = result.value.fitnessGoal.orEmpty()
                    editAadhaar = result.value.aadhaarNumber.orEmpty()
                }
                is ApiResult.Failure -> dataMessage = result.message
            }
        }
        if (page == ProfilePage.Renew) {
            when (val result = withContext(Dispatchers.IO) { repository.getPaymentSettings() }) {
                is ApiResult.Success -> {
                    paymentPhone = result.value.phone.orEmpty()
                    paymentQrUrl = result.value.qrUrl.orEmpty()
                    paymentUpiId = normalizeUpiVpa(result.value.upiId.orEmpty(), paymentPhone)
                    if (paymentUpiId.isBlank()) {
                        dataMessage = "The admin must configure a valid UPI ID."
                    }
                }
                is ApiResult.Failure -> Unit
            }
            when (val result = withContext(Dispatchers.IO) { repository.listSubscriptionPlans(active = true) }) {
                is ApiResult.Success -> {
                    subscriptionPlans.clear()
                    subscriptionPlans.addAll(result.value)
                    if (subscriptionPlans.isEmpty()) dataMessage = "No subscription plans are currently available."
                }
                is ApiResult.Failure -> dataMessage = result.message
            }
            when (val result = withContext(Dispatchers.IO) { repository.listMemberDues() }) {
                is ApiResult.Success -> {
                    memberDues.clear()
                    memberDues.addAll(result.value)
                }
                is ApiResult.Failure -> dataMessage = result.message
            }
        }
        if (page == ProfilePage.Subscription) {
            val memberSubscriptions = withContext(Dispatchers.IO) { repository.mySubscriptions() }
            val plans = withContext(Dispatchers.IO) { repository.listSubscriptionPlans() }
            if (memberSubscriptions is ApiResult.Success && plans is ApiResult.Success) {
                subscriptions.clear()
                subscriptions.addAll(memberSubscriptions.value)
                subscriptionPlans.clear()
                subscriptionPlans.addAll(plans.value)
                dataMessage = if (subscriptions.isEmpty()) "No subscription history." else ""
            } else {
                dataMessage = "Could not load subscriptions."
            }
        }
        if (page == ProfilePage.Transactions) {
            when (val result = withContext(Dispatchers.IO) {
                if (role == UserRole.Admin) repository.listPayments() else repository.myPayments()
            }) {
                is ApiResult.Success -> {
                    payments.clear()
                    payments.addAll(result.value)
                    dataMessage = if (payments.isEmpty()) "No payment records." else ""
                }
                is ApiResult.Failure -> dataMessage = result.message
            }
        }
        if (page == ProfilePage.Attendance) {
            when (val result = withContext(Dispatchers.IO) {
                if (role == UserRole.Admin) repository.attendanceHistory() else repository.myAttendance()
            }) {
                is ApiResult.Success -> {
                    attendance.clear()
                    attendance.addAll(result.value)
                    dataMessage = if (attendance.isEmpty()) "No attendance records." else ""
                }
                is ApiResult.Failure -> dataMessage = result.message
            }
        }
        if (page == ProfilePage.Notifications) {
            when (val result = withContext(Dispatchers.IO) { repository.listNotifications() }) {
                is ApiResult.Success -> {
                    notifications.clear()
                    notifications.addAll(result.value)
                    dataMessage = if (notifications.isEmpty()) "No notifications." else ""
                }
                is ApiResult.Failure -> dataMessage = result.message
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FitnessPalette.Ink),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            Column(
                Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(20.dp),
            ) {
                IosBackButton(onClick = onBack, contentDescription = "Back")
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(FitnessPalette.Lime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(page.icon, null, tint = FitnessPalette.OnLime)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text(page.title, color = FitnessPalette.Label, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text(page.subtitle, color = FitnessPalette.Secondary, fontSize = 12.sp)
                    }
                }
            }
        }
        when (page) {
            ProfilePage.EditProfile -> item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DarkField(editName, { editName = it }, "Full name")
                    DarkField(editPhone, { editPhone = it }, "Phone")
                    Text(
                        "Health dashboard",
                        color = FitnessPalette.Lime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { DarkField(editAge, { editAge = it.filter(Char::isDigit).take(3) }, "Age") }
                        Box(Modifier.weight(1f)) { DarkField(editWeight, { editWeight = it.filter { char -> char.isDigit() || char == '.' }.take(6) }, "Weight kg") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { DarkField(editHeight, { editHeight = it.filter { char -> char.isDigit() || char == '.' }.take(6) }, "Height cm") }
                        Box(Modifier.weight(1f)) { DarkField(editCalories, { editCalories = it.filter(Char::isDigit).take(5) }, "Daily kcal") }
                    }
                    DarkField(editGoal, { editGoal = it.take(120) }, "Fitness goal")
                    Spacer(Modifier.height(4.dp))
                    HighlightCard(
                        "Identity verification",
                        currentUser?.verificationStatus?.replaceFirstChar(Char::uppercase) ?: "Unverified",
                        when (currentUser?.verificationStatus) {
                            "approved" -> "Your Aadhaar details were approved by the club admin."
                            "pending" -> "Your submission is waiting for admin review."
                            "rejected" -> currentUser?.verificationNote?.ifBlank { "Update your details and submit again." }
                                ?: "Update your details and submit again."
                            else -> "Add your 12-digit Aadhaar number and document photo."
                        },
                    )
                    DarkField(
                        editAadhaar,
                        { editAadhaar = it.filter(Char::isDigit).take(12) },
                        "12-digit Aadhaar number",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = aadhaarConsent,
                            onCheckedChange = { aadhaarConsent = it },
                        )
                        Text(
                            "I consent to secure storage and admin review of my Aadhaar details.",
                            color = FitnessPalette.Secondary,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    IosButton(
                        if (currentUser?.aadhaarPhotoPath.isNullOrBlank()) "Upload Aadhaar photo" else "Replace Aadhaar photo",
                        onClick = { aadhaarPhotoPicker.launch("image/*") },
                    )
                    if (editPassword.isNotBlank()) {
                        DarkField(currentPassword, { currentPassword = it }, "Current password")
                    }
                    DarkField(editPassword, { editPassword = it }, "New password (optional)")
                    Spacer(Modifier.height(8.dp))
                    IosButton(
                        "Save profile",
                        enabled = currentUser != null && editName.isNotBlank() &&
                            (editPassword.isBlank() || currentPassword.isNotBlank()) &&
                            (editAadhaar.isBlank() || editAadhaar.length == 12),
                        onClick = {
                            if (currentUser == null) return@IosButton
                            scope.launch {
                                val aadhaarChanged = editAadhaar.isNotBlank() &&
                                    editAadhaar != currentUser?.aadhaarNumber
                                if (aadhaarChanged && !aadhaarConsent) {
                                    dataMessage = "Consent is required before submitting Aadhaar details."
                                    return@launch
                                }
                                val updated = withContext(Dispatchers.IO) {
                                    repository.updateCurrentUser(
                                        name = editName,
                                        phone = editPhone,
                                        age = editAge.toIntOrNull(),
                                        heightCm = editHeight.toDoubleOrNull(),
                                        weightKg = editWeight.toDoubleOrNull(),
                                        dailyCalories = editCalories.toIntOrNull(),
                                        fitnessGoal = editGoal,
                                        aadhaarNumber = editAadhaar.takeIf { aadhaarChanged },
                                        aadhaarConsent = true.takeIf { aadhaarChanged },
                                    )
                                }
                                dataMessage = when (updated) {
                                    is ApiResult.Success -> {
                                        currentUser = updated.value
                                        if (editPassword.isNotBlank()) {
                                            when (val reset = withContext(Dispatchers.IO) {
                                                repository.updateCurrentPassword(currentPassword, editPassword)
                                            }) {
                                                is ApiResult.Success -> {
                                                    editPassword = ""
                                                    currentPassword = ""
                                                }
                                                is ApiResult.Failure -> {
                                                    dataMessage = reset.message
                                                    return@launch
                                                }
                                            }
                                        }
                                        "Profile saved."
                                    }
                                    is ApiResult.Failure -> updated.message
                                }
                            }
                        },
                    )
                    if (dataMessage.isNotBlank()) {
                        Text(dataMessage, color = FitnessPalette.Secondary)
                    }
                }
            }
            ProfilePage.Subscription -> {
                items(subscriptions, key = { it.id }) { subscription ->
                    val plan = subscriptionPlans.firstOrNull { it.id == subscription.planId }
                    HistoryCard(
                        plan?.name ?: "Subscription",
                        "${subscription.startsAt.orEmpty()} — ${subscription.endsAt.orEmpty()}",
                        subscription.status.uppercase(),
                    )
                }
                if (dataMessage.isNotBlank()) item { DataMessage(dataMessage) }
            }
            ProfilePage.Renew -> {
                item {
                    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (paymentQrUrl.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = paymentQrUrl,
                                contentDescription = "Payment QR code",
                                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(24.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            )
                        }
                        DarkField(paymentReference, { paymentReference = it }, "PhonePe transaction reference")
                        Text(
                            if (paymentUpiId.isBlank()) {
                                "The club admin has not configured a UPI ID yet."
                            } else {
                                "Pay to $paymentUpiId${paymentPhone.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}, then submit the PhonePe reference for admin approval."
                            },
                            color = FitnessPalette.Secondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                items(memberDues.filter { !it.status.equals("paid", true) }, key = { "due-${it.id}" }) { due ->
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 7.dp)) {
                        HighlightCard(
                            due.title,
                            "₹${due.amount.toInt()}",
                            "${due.dueMonth} · Due ${due.dueDate?.take(10).orEmpty()}",
                        )
                        Spacer(Modifier.height(8.dp))
                        IosButton(
                            text = "Pay monthly due with PhonePe",
                            enabled = paymentUpiId.isNotBlank(),
                            onClick = {
                                phonePeLauncher.launch(
                                    createPhonePeUpiIntent(
                                        context,
                                        due.amount.toString(),
                                        paymentUpiId,
                                    ),
                                )
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        IosButton(
                            text = "Submit due payment reference",
                            enabled = paymentReference.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    when (val result = withContext(Dispatchers.IO) {
                                        repository.submitPayment(
                                            amount = due.amount,
                                            method = "phonepe",
                                            reference = paymentReference,
                                            dueId = due.id,
                                        )
                                    }) {
                                        is ApiResult.Success -> {
                                            paymentReference = ""
                                            dataMessage = "Monthly due payment submitted for admin verification."
                                        }
                                        is ApiResult.Failure -> dataMessage = result.message
                                    }
                                }
                            },
                        )
                    }
                }
                items(subscriptionPlans, key = { it.id }) { plan ->
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 7.dp)) {
                        HighlightCard(
                            plan.name,
                            "₹${plan.price.toInt()}",
                            "${plan.durationDays} days · ${plan.description.orEmpty()}",
                        )
                        Spacer(Modifier.height(8.dp))
                        IosButton(
                            text = "Open PhonePe",
                            enabled = paymentUpiId.isNotBlank(),
                            onClick = {
                                phonePeLauncher.launch(
                                    createPhonePeUpiIntent(
                                        context = context,
                                        amount = plan.price.toString(),
                                        upiId = paymentUpiId,
                                    ),
                                )
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        IosButton(
                            text = "Submit payment reference",
                            enabled = paymentReference.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    when (val result = withContext(Dispatchers.IO) {
                                        repository.submitPayment(
                                            amount = plan.price,
                                            method = "phonepe",
                                            reference = paymentReference,
                                            subscriptionPlanId = plan.id,
                                        )
                                    }) {
                                        is ApiResult.Success -> {
                                            paymentReference = ""
                                            dataMessage = "Payment submitted. Status: ${result.value.status}."
                                        }
                                        is ApiResult.Failure -> dataMessage = result.message
                                    }
                                }
                            },
                        )
                    }
                }
                if (dataMessage.isNotBlank()) item { DataMessage(dataMessage) }
            }
            ProfilePage.Transactions -> {
                items(payments, key = { it.id }) { payment ->
                    HistoryCard(
                        if (role == UserRole.Admin) "₹${payment.amount.toInt()} · ${payment.memberId}" else "₹${payment.amount.toInt()}",
                        payment.reference ?: payment.createdAt.orEmpty(),
                        payment.status.uppercase(),
                    )
                }
                if (dataMessage.isNotBlank()) item { DataMessage(dataMessage) }
            }
            ProfilePage.Attendance -> {
                items(attendance, key = { it.id }) { record ->
                    TimelineRow(
                        record.checkInAt,
                        if (role == UserRole.Admin) record.memberId else "Gym attendance",
                        record.checkOutAt ?: "Checked in",
                    )
                }
                if (dataMessage.isNotBlank()) item { DataMessage(dataMessage) }
            }
            ProfilePage.Notifications -> {
                items(notifications, key = { it.id }) { notification ->
                    HistoryCard(
                        notification.title,
                        notification.body,
                        if (notification.read) "READ" else "NEW",
                    ) {
                        if (!notification.read) {
                            scope.launch {
                                when (val result = withContext(Dispatchers.IO) {
                                    repository.markNotificationRead(notification.id)
                                }) {
                                    is ApiResult.Success -> {
                                        val index = notifications.indexOfFirst { it.id == notification.id }
                                        if (index >= 0) notifications[index] = result.value
                                    }
                                    is ApiResult.Failure -> dataMessage = result.message
                                }
                            }
                        }
                    }
                }
                if (dataMessage.isNotBlank()) item { DataMessage(dataMessage) }
            }
        }
    }
}

@Composable
private fun ConversationScreen(
    conversation: Conversation,
    repository: DevFitnessRepository,
    token: String,
    onBack: () -> Unit,
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var currentUserId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Loading messages…") }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val name = conversation.title?.takeIf { it.isNotBlank() } ?: "Club conversation"
    DisposableEffect(conversation.id, token) {
        val realtime = RealtimeChatClient(
            baseUrl = BuildConfig.API_BASE_URL,
            token = token,
            conversationId = conversation.id,
            onMessage = { incoming ->
                if (messages.none { it.id == incoming.id }) messages.add(incoming)
                status = ""
            },
            onError = { status = it },
        )
        realtime.connect()
        onDispose { realtime.disconnect() }
    }
    LaunchedEffect(conversation.id) {
        val meResult = withContext(Dispatchers.IO) { repository.getCurrentUser() }
        if (meResult is ApiResult.Success) currentUserId = meResult.value.id
        when (val result = withContext(Dispatchers.IO) { repository.listMessages(conversation.id) }) {
            is ApiResult.Success -> {
                messages.clear()
                messages.addAll(result.value)
                status = if (messages.isEmpty()) "No messages yet." else ""
                withContext(Dispatchers.IO) { repository.markMessagesSeen(conversation.id) }
            }
            is ApiResult.Failure -> status = result.message
        }
    }
    Column(Modifier.fillMaxSize().background(FitnessPalette.Ink)) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = FitnessPalette.Label) }
            Box(Modifier.size(42.dp).clip(CircleShape).background(FitnessPalette.Lime), contentAlignment = Alignment.Center) {
                Text(name.take(1), fontWeight = FontWeight.Bold, color = FitnessPalette.OnLime)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(name, color = FitnessPalette.Label, fontWeight = FontWeight.SemiBold)
                Text("Online", color = FitnessPalette.Lime, fontSize = 11.sp)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(messages, key = { it.id }) { chatMessage ->
                val mine = chatMessage.senderId == currentUserId
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (mine) 20.dp else 5.dp,
                                    bottomEnd = if (mine) 5.dp else 20.dp,
                                ),
                            )
                            .background(if (mine) FitnessPalette.Lime else FitnessPalette.InkRaised)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(chatMessage.text, color = if (mine) FitnessPalette.OnLime else FitnessPalette.Label, fontSize = 14.sp)
                    }
                }
            }
            if (status.isNotBlank()) {
                item { Text(status, color = FitnessPalette.Secondary, modifier = Modifier.padding(12.dp)) }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message…") },
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(FitnessPalette.Lime)
                    .clickable {
                        if (draft.isNotBlank() && !sending) {
                            val text = draft.trim()
                            draft = ""
                            sending = true
                            scope.launch {
                                when (val result = withContext(Dispatchers.IO) {
                                    repository.sendMessage(conversation.id, text)
                                }) {
                                    is ApiResult.Success -> {
                                        messages.add(result.value)
                                        status = ""
                                    }
                                    is ApiResult.Failure -> {
                                        draft = text
                                        status = result.message
                                    }
                                }
                                sending = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Send, "Send", tint = FitnessPalette.OnLime)
            }
        }
    }
}

@Composable
internal fun MemberPageHeader(title: String, subtitle: String, icon: ImageVector) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(FitnessPalette.Lime),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = FitnessPalette.OnLime, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column {
            Text(title, color = FitnessPalette.Label, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = FitnessPalette.Secondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HighlightCard(title: String, value: String, detail: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(FitnessPalette.Lime.copy(alpha = 0.22f), FitnessPalette.InkRaised),
                ),
            )
            .padding(20.dp),
    ) {
        Text(title, color = FitnessPalette.Lime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(value, color = FitnessPalette.Label, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = FitnessPalette.Secondary, fontSize = 12.sp)
    }
}

@Composable
private fun TimelineRow(time: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(13.dp).clip(CircleShape).background(FitnessPalette.Lime))
            Box(Modifier.width(2.dp).height(50.dp).background(FitnessPalette.Lime.copy(alpha = 0.2f)))
        }
        Spacer(Modifier.width(13.dp))
        Column {
            Text(time, color = FitnessPalette.Lime, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(title, color = FitnessPalette.Label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = FitnessPalette.Secondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ActionListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(FitnessPalette.InkRaised)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(
                if (danger) FitnessPalette.Danger.copy(alpha = 0.15f) else FitnessPalette.Lime.copy(alpha = 0.15f),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (danger) FitnessPalette.Danger else FitnessPalette.Lime)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) FitnessPalette.Danger else FitnessPalette.Label, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = FitnessPalette.Secondary, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = FitnessPalette.Tertiary)
    }
}

@Composable
internal fun DataMessage(message: String) {
    Text(
        message,
        color = FitnessPalette.Secondary,
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
    )
}

@Composable
private fun HistoryCard(
    title: String,
    detail: String,
    status: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(FitnessPalette.InkRaised)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(FitnessPalette.Lime.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Verified, null, tint = FitnessPalette.Lime) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = FitnessPalette.Label, fontWeight = FontWeight.SemiBold)
            Text(detail, color = FitnessPalette.Secondary, fontSize = 11.sp)
        }
        Text(status, color = FitnessPalette.Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = FitnessPalette.Label,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

private fun latestLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) return null
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}

private fun createPhonePeUpiIntent(context: Context, amount: String, upiId: String): Intent {
    val uri = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", upiId)
        .appendQueryParameter("pn", "DEV Fitness Club")
        .appendQueryParameter("tn", "DEV Fitness membership renewal")
        .appendQueryParameter("am", amount)
        .appendQueryParameter("cu", "INR")
        .build()
    val phonePe = Intent(Intent.ACTION_VIEW, uri).setPackage("com.phonepe.app")
    return if (phonePe.resolveActivity(context.packageManager) != null) {
        phonePe
    } else {
        Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Pay with UPI")
    }
}

private fun normalizeUpiVpa(upiId: String, phone: String): String {
    val candidate = upiId.trim()
    if (Regex("""^[A-Za-z0-9._-]+@[A-Za-z0-9.-]+$""").matches(candidate)) return candidate
    val digits = candidate.filter(Char::isDigit).takeIf { it.length == 10 }
        ?: phone.filter(Char::isDigit).takeIf { it.length == 10 }
    return digits?.let { "$it@paytm" }.orEmpty()
}
