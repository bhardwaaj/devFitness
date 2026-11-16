package com.example.devfitness.ui.dashboard

import android.content.Intent
import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.devfitness.BuildConfig
import com.example.devfitness.data.api.ApiClient
import com.example.devfitness.data.api.ApiResult
import com.example.devfitness.data.api.DevFitnessRepository
import com.example.devfitness.data.model.User
import com.example.devfitness.data.model.PaymentSettings
import com.example.devfitness.ui.components.AppMark
import com.example.devfitness.ui.components.IosBackButton
import com.example.devfitness.ui.theme.FitnessPalette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class AdminSection(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    Members("User management", "Create, edit and share member logins", Icons.Rounded.People),
    Posts("Posts", "Photos, captions, likes and comments", Icons.Rounded.FitnessCenter),
    Diets("Diet plans", "Broadcast and member assignments", Icons.Rounded.Restaurant),
    Subscriptions("Subscriptions", "Plans, renewals and expiry", Icons.Rounded.Subscriptions),
    Payments("Payments", "UPI settings and verification", Icons.Rounded.CreditCard),
    Dues("Monthly dues", "Assign monthly amounts to members", Icons.Rounded.Payment),
    Blogs("Blogs", "Long-form fitness and nutrition content", Icons.Rounded.Article),
    Notices("Notices", "Member popups with deadlines", Icons.Rounded.Notifications),
    Offers("Offers", "Promotions with start and expiry dates", Icons.Rounded.CreditCard),
    Attendance("Attendance", "Check-ins, check-outs and history", Icons.Rounded.EventAvailable),
    Gallery("Gallery", "Upload club photos, videos and moments", Icons.Rounded.Article),
    Notifications("Notifications", "Send member alerts and announcements", Icons.Rounded.Notifications),
    Chats("Chats", "Member conversations and seen status", Icons.Rounded.Chat),
}

private data class MemberRow(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val active: Boolean,
    val age: Int?,
    val heightCm: Double?,
    val weightKg: Double?,
    val dailyCalories: Int?,
    val fitnessGoal: String,
    val verificationStatus: String,
    val verificationNote: String,
    val aadhaarSubmitted: Boolean,
    val aadhaarNumber: String,
    val aadhaarPhotoPath: String,
)

private fun User.toMemberRow() = MemberRow(
    id = id,
    name = name,
    email = email,
    phone = phone.orEmpty(),
    active = active,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    dailyCalories = dailyCalories,
    fitnessGoal = fitnessGoal.orEmpty(),
    verificationStatus = verificationStatus,
    verificationNote = verificationNote.orEmpty(),
    aadhaarSubmitted = aadhaarSubmitted,
    aadhaarNumber = aadhaarNumber.orEmpty(),
    aadhaarPhotoPath = aadhaarPhotoPath.orEmpty(),
)

private data class CrudRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: String,
    val deadline: String? = null,
    val mediaUrl: String? = null,
)

