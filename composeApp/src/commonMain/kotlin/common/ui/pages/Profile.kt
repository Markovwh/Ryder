package common.ui.pages

import androidx.compose.foundation.background
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
fun ProfilePage() {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {

        item {
            ProfileHeader()
        }

        items(dummyProfilePosts()) { post ->
            ProfilePostCard(post)
        }
    }
}

@Composable
private fun ProfileHeader() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {

        // Avatar + stats
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = RyderRed
            ) {}

            Spacer(modifier = Modifier.width(20.dp))

            ProfileStat(label = "Posts", value = "12")
            ProfileStat(label = "Followers", value = "340")
            ProfileStat(label = "Following", value = "180")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Username
        Text(
            text = "Rider123",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Bio
        Text(
            text = "Living life one ride at a time.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Divider(color = Color.DarkGray)
    }
}

@Composable
private fun ProfileStat(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ProfilePostCard(
    text: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF111111)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

private fun dummyProfilePosts(): List<String> {
    return listOf(
        "Sunset ride through the hills.",
        "New tires installed today.",
        "Weekend ride with the crew.",
        "Nothing beats open roads and loud pipes."
    )
}
