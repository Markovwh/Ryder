@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun CreateEventScreen(
    currentUser: User,
    onCreated: (String) -> Unit,
    onCancel: () -> Unit
) {
    val repo = remember { EventRepository() }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf(12) }
    var selectedMinute by remember { mutableStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(initialHour = 12, initialMinute = 0, is24Hour = true)

    val dateText = if (selectedDateMillis != null) {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(selectedDateMillis!!))
    } else "Izvēlēties datumu"

    val timeText = String.format("%02d:%02d", selectedHour, selectedMinute)

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
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Atcelt", tint = Color(0xFF1A1A1A))
                    }
                    Text(
                        "Izveidot notikumu",
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            when {
                                name.isBlank() -> { error = "Nosaukums nedrīkst būt tukšs"; return@TextButton }
                                place.isBlank() -> { error = "Vieta nedrīkst būt tukša"; return@TextButton }
                                selectedDateMillis == null -> { error = "Izvēlieties datumu"; return@TextButton }
                            }
                            isLoading = true
                            val finalDateTime = selectedDateMillis!! +
                                selectedHour * 3_600_000L +
                                selectedMinute * 60_000L
                            scope.launch {
                                try {
                                    val event = Event(
                                        name = name.trim(),
                                        place = place.trim(),
                                        dateTime = finalDateTime,
                                        description = description.trim(),
                                        creatorId = currentUser.uid,
                                        creatorNickname = currentUser.nickname,
                                        attendeeIds = listOf(currentUser.uid)
                                    )
                                    val created = repo.createEvent(event)
                                    onCreated(created.id)
                                } catch (e: Exception) {
                                    error = "Kļūda: ${e.message}"
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            "Izveidot",
                            color = if (!isLoading) RyderAccent else Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Nosaukums *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = place,
                onValueChange = { place = it; error = null },
                label = { Text("Vieta *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                leadingIcon = {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF757575))
                },
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Date picker row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (selectedDateMillis != null) RyderAccent else Color(0xFF9E9E9E),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (selectedDateMillis != null) RyderAccent else Color(0xFF757575),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    dateText,
                    color = if (selectedDateMillis != null) Color(0xFF1A1A1A) else Color(0xFF757575),
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Time picker row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF9E9E9E), RoundedCornerShape(12.dp))
                    .clickable { showTimePicker = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(timeText, color = Color(0xFF1A1A1A), fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Apraksts") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                maxLines = 6
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Color(0xFFE53935), fontSize = 13.sp)
            }

            if (isLoading) {
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RyderAccent)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK", color = RyderAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Atcelt", color = Color(0xFF757575))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = RyderAccent,
                    todayDateBorderColor = RyderAccent
                )
            )
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Color(0xFFF5F5F5),
            title = { Text("Izvēlēties laiku", color = Color(0xFF1A1A1A)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            selectorColor = RyderAccent
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK", color = RyderAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Atcelt", color = Color(0xFF757575))
                }
            }
        )
    }
}
