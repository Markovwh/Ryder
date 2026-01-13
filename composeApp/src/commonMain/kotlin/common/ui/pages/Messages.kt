package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.ui.pages.components.RyderRed

@Composable
fun MessagesPage() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // Header
        Text(
            text = "Messages",
            color = RyderRed,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(24.dp)
        )

        Divider(color = Color.DarkGray)

        // Conversations list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(dummyMessages()) { message ->
                MessageRow(
                    username = message.username,
                    lastMessage = message.lastMessage,
                    timestamp = message.time
                )
            }
        }
    }
}

@Composable
private fun MessageRow(
    username: String,
    lastMessage: String,
    timestamp: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Avatar placeholder
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = RyderRed
        ) {}

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = username,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = lastMessage,
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = timestamp,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }

    Divider(
        color = Color(0xFF1E1E1E),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 84.dp)
    )
}

private data class MessagePreview(
    val username: String,
    val lastMessage: String,
    val time: String
)

private fun dummyMessages(): List<MessagePreview> {
    return listOf(
        MessagePreview("IronRider", "You riding out tonight?", "2m"),
        MessagePreview("NightWolf", "That build looks clean", "15m"),
        MessagePreview("RoadQueen", "Meet at the gas station", "1h"),
        MessagePreview("ThrottleKing", "Route’s ready, check it out", "3h"),
        MessagePreview("MotoLife", "Weekend plans?", "Yesterday")
    )
}