@Composable
fun MemberManagementScreen(token: String, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val repository = remember(token) {
        DevFitnessRepository(
            ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }),
        )
    }
    val scope = rememberCoroutineScope()
    val members = remember { mutableStateListOf<MemberRow>() }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<MemberRow?>(null) }
    var reviewing by remember { mutableStateOf<MemberRow?>(null) }
    var creating by remember { mutableStateOf(false) }
    var loadMessage by remember { mutableStateOf("Loading members…") }

    LaunchedEffect(token) {
        when (val result = withContext(Dispatchers.IO) { repository.listUsers(role = "member") }) {
            is ApiResult.Success -> {
                members.clear()
                members.addAll(result.value.map { it.toMemberRow() })
                loadMessage = if (members.isEmpty()) "No members yet. Tap + to create the first member." else ""
            }
            is ApiResult.Failure -> loadMessage = result.message
        }
    }

    Box(Modifier.fillMaxSize().background(FitnessPalette.Ink)) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                if (onBack != null) {
                    Column(
                        Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        IosBackButton(onClick = onBack, contentDescription = "Back")
                        Spacer(Modifier.height(16.dp))
                        PageHeader(
                            title = "Members",
                            subtitle = "${members.size} gym members · ${members.count { it.active }} active",
                            icon = Icons.Rounded.People,
                            applyStatusInsets = false,
                        )
                    }
                } else {
                    PageHeader(
                        title = "Members",
                        subtitle = "${members.size} gym members · ${members.count { it.active }} active",
                        icon = Icons.Rounded.People,
                    )
                }
            }
            item {
                SearchField(query = query, onQueryChange = { query = it }, placeholder = "Search name, email or phone")
            }
            items(
                members.filter {
                    query.isBlank() || listOf(it.name, it.email, it.phone).any { value ->
                        value.contains(query, ignoreCase = true)
                    }
                },
                key = { it.id },
            ) { member ->
                MemberCard(
                    member = member,
                    onVerify = { reviewing = member },
                    onEdit = { editing = member },
                    onShare = {
                        val message = """
                            Welcome to DEV Fitness Club!
                            
                            Download and install the DEV Fitness app, then sign in with:
                            Email: ${member.email}
                            
                            Contact the club admin for your temporary password.
                        """.trimIndent()
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message)
                                },
                                "Share member login",
                            ),
                        )
                    },
                    onDelete = {
                        scope.launch {
                            when (
                                val result = withContext(Dispatchers.IO) {
                                    repository.updateUser(member.id, active = false)
                                }
                            ) {
                                is ApiResult.Success -> {
                                    val index = members.indexOfFirst { it.id == member.id }
                                    if (index >= 0) members[index] = result.value.toMemberRow()
                                    loadMessage = "${member.name} is now inactive and cannot sign in."
                                }
                                is ApiResult.Failure -> loadMessage = result.message
                            }
                        }
                    },
                )
            }
            if (loadMessage.isNotBlank()) {
                item {
                    Text(
                        loadMessage,
                        color = FitnessPalette.Secondary,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            containerColor = FitnessPalette.Lime,
            contentColor = FitnessPalette.OnLime,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(22.dp),
        ) {
            Icon(Icons.Rounded.Add, "Add member")
        }
    }

    if (creating || editing != null) {
        MemberEditor(
            current = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { name, email, phone, password, active ->
                val current = editing
                if (current == null) {
                    scope.launch {
                        when (
                            val result = withContext(Dispatchers.IO) {
                                repository.createUser(
                                    name = name,
                                    email = email,
                                    password = password,
                                    role = "member",
                                    phone = phone,
                                    active = active,
                                )
                            }
                        ) {
                            is ApiResult.Success -> members.add(result.value.toMemberRow())
                            is ApiResult.Failure -> loadMessage = result.message
                        }
                    }
                } else {
                    scope.launch {
                        when (
                            val result = withContext(Dispatchers.IO) {
                                repository.updateUser(
                                    userId = current.id,
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    active = active,
                                )
                            }
                        ) {
                            is ApiResult.Success -> {
                                val index = members.indexOfFirst { it.id == current.id }
                                if (index >= 0) members[index] = result.value.toMemberRow()
                            }
                            is ApiResult.Failure -> loadMessage = result.message
                        }
                    }
                }
                creating = false
                editing = null
            },
        )
    }
    reviewing?.let { member ->
        VerificationReviewDialog(
            member = member,
            token = token,
            onDismiss = { reviewing = null },
            onDecision = { approved, note ->
                scope.launch {
                    when (val result = withContext(Dispatchers.IO) {
                        repository.reviewUserVerification(member.id, approved, note)
                    }) {
                        is ApiResult.Success -> {
                            val index = members.indexOfFirst { it.id == member.id }
                            if (index >= 0) members[index] = result.value.toMemberRow()
                            loadMessage = "${member.name} verification ${if (approved) "approved" else "rejected"}."
                            reviewing = null
                        }
                        is ApiResult.Failure -> loadMessage = result.message
                    }
                }
            },
        )
    }
}

@Composable
fun CreateHubScreen(token: String, onOpen: (AdminSection) -> Unit) {
    val actions = listOf(
        Triple("New post", "Share a photo or update", AdminSection.Posts),
        Triple("Add member", "Create gym login details", AdminSection.Members),
        Triple("Diet plan", "Assign or broadcast", AdminSection.Diets),
        Triple("Write blog", "Publish fitness content", AdminSection.Blogs),
        Triple("New notice", "Set a popup deadline", AdminSection.Notices),
        Triple("Create offer", "Promotion with expiry", AdminSection.Offers),
        Triple("Subscription plan", "Price and duration", AdminSection.Subscriptions),
        Triple("Payment QR", "Set PhonePe / UPI details", AdminSection.Payments),
        Triple("Monthly dues", "Assign member payment amounts", AdminSection.Dues),
        Triple("Gallery moment", "Upload a photo or video", AdminSection.Gallery),
        Triple("Notification", "Send an alert to members", AdminSection.Notifications),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FitnessPalette.Ink),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            PageHeader("Create", "Publish and manage club activity", Icons.Rounded.Add)
        }
        items(actions) { (title, subtitle, section) ->
            ActionRow(
                title = title,
                subtitle = subtitle,
                icon = when (section) {
                    AdminSection.Diets -> Icons.Rounded.Restaurant
                    AdminSection.Blogs -> Icons.Rounded.Article
                    AdminSection.Notices -> Icons.Rounded.Notifications
                    AdminSection.Offers -> Icons.Rounded.CreditCard
                    AdminSection.Subscriptions -> Icons.Rounded.Subscriptions
                    AdminSection.Payments -> Icons.Rounded.QrCode
                    AdminSection.Dues -> Icons.Rounded.Payment
                    AdminSection.Gallery -> Icons.Rounded.Article
                    AdminSection.Notifications -> Icons.Rounded.Notifications
                    else -> if (title == "Add member") Icons.Rounded.People else Icons.Rounded.FitnessCenter
                },
                onClick = {
                    onOpen(section ?: AdminSection.Posts)
                },
            )
        }
    }
}

@Composable
fun AdminManagementScreen(onOpen: (AdminSection) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FitnessPalette.Ink),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            PageHeader("Management", "Every club operation in one place", Icons.Rounded.VerifiedUser)
        }
        items(AdminSection.entries) { section ->
            ActionRow(section.title, section.subtitle, section.icon) { onOpen(section) }
        }
    }
}

