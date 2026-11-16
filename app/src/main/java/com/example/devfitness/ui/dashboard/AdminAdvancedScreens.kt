package com.example.devfitness.ui.dashboard

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.devfitness.BuildConfig
import com.example.devfitness.data.api.ApiClient
import com.example.devfitness.data.api.ApiResult
import com.example.devfitness.data.api.DevFitnessRepository
import com.example.devfitness.data.model.GalleryItem
import com.example.devfitness.data.model.MemberDue
import com.example.devfitness.data.model.User
import com.example.devfitness.ui.components.IosBackButton
import com.example.devfitness.ui.theme.FitnessPalette
import java.time.Instant
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdminDueScreen(token: String, onBack: () -> Unit) {
    val repository = advancedRepository(token)
    val dues = remember { mutableStateListOf<MemberDue>() }
    val members = remember { mutableStateListOf<User>() }
    var editor by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Loading monthly dues…") }
    LaunchedEffect(token) {
        val dueResult = withContext(Dispatchers.IO) { repository.listAllDues() }
        val memberResult = withContext(Dispatchers.IO) { repository.listUsers(role = "member", active = true) }
        if (dueResult is ApiResult.Success) dues.addAll(dueResult.value)
        if (memberResult is ApiResult.Success) members.addAll(memberResult.value)
        message = if (dues.isEmpty()) "No monthly dues assigned yet." else ""
    }
    AdvancedListShell("Monthly dues", "Set each member’s payable amount", onBack, onAdd = { editor = true }) {
        items(dues, key = { it.id }) { due ->
            val member = members.firstOrNull { it.id == due.memberId }
            AdvancedRow(
                member?.name ?: due.memberId,
                "${due.title} · ₹${due.amount.toInt()} · ${due.dueMonth}",
                due.status.uppercase(),
            )
        }
        if (message.isNotBlank()) item { DataMessage(message) }
    }
    if (editor) {
        val scope = rememberCoroutineScope()
        var title by remember { mutableStateOf("Monthly gym fee") }
        var amount by remember { mutableStateOf("") }
        var dueMonth by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("") }
        var all by remember { mutableStateOf(true) }
        val selected = remember { mutableStateListOf<String>() }
        RecipientEditorDialog(
            title = "Assign monthly dues",
            members = members,
            all = all,
            selected = selected,
            onAllChange = { checked ->
                all = checked
                selected.clear()
                if (checked) selected.addAll(members.map(User::id))
            },
            extra = {
                DarkField(title, { title = it }, "Fee title")
                DarkField(amount, { amount = it.filter { char -> char.isDigit() || char == '.' } }, "Amount")
                CalendarField("Due date", dueDate) { date ->
                    dueDate = "${date}T23:59:59.000Z"
                    dueMonth = date.take(7)
                }
            },
            confirmEnabled = amount.toDoubleOrNull()?.let { it > 0 } == true &&
                dueDate.isNotBlank() && (all || selected.isNotEmpty()),
            onDismiss = { editor = false },
            onConfirm = {
                scope.launch {
                    when (val result = withContext(Dispatchers.IO) {
                        repository.assignMonthlyDue(
                            memberIds = selected.toList(),
                            allMembers = all,
                            title = title,
                            amount = amount.toDouble(),
                            dueMonth = dueMonth,
                            dueDate = dueDate,
                        )
                    }) {
                        is ApiResult.Success -> message = "Assigned dues to ${result.value} member(s)."
                        is ApiResult.Failure -> message = result.message
                    }
                    editor = false
                }
            },
        )
    }
}

@Composable
fun AdminNotificationScreen(token: String, onBack: () -> Unit) {
    val repository = advancedRepository(token)
    val members = remember { mutableStateListOf<User>() }
    var editor by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Send an alert to all or selected members.") }
    LaunchedEffect(token) {
        val result = withContext(Dispatchers.IO) { repository.listUsers(role = "member", active = true) }
        if (result is ApiResult.Success) members.addAll(result.value)
    }
    AdvancedListShell("Notifications", "Messages, notices and member alerts", onBack, onAdd = { editor = true }) {
        item { DataMessage(message) }
    }
    if (editor) {
        val scope = rememberCoroutineScope()
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var all by remember { mutableStateOf(true) }
        val selected = remember { mutableStateListOf<String>() }
        RecipientEditorDialog(
            title = "Send notification",
            members = members,
            all = all,
            selected = selected,
            onAllChange = { checked ->
                all = checked
                selected.clear()
                if (checked) selected.addAll(members.map(User::id))
            },
            extra = {
                DarkField(title, { title = it }, "Title")
                DarkField(body, { body = it }, "Message", minLines = 3)
            },
            confirmEnabled = title.isNotBlank() && body.isNotBlank() && (all || selected.isNotEmpty()),
            onDismiss = { editor = false },
            onConfirm = {
                scope.launch {
                    when (val result = withContext(Dispatchers.IO) {
                        repository.sendNotification(title, body, selected.toList(), all)
                    }) {
                        is ApiResult.Success -> message = "Notification sent to ${result.value} member(s)."
                        is ApiResult.Failure -> message = result.message
                    }
                    editor = false
                }
            },
        )
    }
}

