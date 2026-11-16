package com.example.devfitness.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.devfitness.data.UserRole
import com.example.devfitness.BuildConfig
import com.example.devfitness.data.api.RealtimeUpdatesClient
import com.example.devfitness.data.api.ApiClient
import com.example.devfitness.data.api.ApiResult
import com.example.devfitness.data.api.DevFitnessRepository
import com.example.devfitness.ui.notifications.AppNotifications
import com.example.devfitness.ui.theme.FitnessPalette
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class NavItem(
    val label: String,
    val icon: ImageVector,
    val accent: Boolean = false,
)

private val adminNav = listOf(
    NavItem("Home", Icons.Rounded.Home),
    NavItem("Manage", Icons.Rounded.Dashboard),
    NavItem("Create", Icons.Rounded.Add, accent = true),
    NavItem("Chats", Icons.Rounded.ChatBubble),
)

private val memberNav = listOf(
    NavItem("Home", Icons.Rounded.Home),
    NavItem("Diet", Icons.Rounded.Restaurant),
    NavItem("Check in", Icons.Rounded.LocationOn, accent = true),
    NavItem("Gallery", Icons.Rounded.PhotoLibrary),
    NavItem("Chats", Icons.Rounded.ChatBubble),
)

@Composable
fun RoleDashboard(
    name: String,
    email: String,
    role: UserRole,
    token: String,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    DisposableEffect(token) {
        val updates = RealtimeUpdatesClient(
            baseUrl = BuildConfig.API_BASE_URL,
            token = token,
            onNotification = { title, body -> AppNotifications.show(context, title, body) },
            onChat = { sender, message -> AppNotifications.show(context, sender, message) },
        )
        updates.connect()
        onDispose { updates.disconnect() }
    }
    val notificationRepository = remember(token) {
        DevFitnessRepository(ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }))
    }
    val deliveredNotifications = remember {
        context.getSharedPreferences("delivered_notifications", android.content.Context.MODE_PRIVATE)
    }
    LaunchedEffect(token) {
        while (true) {
            when (val result = withContext(Dispatchers.IO) {
                notificationRepository.listNotifications(unreadOnly = true)
            }) {
                is ApiResult.Success -> result.value.asReversed()
                    .filter { !it.read && !deliveredNotifications.getBoolean(it.id, false) }
                    .forEach { notification ->
                        AppNotifications.show(context, notification.title, notification.body)
                        deliveredNotifications.edit().putBoolean(notification.id, true).apply()
                    }
                is ApiResult.Failure -> Unit
            }
            delay(10_000)
        }
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var adminSection by remember { mutableStateOf<AdminSection?>(null) }
    val navItems = if (role == UserRole.Admin) adminNav else memberNav

    BackHandler(enabled = adminSection != null || selectedTab != 0) {
        if (adminSection != null) adminSection = null else selectedTab = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = FitnessPalette.Ink,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            SocialBottomBar(
                items = navItems,
                selected = selectedTab,
                onSelect = {
                    adminSection = null
                    selectedTab = it
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = Triple(selectedTab, adminSection, role),
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(initialScale = 0.985f)) togetherWith fadeOut(tween(140))
            },
            label = "app-page",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) { (tab, section, currentRole) ->
            if (section != null && currentRole == UserRole.Admin) {
                AdminCrudPage(section = section, token = token, onBack = { adminSection = null })
            } else if (currentRole == UserRole.Admin) {
                when (tab) {
                    0 -> SocialFeedScreen(
                        name = name,
                        role = currentRole,
                        token = token,
                        onNotificationsClick = { adminSection = AdminSection.Notifications },
                        onProfileClick = { selectedTab = 4 },
                    )
                    1 -> AdminManagementScreen(onOpen = { adminSection = it })
                    2 -> CreateHubScreen(token = token, onOpen = { adminSection = it })
                    3 -> ChatListScreen(token = token, role = currentRole)
                    else -> ProfileScreen(
                        name = name,
                        email = email,
                        role = currentRole,
                        token = token,
                        onLogout = onLogout,
                    )
                }
            } else {
                when (tab) {
                    0 -> SocialFeedScreen(
                        name = name,
                        role = currentRole,
                        token = token,
                        onNotificationsClick = { selectedTab = 6 },
                        onProfileClick = { selectedTab = 5 },
                    )
                    1 -> DietPlanScreen(token = token)
                    2 -> AttendanceCheckScreen(token = token)
                    3 -> GalleryScreen(token = token)
                    4 -> ChatListScreen(token = token, role = currentRole)
                    else -> ProfileScreen(
                        name = name,
                        email = email,
                        role = currentRole,
                        token = token,
                        openNotifications = tab == 6,
                        onLogout = onLogout,
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialBottomBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xF2182219))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selected == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.accent) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(FitnessPalette.Lime),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(item.icon, item.label, tint = FitnessPalette.OnLime, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) FitnessPalette.Lime else FitnessPalette.Secondary,
                                modifier = Modifier.size(25.dp),
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = item.label,
                                color = if (isSelected) FitnessPalette.Lime else FitnessPalette.Secondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}