@Composable
fun AdminCrudPage(section: AdminSection, token: String, onBack: () -> Unit) {
    if (section == AdminSection.Members) {
        MemberManagementScreen(token = token, onBack = onBack)
        return
    }
    if (section == AdminSection.Dues) {
        AdminDueScreen(token, onBack)
        return
    }
    if (section == AdminSection.Gallery) {
        AdminGalleryScreen(token, onBack)
        return
    }
    if (section == AdminSection.Notifications) {
        AdminNotificationScreen(token, onBack)
        return
    }
    val rows = remember(section) { mutableStateListOf<CrudRow>() }
    val repository = remember(token) {
        DevFitnessRepository(
            ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }),
        )
    }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<CrudRow?>(null) }
    var assigning by remember { mutableStateOf<CrudRow?>(null) }
    var creating by remember { mutableStateOf(false) }
    var apiMessage by remember { mutableStateOf("") }
    var paymentSettings by remember { mutableStateOf(PaymentSettings()) }
    var editingPaymentSettings by remember { mutableStateOf(false) }

    LaunchedEffect(section, token) {
        if (section == AdminSection.Payments) {
            val settings = withContext(Dispatchers.IO) { repository.getPaymentSettings() }
            if (settings is ApiResult.Success) paymentSettings = settings.value
        }
        val loaded = withContext(Dispatchers.IO) { loadCrudRows(repository, section) }
        if (loaded is ApiResult.Success) {
            rows.clear()
            rows.addAll(loaded.value)
            apiMessage = if (rows.isEmpty()) "Nothing here yet. Tap + to create one." else ""
        } else if (loaded is ApiResult.Failure) {
            apiMessage = loaded.message
        }
    }

    Box(Modifier.fillMaxSize().background(FitnessPalette.Ink)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            item {
                Column(
                    Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    IosBackButton(onClick = onBack, contentDescription = "Back")
                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionIcon(section.icon)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(section.title, color = FitnessPalette.Label, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Text(section.subtitle, color = FitnessPalette.Secondary, fontSize = 13.sp)
                        }
                    }
                }
            }
            if (section == AdminSection.Payments) {
                item {
                    PaymentSettingsCard(paymentSettings) { editingPaymentSettings = true }
                }
            }
            items(rows, key = { it.id }) { row ->
                CrudCard(
                    row = row,
                    canEdit = section !in setOf(AdminSection.Attendance, AdminSection.Chats),
                    canDelete = section !in setOf(AdminSection.Payments, AdminSection.Attendance, AdminSection.Chats),
                    extraActionLabel = if (section in setOf(AdminSection.Diets, AdminSection.Subscriptions)) "Assign" else null,
                    onExtraAction = { assigning = row },
                    onEdit = { editing = row },
                    onDelete = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { deleteCrudRow(repository, section, row.id) }
                            if (result is ApiResult.Success) {
                                rows.remove(row)
                            } else if (result is ApiResult.Failure) {
                                apiMessage = result.message
                            }
                        }
                    },
                )
            }
            if (apiMessage.isNotBlank()) {
                item {
                    Text(
                        apiMessage,
                        color = FitnessPalette.Secondary,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
        if (section !in setOf(AdminSection.Payments, AdminSection.Attendance, AdminSection.Chats)) {
            FloatingActionButton(
                onClick = { creating = true },
                containerColor = FitnessPalette.Lime,
                contentColor = FitnessPalette.OnLime,
                modifier = Modifier.align(Alignment.BottomEnd).padding(22.dp),
            ) { Icon(Icons.Rounded.Add, "Create") }
        }
    }

    if (creating || editing != null) {
        CrudEditor(
            section = section,
            current = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            repository = repository,
            onSave = { title, subtitle, status, deadline, mediaUrl ->
                val current = editing
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        saveCrudRow(repository, section, current, title, subtitle, status, deadline, mediaUrl)
                    }
                    when (result) {
                        is ApiResult.Success -> {
                            val saved = result.value
                            val index = rows.indexOfFirst { it.id == current?.id }
                            if (index >= 0) rows[index] = saved else rows.add(0, saved)
                        }
                        is ApiResult.Failure -> apiMessage = result.message
                    }
                }
                creating = false
                editing = null
            },
        )
    }
    if (editingPaymentSettings) {
        PaymentSettingsEditor(
            current = paymentSettings,
            repository = repository,
            onDismiss = { editingPaymentSettings = false },
            onSave = { updated ->
                scope.launch {
                    when (val result = withContext(Dispatchers.IO) { repository.updatePaymentSettings(updated) }) {
                        is ApiResult.Success -> paymentSettings = result.value
                        is ApiResult.Failure -> apiMessage = result.message
                    }
                }
                editingPaymentSettings = false
            },
        )
    }
    assigning?.let { plan ->
        PlanAssignmentDialog(
            section = section,
            plan = plan,
            repository = repository,
            onDismiss = { assigning = null },
        )
    }
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    applyStatusInsets: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (applyStatusInsets) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = FitnessPalette.Label, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = FitnessPalette.Secondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(FitnessPalette.Lime),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = FitnessPalette.OnLime, modifier = Modifier.size(27.dp))
    }
}

@Composable
internal fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = FitnessPalette.Tertiary) },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = FitnessPalette.Secondary) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
    )
}

