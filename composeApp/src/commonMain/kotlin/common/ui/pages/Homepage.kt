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
import common.data.PostRepository
import common.ui.pages.components.PostCard
import ui.pages.components.NavBar
import common.model.Post
import common.model.User
import androidx.compose.runtime.*

private val RyderRed = Color(0xFFD32F2F)

@Composable
fun Homepage(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    isUserLoggedIn: Boolean
) {

    val repository = remember { PostRepository() }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.listenToPosts {
            posts = it
        }
    }

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ryder",
                    color = RyderRed,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (!isUserLoggedIn) {  // ← only show if not logged in
                    TextButton(onClick = onLoginClick) {
                        Text("Login", color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onRegisterClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RyderRed
                        )
                    ) {
                        Text("Sign Up")
                    }
                }
            }

            // Feed
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts) { post ->
                    PostCard(post)
                }
            }
        }
    }
}