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
import common.data.UserRepository
import common.model.Post
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.PostCard
import common.ui.pages.components.RyderAccent

@Composable
fun Homepage(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    isUserLoggedIn: Boolean,
    currentUser: User? = null
) {
    val repository = remember { PostRepository() }
    val userRepo = remember { UserRepository() }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var followingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var followerIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Fetch who the current user follows and who follows them so the feed can
    // apply "Privāts" / "Draugi" visibility rules correctly.
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            followingIds = currentUser.following.toSet()
            try { followerIds = userRepo.getFollowerIds(uid) } catch (_: Exception) {}
        } else {
            followingIds = emptySet()
            followerIds = emptySet()
        }
    }

    // Re-subscribe whenever the viewer's relationship sets change so the feed
    // immediately reflects new follows/unfollows.
    DisposableEffect(currentUser?.uid, followingIds, followerIds) {
        val unsub = repository.listenToPosts(
            currentUserId = currentUser?.uid,
            followingIds = followingIds,
            followerIds = followerIds
        ) { posts = it }
        onDispose { unsub() }
    }

    Scaffold(containerColor = AppColors.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ryder",
                    color = RyderAccent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!isUserLoggedIn) {
                    TextButton(onClick = onLoginClick) {
                        Text("Pieslēgties", color = AppColors.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRegisterClick,
                        colors = ButtonDefaults.buttonColors(containerColor = RyderAccent)
                    ) {
                        Text("Reģistrēties", color = Color.White)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(posts) { post ->
                    PostCard(post = post, currentUser = currentUser)
                }
            }
        }
    }
}