@Composable
private fun MemberCard(
    member: MemberRow,
    onVerify: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FitnessPalette.InkRaised)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(FitnessPalette.Lime),
            contentAlignment = Alignment.Center,
        ) {
            Text(member.name.take(1), color = FitnessPalette.OnLime, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.name, color = FitnessPalette.Label, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(7.dp))
                StatusPill(if (member.active) "ACTIVE" else "INACTIVE")
            }
            Text(member.email, color = FitnessPalette.Secondary, fontSize = 12.sp)
            if (member.phone.isNotBlank()) {
                Text(member.phone, color = FitnessPalette.Tertiary, fontSize = 11.sp)
            }
            Text(
                "Verification: ${member.verificationStatus.replaceFirstChar(Char::uppercase)}",
                color = if (member.verificationStatus == "approved") FitnessPalette.Lime else FitnessPalette.Secondary,
                fontSize = 10.sp,
            )
        }
        IconButton(onClick = onVerify) {
            Icon(
                Icons.Rounded.VerifiedUser,
                "Review Aadhaar verification",
                tint = if (member.aadhaarSubmitted) FitnessPalette.Lime else FitnessPalette.Tertiary,
            )
        }
        IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Share login", tint = FitnessPalette.Secondary) }
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit", tint = FitnessPalette.Lime) }
        if (member.active) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, "Deactivate account", tint = FitnessPalette.Danger)
            }
        }
    }
}

@Composable
private fun VerificationReviewDialog(
    member: MemberRow,
    token: String,
    onDismiss: () -> Unit,
    onDecision: (Boolean, String) -> Unit,
) {
    val context = LocalContext.current
    var note by remember(member.id) { mutableStateOf(member.verificationNote) }
    val photoUrl = BuildConfig.API_BASE_URL.trimEnd('/') + member.aadhaarPhotoPath
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitnessPalette.InkRaised,
        title = {
            Column {
                Text("Identity verification", color = FitnessPalette.Label)
                Text(member.name, color = FitnessPalette.Lime, fontSize = 13.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusPill(member.verificationStatus.uppercase())
                Text(
                    if (member.aadhaarNumber.length == 12) {
                        "Aadhaar: ${member.aadhaarNumber.chunked(4).joinToString(" ")}"
                    } else {
                        "Aadhaar number not submitted"
                    },
                    color = FitnessPalette.Label,
                    fontWeight = FontWeight.SemiBold,
                )
                if (member.aadhaarPhotoPath.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUrl)
                            .addHeader("Authorization", "Bearer $token")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Aadhaar document for ${member.name}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(FitnessPalette.Ink),
                    )
                } else {
                    DataMessage("Aadhaar photo not submitted.")
                }
                Text(
                    listOfNotNull(
                        member.age?.let { "Age $it" },
                        member.weightKg?.let { "${it.toInt()} kg" },
                        member.heightCm?.let { "${it.toInt()} cm" },
                        member.dailyCalories?.let { "$it kcal" },
                    ).joinToString(" · ").ifBlank { "Health profile not completed" },
                    color = FitnessPalette.Secondary,
                    fontSize = 12.sp,
                )
                if (member.fitnessGoal.isNotBlank()) {
                    Text("Goal: ${member.fitnessGoal}", color = FitnessPalette.Secondary, fontSize = 12.sp)
                }
                DarkField(note, { note = it.take(500) }, "Admin note", minLines = 2)
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    enabled = member.aadhaarSubmitted,
                    onClick = { onDecision(false, note) },
                ) { Text("Reject", color = FitnessPalette.Danger) }
                TextButton(
                    enabled = member.aadhaarSubmitted,
                    onClick = { onDecision(true, note) },
                ) { Text("Approve", color = FitnessPalette.Lime) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = FitnessPalette.Secondary) }
        },
    )
}

@Composable
private fun CrudCard(
    row: CrudRow,
    canEdit: Boolean,
    canDelete: Boolean,
    extraActionLabel: String? = null,
    onExtraAction: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FitnessPalette.InkRaised)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.title, color = FitnessPalette.Label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                StatusPill(row.status.uppercase())
            }
            Spacer(Modifier.height(5.dp))
            Text(row.subtitle, color = FitnessPalette.Secondary, fontSize = 13.sp, lineHeight = 17.sp)
            row.deadline?.takeIf { it.isNotBlank() }?.let {
                Text("Until $it", color = FitnessPalette.Lime, fontSize = 11.sp)
            }
        }
        if (canEdit) {
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit", tint = FitnessPalette.Lime) }
        }
        if (extraActionLabel != null) {
            TextButton(onClick = onExtraAction) {
                Text(extraActionLabel, color = FitnessPalette.Lime, fontSize = 11.sp)
            }
        }
        if (canDelete) {
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Delete", tint = FitnessPalette.Danger) }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    val positive = text in listOf("PAID", "ACTIVE", "PUBLISHED", "APPROVED", "OPEN")
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (positive) FitnessPalette.Lime.copy(alpha = 0.14f) else FitnessPalette.Danger.copy(alpha = 0.13f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            color = if (positive) FitnessPalette.Lime else FitnessPalette.Danger,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FitnessPalette.InkRaised)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionIcon(icon)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = FitnessPalette.Label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, color = FitnessPalette.Secondary, fontSize = 12.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = FitnessPalette.Tertiary)
    }
}

