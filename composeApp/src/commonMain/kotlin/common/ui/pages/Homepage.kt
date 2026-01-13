package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.pages.components.NavBar
import common.ui.pages.components.PostCard

// Galvenā prgrammas akcenta krāsa - sarkana
private val RyderRed = Color(0xFFD32F2F)

@Composable
fun Homepage() {

    // Nosaka navigācijas joslas elementu darbību
    Scaffold(
        bottomBar = {
            NavBar(
                onHome = { },
                onSearch = { },
                onMessages = { },
                onProfile = { }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Lietotnes nosaukums ekrāna augšā
            Text(
                text = "Ryder",
                color = RyderRed,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(16.dp)
            )

            // Placeholder feed
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(placeholderPosts()) { post ->
                    PostCard(post)
                }
            }
        }
    }
}

public fun placeholderPosts(): List<String> {
    return listOf(
        "Just finished a 200-mile ride through the canyon. Unreal views.",
        "Anyone riding out this weekend?",
        "New exhaust installed. Sounds mean.",
        "Looking for group rides near Austin."
    )
}
