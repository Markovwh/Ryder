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
import common.ui.pages.components.RyderRed
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

    LaunchedEffect(eventId) {
        isLoading = true
        try { event = repo.getEvent(eventId) } catch (_: Exception) {}
        isLoading = false
    }

    val e = event
    val isCreator = e != null && currentUser != null && e.creatorId == currentUser.uid
    val isAttending = e != null && currentUser != null && currentUser.uid in e.attendeeIds

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Surface(color = Color(0xFF111111)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = Color.White)
                    }
                    Text(
                        text = e?.name ?: "Notikums",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCreator) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = Color.Gray)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Dzēst notikumu", color = Color(0xFFFF4444)) },
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
                CircularProgressIndicator(color = RyderRed)
            }
            return@Scaffold
        }
        if (e == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Notikums nav atrasts", color = Color.Gray)
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
                        .background(Color(0xFF1A1A1A))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(modifier = Modifier.size(72.dp), shape = CircleShape, color = RyderRed) {
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
                        Text(
                            e.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Organizē: ${e.creatorNickname}", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
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
                    HorizontalDivider(color = Color(0xFF2D2D2D))
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${e.attendeeIds.size} apmeklētāji",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            if (isAttending) {
                                Text("Tu apmeklē šo notikumu", color = RyderRed, fontSize = 12.sp)
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
                                    containerColor = if (isAttending) Color.Transparent else RyderRed,
                                    contentColor = if (isAttending) RyderRed else Color.White
                                ),
                                border = if (isAttending)
                                    androidx.compose.foundation.BorderStroke(1.dp, RyderRed) else null,
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
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Dzēst notikumu?", color = Color.White) },
            text = { Text("Šo darbību nevar atsaukt.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        try { repo.deleteEvent(eventId) } catch (_: Exception) {}
                        onBack()
                    }
                }) { Text("Dzēst", color = Color(0xFFFF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Atcelt", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun EventInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = RyderRed, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = Color.White, fontSize = 15.sp)
        }
    }
}

private fun formatEventDateTime(millis: Long): String {
    if (millis == 0L) return "Nav norādīts"
    val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