@Composable
private fun MemberEditor(
    current: MemberRow?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit,
) {
    var name by remember(current) { mutableStateOf(current?.name.orEmpty()) }
    var email by remember(current) { mutableStateOf(current?.email.orEmpty()) }
    var phone by remember(current) { mutableStateOf(current?.phone.orEmpty()) }
    var active by remember(current) { mutableStateOf(current?.active ?: true) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitnessPalette.InkRaised,
        title = { Text(if (current == null) "Create member" else "Edit member", color = FitnessPalette.Label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkField(name, { name = it }, "Full name")
                DarkField(email, { email = it }, "Email")
                DarkField(phone, { phone = it }, "Phone")
                if (current == null) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Initial password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                }
                ToggleRow("Account active", active) { active = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && email.isNotBlank() && (current != null || password.length >= 6),
                onClick = { onSave(name, email, phone, password, active) },
            ) { Text("Save", color = FitnessPalette.Lime) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = FitnessPalette.Secondary) } },
    )
}

@Composable
private fun CrudEditor(
    section: AdminSection,
    current: CrudRow?,
    repository: DevFitnessRepository,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(current) { mutableStateOf(current?.title.orEmpty()) }
    var detail by remember(current) { mutableStateOf(current?.subtitle.orEmpty()) }
    var packagePrice by remember(current) {
        mutableStateOf(
            Regex("""₹?(\d+(?:\.\d+)?)""").find(current?.subtitle.orEmpty())
                ?.groupValues?.getOrNull(1).orEmpty(),
        )
    }
    var packageDays by remember(current) {
        mutableStateOf(
            Regex("""(\d+)\s*days""", RegexOption.IGNORE_CASE).find(current?.subtitle.orEmpty())
                ?.groupValues?.getOrNull(1).orEmpty(),
        )
    }
    var status by remember(current) { mutableStateOf(current?.status ?: "Active") }
    var deadline by remember(current) { mutableStateOf(current?.deadline.orEmpty()) }
    var mediaUrl by remember(current) { mutableStateOf(current?.mediaUrl) }
    var selectedMedia by remember { mutableStateOf<android.net.Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf("") }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedMedia = it
        if (it != null) uploadMessage = "Media selected from this device."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitnessPalette.InkRaised,
        title = { Text("${if (current == null) "Create" else "Edit"} ${section.title.lowercase()}", color = FitnessPalette.Label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DarkField(title, { title = it }, titleLabel(section))
                if (section == AdminSection.Subscriptions) {
                    DarkField(packagePrice, { packagePrice = it.filter { char -> char.isDigit() || char == '.' } }, "Package price")
                    DarkField(packageDays, { packageDays = it.filter(Char::isDigit) }, "Tenure in days")
                    DarkField(detail, { detail = it }, "Package description", minLines = 2)
                } else {
                    DarkField(detail, { detail = it }, detailLabel(section), minLines = 3)
                }
                DarkField(status, { status = it }, "Status / audience")
                if (section in setOf(AdminSection.Posts, AdminSection.Blogs, AdminSection.Notices, AdminSection.Offers)) {
                    TextButton(onClick = { mediaPicker.launch("image/*") }) {
                        Text(
                            if (selectedMedia == null && mediaUrl.isNullOrBlank()) "Choose image from gallery" else "Change selected image",
                            color = FitnessPalette.Lime,
                        )
                    }
                    if (uploadMessage.isNotBlank()) Text(uploadMessage, color = FitnessPalette.Secondary, fontSize = 11.sp)
                }
                if (section in setOf(AdminSection.Notices, AdminSection.Offers)) {
                    DatePickerField("Deadline", deadline) { deadline = it }
                    Text("Published items remain visible to members until this deadline.", color = FitnessPalette.Secondary, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && !uploading,
                onClick = {
                    val savedDetail = if (section == AdminSection.Subscriptions) {
                        "${packagePrice.ifBlank { "0" }} · ${packageDays.ifBlank { "30" }} days · $detail"
                    } else {
                        detail
                    }
                    val uri = selectedMedia
                    if (uri == null) {
                        onSave(title, savedDetail, status, deadline.takeIf { it.isNotBlank() }, mediaUrl)
                    } else {
                        uploading = true
                        scope.launch {
                            val upload = withContext(Dispatchers.IO) {
                                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    ?: return@withContext ApiResult.Failure(message = "Could not read selected image")
                                repository.uploadMedia(bytes, mime)
                            }
                            when (upload) {
                                is ApiResult.Success -> onSave(
                                    title,
                                    savedDetail,
                                    status,
                                    deadline.takeIf { it.isNotBlank() },
                                    upload.value.url,
                                )
                                is ApiResult.Failure -> {
                                    uploadMessage = upload.message
                                    uploading = false
                                }
                            }
                        }
                    }
                },
            ) {
                Text("Save", color = FitnessPalette.Lime)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = FitnessPalette.Secondary) } },
    )
}

@Composable
private fun PaymentSettingsCard(settings: PaymentSettings, onEdit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(FitnessPalette.Lime.copy(alpha = 0.13f))
            .padding(17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionIcon(Icons.Rounded.QrCode)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text("Payment receiver", color = FitnessPalette.Label, fontWeight = FontWeight.SemiBold)
            Text(
                settings.upiId?.takeIf { it.isNotBlank() } ?: "UPI ID not configured",
                color = FitnessPalette.Lime,
                fontSize = 13.sp,
            )
            if (!settings.phone.isNullOrBlank()) {
                Text(settings.phone.orEmpty(), color = FitnessPalette.Secondary, fontSize = 11.sp)
            }
        }
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit payment settings", tint = FitnessPalette.Lime) }
    }
}

