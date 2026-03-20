@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import androidx.compose.foundation.background
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
import common.data.EventRepository
import common.model.Event
import common.model.User
import common.ui.pages.components.RyderAccent
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
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPlace by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    LaunchedEffect(eventId) {
        isLoading = true
        try { event = repo.getEvent(eventId) } catch (_: Exception) {}
        isLoading = false
    }

    val e = event
    val isCreator = e != null && currentUser != null && e.creatorId == currentUser.uid
    val isAttending = e != null && currentUser != null && currentUser.uid in e.attendeeIds

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF1A1A1A),
        unfocusedTextColor = Color(0xFF1A1A1A),
        focusedBorderColor = RyderAccent,
        unfocusedBorderColor = Color(0xFF9E9E9E),
        focusedLabelColor = RyderAccent,
        unfocusedLabelColor = Color(0xFF757575),
        cursorColor = RyderAccent
    )

    Scaffold(
        containerColor = Color(0xFFEEEEEE),
        topBar = {
            Surface(color = Color(0xFFF5F5F5)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = Color(0xFF1A1A1A))
                    }
                    Text(
                        text = e?.name ?: "Notikums",
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCreator) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = Color(0xFF757575))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Rediģēt notikumu") },
                                    onClick = {
                                        showMenu = false
                                        editName = e?.name ?: ""
                                        editPlace = e?.place ?: ""
                                        editDescription = e?.description ?: ""
                                        showEditDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dzēst notikumu", color = Color(0xFFE53935)) },
                                    onClick = { showMenu = false; showDeleteConfirm = true }
                                )
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
                Text("Notikums nav atrasts", color = Color(0xFF757575))
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
                        .background(Color(0xFFF5F5F5))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(modifier = Modifier.size(72.dp), shape = CircleShape, color = RyderAccent) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = Color(0xFF1A1A1A),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            e.name,
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Organizē: ${e.creatorNickname}", color = Color(0xFF757575), fontSize = 13.sp)
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(20.dp)) {
                    EventInfoRow(icon = Icons.Default.Place, label = "Vieta", value = e.place)
                    Spacer(Modifier.height(16.dp))
                    EventInfoRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Datums un laiks",
                        value = formatEventDateTime(e.dateTime)
                    )
                    if (e.description.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        EventInfoRow(icon = Icons.Default.Info, label = "Apraksts", value = e.description)
                    }
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFD9D9D9))
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${e.attendeeIds.size} apmeklētāji",
                                color = Color(0xFF1A1A1A),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            if (isAttending) {
                                Text("Tu apmeklē šo notikumu", color = RyderAccent, fontSize = 12.sp)
                            }
                        }
                        if (currentUser != null) {
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
                                    contentColor = if (isAttending) RyderAccent else Color(0xFF1A1A1A)
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
            containerColor = Color(0xFFF5F5F5),
            title = { Text("Dzēst notikumu?", color = Color(0xFF1A1A1A)) },
            text = { Text("Šo darbību nevar atsaukt.", color = Color(0xFF757575)) },
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
                    Text("Atcelt", color = Color(0xFF757575))
                }
            }
        )
    }

    if (showEditDialog && e != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Color(0xFFF5F5F5),
            title = { Text("Rediģēt notikumu", color = Color(0xFF1A1A1A)) },
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
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updated = e.copy(
                        name = editName.trim().ifEmpty { e.name },
                        place = editPlace.trim().ifEmpty { e.place },
                        description = editDescription.trim()
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
                TextButton(onClick = { showEditDialog = false }) { Text("Atcelt", color = Color(0xFF757575)) }
            }
        )
    }
}

@Composable
private fun EventInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = RyderAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = Color(0xFF757575), fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = Color(0xFF1A1A1A), fontSize = 15.sp)
        }
    }
}

private fun formatEventDateTime(millis: Long): String {
    if (millis == 0L) return "Nav norādīts"
    val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
