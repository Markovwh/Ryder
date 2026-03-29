package common.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.data.PostRepository
import common.model.Post
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.PostCard
import common.ui.pages.components.RyderAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagFeedPage(
    hashtag: String,
    currentUser: User?,
    onBack: () -> Unit
) {
    val repository = remember { PostRepository() }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(hashtag) {
        isLoading = true
        try { posts = repository.getPostsByHashtag(hashtag) } catch (_: Exception) {}
        isLoading = false
    }

    val bg = AppColors.background
    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary

    Scaffold(
        containerColor = bg,
        topBar = {
            Surface(color = surface, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = textPrimary)
                    }
                    Text(
                        text = "#$hashtag",
                        color = RyderAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = RyderAccent) }
            posts.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Nav ierakstu ar #$hashtag", color = textSecondary, fontSize = 14.sp) }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "${posts.size} ierakst${if (posts.size == 1) "s" else "i"}",
                        color = textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(posts, key = { it.id }) { post ->
                    PostCard(post = post, currentUser = currentUser)
                }
            }
        }
    }
}