@Composable
private fun PaymentSettingsEditor(
    current: PaymentSettings,
    repository: DevFitnessRepository,
    onDismiss: () -> Unit,
    onSave: (PaymentSettings) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var upiId by remember(current) { mutableStateOf(current.upiId.orEmpty()) }
    var phone by remember(current) { mutableStateOf(current.phone.orEmpty()) }
    var qrUrl by remember(current) { mutableStateOf(current.qrUrl.orEmpty()) }
    var uploadMessage by remember { mutableStateOf("") }
    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    val mime = context.contentResolver.getType(uri) ?: "image/png"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext ApiResult.Failure(message = "Could not read QR image")
                    repository.uploadMedia(bytes, mime)
                }
                when (result) {
                    is ApiResult.Success -> {
                        qrUrl = result.value.url
                        uploadMessage = "QR image uploaded."
                    }
                    is ApiResult.Failure -> uploadMessage = result.message
                }
            }
        }
    }
    val normalizedUpi = remember(upiId, phone) {
        val candidate = upiId.trim()
        when {
            Regex("""^[A-Za-z0-9._-]+@[A-Za-z0-9.-]+$""").matches(candidate) -> candidate
            candidate.filter(Char::isDigit).length == 10 -> "${candidate.filter(Char::isDigit)}@paytm"
            phone.filter(Char::isDigit).length == 10 -> "${phone.filter(Char::isDigit)}@paytm"
            else -> ""
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitnessPalette.InkRaised,
        title = { Text("Payment receiver", color = FitnessPalette.Label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DarkField(upiId, { upiId = it }, "PhonePe / UPI receiver ID")
                DarkField(phone, { phone = it }, "Payment phone number")
                TextButton(onClick = { qrPicker.launch("image/*") }) {
                    Text(if (qrUrl.isBlank()) "Choose payment QR from gallery" else "Change payment QR", color = FitnessPalette.Lime)
                }
                if (uploadMessage.isNotBlank()) Text(uploadMessage, color = FitnessPalette.Secondary, fontSize = 11.sp)
                Text(
                    if (normalizedUpi.isBlank()) {
                        "Enter a valid UPI ID such as 9876543210@paytm."
                    } else {
                        "Payments will open for $normalizedUpi"
                    },
                    color = if (normalizedUpi.isBlank()) FitnessPalette.Danger else FitnessPalette.Lime,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalizedUpi.isNotBlank(),
                onClick = { onSave(current.copy(upiId = normalizedUpi, phone = phone, qrUrl = qrUrl)) },
            ) { Text("Save", color = FitnessPalette.Lime) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = FitnessPalette.Secondary) } },
    )
}

@Composable
internal fun DarkField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        singleLine = minLines == 1,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = FitnessPalette.Label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PlanAssignmentDialog(
    section: AdminSection,
    plan: CrudRow,
    repository: DevFitnessRepository,
    onDismiss: () -> Unit,
) {
    val members = remember { mutableStateListOf<User>() }
    val selectedIds = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    var allMembers by remember { mutableStateOf(false) }
    var endDate by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Loading members…") }
    LaunchedEffect(plan.id) {
        when (val result = withContext(Dispatchers.IO) { repository.listUsers(role = "member", active = true) }) {
            is ApiResult.Success -> {
                members.clear()
                members.addAll(result.value)
                message = if (members.isEmpty()) "No active members available." else ""
            }
            is ApiResult.Failure -> message = result.message
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitnessPalette.InkRaised,
        title = { Text("Assign ${plan.title}", color = FitnessPalette.Label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (section == AdminSection.Subscriptions) {
                    DatePickerField("Subscription end date", endDate) { endDate = it }
                }
                ToggleRow("Assign to all active members", allMembers) {
                    allMembers = it
                    selectedIds.clear()
                    if (it) selectedIds.addAll(members.map(User::id))
                }
                if (message.isNotBlank()) {
                    Text(message, color = FitnessPalette.Secondary, fontSize = 12.sp)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(members, key = { it.id }) { member ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(FitnessPalette.Ink).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(member.name, color = FitnessPalette.Label, fontWeight = FontWeight.SemiBold)
                                Text(member.email, color = FitnessPalette.Secondary, fontSize = 10.sp)
                            }
                            Checkbox(
                                checked = member.id in selectedIds,
                                onCheckedChange = { checked ->
                                    allMembers = false
                                    if (checked) selectedIds.add(member.id) else selectedIds.remove(member.id)
                                },
                            )
                            if (section == AdminSection.Diets) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            message = when (
                                                val history = withContext(Dispatchers.IO) {
                                                    repository.dietHistory(member.id)
                                                }
                                            ) {
                                                is ApiResult.Success -> "${member.name}: ${history.value.size} diet assignment(s) in history."
                                                is ApiResult.Failure -> history.message
                                            }
                                        }
                                    },
                                ) { Text("History", color = FitnessPalette.Secondary, fontSize = 10.sp) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedIds.isNotEmpty() &&
                    (section != AdminSection.Subscriptions || endDate.isNotBlank()),
                onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            if (section == AdminSection.Diets) {
                                repository.assignDietPlanToMembers(plan.id, selectedIds.toList()).map { Unit }
                            } else {
                                var outcome: ApiResult<Unit> = ApiResult.Success(Unit, 200)
                                for (memberId in selectedIds) {
                                    if (outcome is ApiResult.Failure) break
                                    outcome = repository.createMemberSubscription(
                                            memberId = memberId,
                                            planId = plan.id,
                                            endsAt = endDate,
                                        ).map { Unit }
                                }
                                outcome
                            }
                        }
                        message = when (result) {
                            is ApiResult.Success -> "${plan.title} assigned to ${selectedIds.size} member(s)."
                            is ApiResult.Failure -> result.message
                        }
                    }
                },
            ) { Text("Assign", color = FitnessPalette.Lime) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = FitnessPalette.Secondary) } },
    )
}

