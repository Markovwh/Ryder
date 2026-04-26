package common.ui.pages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun ScrollTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary

    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surface,
        title = { Text("Izvēlēties laiku", color = textPrimary, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeColumn(
                        values = (0..23).toList(),
                        selected = hour,
                        onSelected = { hour = it },
                        textSecondary = textSecondary
                    )
                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    TimeColumn(
                        values = (0..59).toList(),
                        selected = minute,
                        onSelected = { minute = it },
                        textSecondary = textSecondary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = RyderAccent
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) {
                Text("OK", color = RyderAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Atcelt", color = textSecondary)
            }
        }
    )
}

@Composable
private fun TimeColumn(
    values: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    val itemHeightDp = 44.dp
    val visibleCount = 5
    val paddedValues = listOf(-1, -1) + values + listOf(-1, -1)

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selected.coerceIn(0, values.size - 1)
    )
    val flingBehavior = rememberSnapFlingBehavior(listState)

    val currentSelected by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex.coerceIn(0, values.size - 1)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            onSelected(listState.firstVisibleItemIndex.coerceIn(0, values.size - 1))
        }
    }

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(itemHeightDp * visibleCount)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeightDp)
                .background(RyderAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
        )
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(paddedValues) { index, value ->
                val dist = abs(index - (currentSelected + 2))
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    if (value >= 0) {
                        Text(
                            text = String.format("%02d", value),
                            fontSize = when (dist) { 0 -> 28.sp; 1 -> 22.sp; else -> 18.sp },
                            fontWeight = if (dist == 0) FontWeight.Bold else FontWeight.Normal,
                            color = when (dist) {
                                0 -> RyderAccent
                                1 -> textSecondary.copy(alpha = 0.7f)
                                else -> textSecondary.copy(alpha = 0.3f)
                            }
                        )
                    }
                }
            }
        }
    }
}