@Composable
fun AdminGalleryScreen(token: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = advancedRepository(token)
    val scope = rememberCoroutineScope()
    val gallery = remember { mutableStateListOf<GalleryItem>() }
    var message by remember { mutableStateOf("Choose a photo or video from this phone.") }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var editor by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var momentDate by remember { mutableStateOf(Instant.now().toString().take(10)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
        if (uri != null) {
            caption = ""
            momentDate = Instant.now().toString().take(10)
            editor = true
        }
    }
    LaunchedEffect(token) {
        when (val result = withContext(Dispatchers.IO) { repository.listGallery() }) {
            is ApiResult.Success -> gallery.addAll(result.value)
            is ApiResult.Failure -> message = result.message
        }
    }
    AdvancedListShell("Gallery", "Photos, videos and club moments", onBack, onAdd = { picker.launch("*/*") }) {
        items(gallery, key = { it.id }) { item ->
            AdvancedRow(
                item.caption.ifBlank { "Club moment" },
                item.momentDate.orEmpty(),
                if (item.mimeType.startsWith("video/")) "VIDEO" else "PHOTO",
            )
        }
        if (message.isNotBlank()) item { DataMessage(message) }
    }
    if (editor) {
        AlertDialog(
            onDismissRequest = { editor = false },
            containerColor = FitnessPalette.InkRaised,
            title = { Text("Publish club moment", color = FitnessPalette.Label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkField(caption, { caption = it }, "Caption")
                    CalendarField("Moment date", momentDate) { momentDate = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = selectedUri ?: return@TextButton
                    editor = false
                    scope.launch {
                        message = "Uploading moment…"
                        val result = withContext(Dispatchers.IO) {
                            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                ?: return@withContext ApiResult.Failure(message = "Could not read selected media")
                            when (val upload = repository.uploadMedia(bytes, mime)) {
                                is ApiResult.Success -> repository.createGalleryItem(
                                    upload.value.url,
                                    upload.value.mimeType,
                                    caption,
                                    "${momentDate}T12:00:00.000Z",
                                )
                                is ApiResult.Failure -> upload
                            }
                        }
                        when (result) {
                            is ApiResult.Success -> {
                                gallery.add(0, result.value)
                                message = "Moment published."
                            }
                            is ApiResult.Failure -> message = result.message
                        }
                    }
                }) { Text("Upload", color = FitnessPalette.Lime) }
            },
            dismissButton = { TextButton(onClick = { editor = false }) { Text("Cancel", color = FitnessPalette.Secondary) } },
        )
    }
}

@Composable
private fun AdvancedListShell(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(FitnessPalette.Ink)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            item {
                Column(Modifier.padding(20.dp)) {
                    IosBackButton(onBack, "Back")
                    MemberPageHeader(title, subtitle, Icons.Rounded.Payment)
                }
            }
            content()
        }
        FloatingActionButton(
            onClick = onAdd,
            containerColor = FitnessPalette.Lime,
            modifier = Modifier.align(Alignment.BottomEnd).padding(22.dp),
        ) { Text("+", color = FitnessPalette.OnLime) }
    }
}

@Composable
private fun AdvancedRow(title: String, detail: String, status: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Text(title, color = FitnessPalette.Label)
        Text(detail, color = FitnessPalette.Secondary)
        Text(status, color = FitnessPalette.Lime)
    }
}

@Composable
private fun RecipientEditorDialog(
    title: String,
    members: List<User>,
    all: Boolean,
    selected: MutableList<String>,
    onAllChange: (Boolean) -> Unit,
    extra: @Composable () -> Unit,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitnessPalette.InkRaised,
        title = { Text(title, color = FitnessPalette.Label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                extra()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("All active members", color = FitnessPalette.Label, modifier = Modifier.weight(1f))
                    Switch(checked = all, onCheckedChange = onAllChange)
                }
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(members, key = { it.id }) { member ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(member.name, color = FitnessPalette.Label)
                                Text(member.email, color = FitnessPalette.Secondary)
                            }
                            Checkbox(
                                checked = all || member.id in selected,
                                onCheckedChange = { checked ->
                                    if (!all) {
                                        if (checked) selected.add(member.id) else selected.remove(member.id)
                                    }
                                },
                                enabled = !all,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = confirmEnabled, onClick = onConfirm) {
                Text("Confirm", color = FitnessPalette.Lime)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = FitnessPalette.Secondary) } },
    )
}

@Composable
private fun CalendarField(label: String, value: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    TextButton(onClick = {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day -> onValueChange("%04d-%02d-%02d".format(year, month + 1, day)) },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }) { Text(if (value.isBlank()) label else "$label: ${value.take(10)}", color = FitnessPalette.Lime) }
}

@Composable
private fun advancedRepository(token: String) = remember(token) {
    DevFitnessRepository(ApiClient(BuildConfig.API_BASE_URL, tokenProvider = { token }))
}