@Composable
private fun DatePickerField(label: String, value: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onValueChange("%04d-%02d-%02dT23:59:59.000Z".format(year, month + 1, day))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            ).show()
        },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FitnessPalette.Ink),
    ) {
        Text(
            if (value.isBlank()) label else "$label: ${value.take(10)}",
            color = if (value.isBlank()) FitnessPalette.Secondary else FitnessPalette.Lime,
        )
    }
}

private fun titleLabel(section: AdminSection): String = when (section) {
    AdminSection.Members -> "Member name"
    AdminSection.Posts -> "Caption"
    AdminSection.Diets -> "Plan name"
    AdminSection.Subscriptions -> "Plan name"
    AdminSection.Payments -> "Payer / payment method"
    AdminSection.Dues -> "Monthly due"
    AdminSection.Blogs -> "Blog title"
    AdminSection.Notices -> "Notice headline"
    AdminSection.Offers -> "Offer title"
    AdminSection.Attendance -> "Member name"
    AdminSection.Gallery -> "Gallery moment"
    AdminSection.Notifications -> "Notification"
    AdminSection.Chats -> "Conversation"
}

private fun detailLabel(section: AdminSection): String = when (section) {
    AdminSection.Members -> "Email, phone and plan"
    AdminSection.Posts -> "Post content / media URL"
    AdminSection.Diets -> "Meals and instructions"
    AdminSection.Subscriptions -> "Duration and price"
    AdminSection.Payments -> "Amount, UPI reference or QR URL"
    AdminSection.Dues -> "Amount, month and deadline"
    AdminSection.Blogs -> "Article body"
    AdminSection.Notices -> "Body, audience and deadline"
    AdminSection.Offers -> "Description, discount and deadline"
    AdminSection.Attendance -> "Check-in and check-out time"
    AdminSection.Gallery -> "Caption and media"
    AdminSection.Notifications -> "Message and recipients"
    AdminSection.Chats -> "Latest message"
}

private fun loadCrudRows(
    repository: DevFitnessRepository,
    section: AdminSection,
): ApiResult<List<CrudRow>> = when (section) {
    AdminSection.Members -> ApiResult.Success(emptyList(), 200)
    AdminSection.Posts -> repository.listPosts().map { posts ->
        posts.map {
            CrudRow(
                it.id,
                it.text,
                "${it.likeCount} likes · ${it.commentCount} comments",
                "Published",
                mediaUrl = it.imageUrl,
            )
        }
    }
    AdminSection.Diets -> repository.listDietPlans().map { plans ->
        plans.map { CrudRow(it.id, it.name, it.description.orEmpty(), if (it.active) "Active" else "Inactive") }
    }
    AdminSection.Subscriptions -> repository.listSubscriptionPlans().map { plans ->
        plans.map {
            CrudRow(
                it.id,
                it.name,
                "₹${it.price.toInt()} · ${it.durationDays} days${it.description?.takeIf(String::isNotBlank)?.let { text -> " · $text" }.orEmpty()}",
                if (it.active) "Active" else "Inactive",
            )
        }
    }
    AdminSection.Payments -> repository.listPayments().map { payments ->
        payments.map { CrudRow(it.id, it.memberId, "₹${it.amount.toInt()} · ${it.reference.orEmpty()}", it.status) }
    }
    AdminSection.Dues -> repository.listAllDues().map { dues ->
        dues.map { CrudRow(it.id, it.title, "₹${it.amount.toInt()} · ${it.dueMonth}", it.status) }
    }
    AdminSection.Blogs -> repository.listBlogs().map { blogs ->
        blogs.map {
            CrudRow(
                it.id,
                it.title,
                it.body,
                if (it.published) "Published" else "Draft",
                mediaUrl = it.imageUrl,
            )
        }
    }
    AdminSection.Notices -> repository.listNotices().map { notices ->
        notices.map {
            CrudRow(
                it.id,
                it.title,
                it.body,
                if (it.published) "Published" else "Draft",
                it.deadline,
                it.imageUrl,
            )
        }
    }
    AdminSection.Offers -> repository.listOffers().map { offers ->
        offers.map {
            CrudRow(
                it.id,
                it.title,
                it.description,
                if (it.active) "Published" else "Draft",
                it.endsAt,
                it.imageUrl,
            )
        }
    }
    AdminSection.Attendance -> repository.attendanceHistory().map { records ->
        records.map { CrudRow(it.id, it.memberId, "${it.checkInAt} → ${it.checkOutAt ?: "Checked in"}", it.status ?: "Open") }
    }
    AdminSection.Gallery -> repository.listGallery().map { items ->
        items.map { CrudRow(it.id, it.caption.ifBlank { "Gallery moment" }, it.url, "Published") }
    }
    AdminSection.Notifications -> ApiResult.Success(emptyList(), 200)
    AdminSection.Chats -> repository.listConversations().map { conversations ->
        conversations.map { CrudRow(it.id, it.title ?: "Member chat", it.lastMessage.orEmpty(), "Open") }
    }
}

