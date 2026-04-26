@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.data.AdminRepository
import common.data.EventRepository
import common.model.Event
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.RyderAccent
import common.ui.pages.components.ScrollTimePickerDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventDetailPage(
    eventId: String,
    currentUser: User?,
    onBack: () -> Unit
) {
    val repo = remember { EventRepository() }
    val adminRepo = remember { AdminRepository() }
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPlace by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editHour by remember { mutableStateOf(0) }
    var editMinute by remember { mutableStateOf(0) }
    var showEditTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        isLoading = true
        try { event = repo.getEvent(eventId) } catch (_: Exception) {}
        isLoading = false
    }

    val e = event
    val isCreator = e != null && currentUser != null && e.creatorId == currentUser.uid
    val isAttending = e != null && currentUser != null && currentUser.uid in e.attendeeIds

    val bg = AppColors.background
    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val divColor = AppColors.divider
    val inputBorder = AppColors.inputBorder

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        focusedBorderColor = RyderAccent,
        unfocusedBorderColor = inputBorder,
        focusedLabelColor = RyderAccent,
        unfocusedLabelColor = textSecondary,
        cursorColor = RyderAccent
    )

    Scaffold(
        containerColor = bg,
        topBar = {
            Surface(color = surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = textPrimary)
                    }
                    Text(
                        text = e?.name ?: "Notikums",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentUser != null && e != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = textSecondary)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (isCreator) {
                                    DropdownMenuItem(
                                        text = { Text("Rediģēt notikumu") },
                                        onClick = {
                                            showMenu = false
                                            editName = e.name
                                            editPlace = e.place
                                            editDescription = e.description
                                            val cal = Calendar.getInstance().apply { timeInMillis = e.dateTime }
                                            editHour = cal.get(Calendar.HOUR_OF_DAY)
                                            editMinute = cal.get(Calendar.MINUTE)
                                            showEditDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Dzēst notikumu", color = Color(0xFFE53935)) },
                                        onClick = { showMenu = false; showDeleteConfirm = true }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Ziņot notikumu") },
                                        leadingIcon = { Icon(Icons.Default.Flag, null, tint = textSecondary) },
                                        onClick = { showMenu = false; showReportDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RyderAccent)
            }
            return@Scaffold
        }
        if (e == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Notikums nav atrasts", color = textSecondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface)
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(modifier = Modifier.size(72.dp), shape = CircleShape, color = RyderAccent) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(e.name, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Organizē: ${e.creatorNickname}", color = textSecondary, fontSize = 13.sp)
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface)
                        .padding(20.dp)
                ) {
                    EventInfoRow(icon = Icons.Default.Place, label = "Vieta", value = e.place, textPrimary = textPrimary, textSecondary = textSecondary)
                    Spacer(Modifier.height(16.dp))
                    EventInfoRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Datums un laiks",
                        value = formatEventDateTime(e.dateTime),
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    if (e.description.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        EventInfoRow(icon = Icons.Default.Info, label = "Apraksts", value = e.description, textPrimary = textPrimary, textSecondary = textSecondary)
                    }
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = divColor)
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${e.attendeeIds.size} apmeklētāji",
                                color = textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            if (isAttending) {
                                Text("Tu apmeklē šo notikumu", color = RyderAccent, fontSize = 12.sp)
                            }
                        }
                        if (currentUser != null && !isCreator) {
                            Button(
                                onClick = {
                                    val uid = currentUser.uid
                                    scope.launch {
                                        try {
                                            if (isAttending) {
                                                repo.unattendEvent(eventId, uid)
                                                event = e.copy(attendeeIds = e.attendeeIds - uid)
                                            } else {
                                                repo.attendEvent(eventId, uid)
                                                event = e.copy(attendeeIds = e.attendeeIds + uid)
                                            }
                                        } catch (_: Exception) {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAttending) Color.Transparent else RyderAccent,
                                    contentColor = if (isAttending) RyderAccent else Color.White
                                ),
                                border = if (isAttending)
                                    androidx.compose.foundation.BorderStroke(1.dp, RyderAccent) else null,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    if (isAttending) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isAttending) "Apmeklēju" else "Apmeklēt")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = surface,
            title = { Text("Dzēst notikumu?", color = textPrimary) },
            text = { Text("Šo darbību nevar atsaukt.", color = textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        try { repo.deleteEvent(eventId) } catch (_: Exception) {}
                        onBack()
                    }
                }) { Text("Dzēst", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Atcelt", color = textSecondary)
                }
            }
        )
    }

    if (showEditDialog && e != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = surface,
            title = { Text("Rediģēt notikumu", color = textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nosaukums") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = editPlace,
                        onValueChange = { editPlace = it },
                        label = { Text("Vieta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Apraksts") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        minLines = 2
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AppColors.inputBorder, RoundedCornerShape(12.dp))
                            .clickable { showEditTimePicker = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = RyderAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = String.format("%02d:%02d", editHour, editMinute),
                            color = textPrimary,
                            fontSize = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val dayMillis = run {
                        val cal = Calendar.getInstance().apply { timeInMillis = e.dateTime }
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                    val updated = e.copy(
                        name = editName.trim().ifEmpty { e.name },
                        place = editPlace.trim().ifEmpty { e.place },
                        description = editDescription.trim(),
                        dateTime = dayMillis + editHour * 3_600_000L + editMinute * 60_000L
                    )
                    showEditDialog = false
                    scope.launch {
                        try {
                            repo.updateEvent(updated)
                            event = updated
                        } catch (_: Exception) {}
                    }
                }) { Text("Saglabāt", color = RyderAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Atcelt", color = textSecondary) }
            }
        )
    }

    if (showEditTimePicker) {
        ScrollTimePickerDialog(
            initialHour = editHour,
            initialMinute = editMinute,
            onConfirm = { h, m -> editHour = h; editMinute = m; showEditTimePicker = false },
            onDismiss = { showEditTimePicker = false }
        )
    }

    if (showReportDialog && currentUser != null && e != null) {
        val reasons = listOf("Surogātpasts", "Nepiedienīgs saturs", "Naida runa", "Viltus informācija", "Cits")
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = surface,
            title = { Text("Ziņot par notikumu", color = textPrimary) },
            text = {
                Column {
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                showReportDialog = false
                                scope.launch {
                                    try {
                                        adminRepo.submitReport(
                                            targetId = eventId,
                                            targetType = "event",
                                            targetOwnerNickname = e.name,
                                            reporterId = currentUser.uid,
                                            reporterNickname = currentUser.nickname,
                                            reason = reason
                                        )
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reason, color = textPrimary, modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider(color = divColor)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Atcelt", color = textSecondary) }
            }
        )
    }
}

@Composable
private fun EventInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = RyderAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = textSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = textPrimary, fontSize = 15.sp)
        }
    }
}

private fun formatEventDateTime(millis: Long): String {
    if (millis == 0L) return "Nav norādīts"
    val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
