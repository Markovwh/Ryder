package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.data.PostRepository
import common.model.Post
import common.model.User
import common.ui.pages.components.PostCard

private val RyderRed = Color(0xFFD32F2F)

@Composable
fun Homepage(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    isUserLoggedIn: Boolean,
    currentUser: User? = null
) {
    val repository = remember { PostRepository() }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.listenToPosts { posts = it }
    }

    Scaffold(containerColor = Color.Black) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ryder",
                    color = RyderRed,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!isUserLoggedIn) {
                    TextButton(onClick = onLoginClick) {
                        Text("Pieslēgties", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRegisterClick,
                        colors = ButtonDefaults.buttonColors(containerColor = RyderRed)
                    ) {
                        Text("Reģistrēties")
                    }
                }
            }

            // Feed
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(posts) { post ->
                    PostCard(post = post, currentUser = currentUser)
                }
            }
        }
    }
}