private fun saveCrudRow(
    repository: DevFitnessRepository,
    section: AdminSection,
    current: CrudRow?,
    title: String,
    detail: String,
    status: String,
    deadline: String?,
    mediaUrl: String?,
): ApiResult<CrudRow> {
    val canUpdate = current != null
    return when (section) {
        AdminSection.Posts -> {
            val result = if (canUpdate) {
                repository.updatePost(current!!.id, text = title, imageUrl = mediaUrl)
            } else {
                repository.createPost(title, mediaUrl)
            }
            result.map {
                CrudRow(
                    it.id,
                    it.text,
                    "${it.likeCount} likes · ${it.commentCount} comments",
                    "Published",
                    mediaUrl = it.imageUrl,
                )
            }
        }
        AdminSection.Diets -> {
            val result = if (canUpdate) {
                repository.updateDietPlan(current!!.id, name = title, description = detail, active = status != "Inactive")
            } else {
                repository.createDietPlan(title, description = detail, active = status != "Inactive")
            }
            result.map { CrudRow(it.id, it.name, it.description.orEmpty(), if (it.active) "Active" else "Inactive") }
        }
        AdminSection.Subscriptions -> {
            val amount = Regex("""\d+(?:\.\d+)?""").find(detail)?.value?.toDoubleOrNull() ?: 1200.0
            val days = Regex("""(\d+)\s*days""", RegexOption.IGNORE_CASE).find(detail)
                ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 30
            val description = detail.substringAfter("days ·", "").trim().takeIf(String::isNotBlank)
            val result = if (canUpdate) {
                repository.updateSubscriptionPlan(
                    current!!.id,
                    name = title,
                    price = amount,
                    durationDays = days,
                    description = description,
                    active = status != "Inactive",
                )
            } else {
                repository.createSubscriptionPlan(
                    title,
                    amount,
                    days,
                    description = description,
                    active = status != "Inactive",
                )
            }
            result.map {
                CrudRow(
                    it.id,
                    it.name,
                    "₹${it.price.toInt()} · ${it.durationDays} days${it.description?.takeIf(String::isNotBlank)?.let { text -> " · $text" }.orEmpty()}",
                    if (it.active) "Active" else "Inactive",
                )
            }
        }
        AdminSection.Blogs -> {
            val published = status != "Draft"
            val result = if (canUpdate) {
                repository.updateBlog(current!!.id, title = title, body = detail, imageUrl = mediaUrl, published = published)
            } else {
                repository.createBlog(title, detail, imageUrl = mediaUrl, published = published)
            }
            result.map {
                CrudRow(
                    it.id,
                    it.title,
                    it.body,
                    if (it.published) "Published" else "Draft",
                    mediaUrl = it.imageUrl,
                )
            }
        }
        AdminSection.Notices -> {
            val published = status != "Draft"
            val result = if (canUpdate) {
                repository.updateNotice(
                    current!!.id,
                    title = title,
                    body = detail,
                    published = published,
                    deadline = deadline,
                    imageUrl = mediaUrl,
                )
            } else {
                repository.createNotice(title, detail, published, deadline = deadline, imageUrl = mediaUrl)
            }
            result.map {
                CrudRow(
                    it.id,
                    it.title,
                    it.body,
                    if (it.published) "Published" else "Draft",
                    it.deadline,
                    it.imageUrl,
                )
            }
        }
        AdminSection.Offers -> {
            val active = status != "Draft"
            val result = if (canUpdate) {
                repository.updateOffer(
                    current!!.id,
                    title = title,
                    description = detail,
                    endsAt = deadline,
                    active = active,
                    imageUrl = mediaUrl,
                )
            } else {
                repository.createOffer(title, detail, endsAt = deadline, active = active, imageUrl = mediaUrl)
            }
            result.map {
                CrudRow(
                    it.id,
                    it.title,
                    it.description,
                    if (it.active) "Published" else "Draft",
                    it.endsAt,
                    it.imageUrl,
                )
            }
        }
        AdminSection.Payments -> {
            if (canUpdate && status.lowercase() in setOf("approved", "rejected")) {
                repository.reviewPayment(
                    paymentId = current!!.id,
                    approved = status.equals("approved", ignoreCase = true),
                    adminNote = detail,
                ).map {
                    CrudRow(it.id, it.memberId, "₹${it.amount.toInt()} · ${it.reference.orEmpty()}", it.status)
                }
            } else {
                ApiResult.Failure(
                    message = "Set payment status to Approved or Rejected before saving.",
                )
            }
        }
        else -> ApiResult.Failure(message = "This page is read-only. Records are created by member activity.")
    }
}

private fun deleteCrudRow(
    repository: DevFitnessRepository,
    section: AdminSection,
    id: String,
): ApiResult<Unit> {
    return when (section) {
        AdminSection.Posts -> repository.deletePost(id)
        AdminSection.Diets -> repository.deleteDietPlan(id)
        AdminSection.Subscriptions -> repository.deleteSubscriptionPlan(id)
        AdminSection.Blogs -> repository.deleteBlog(id)
        AdminSection.Notices -> repository.deleteNotice(id)
        AdminSection.Offers -> repository.deleteOffer(id)
        else -> ApiResult.Failure(message = "This record cannot be deleted from this page.")
    }
}
