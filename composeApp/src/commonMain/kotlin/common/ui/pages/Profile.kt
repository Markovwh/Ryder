package common.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import common.data.AuthService
import common.model.User

private val RyderRed = Color(0xFFD32F2F)

@Composable
fun ProfilePage(authService: AuthService) {

    var user by remember { mutableStateOf<User?>(null) }
    val userPosts = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val uid = authService.getCurrentUserId() ?: return@LaunchedEffect
        val result = authService.getUserData(uid)
        if (result.isSuccess) {
            user = result.getOrNull()
            // Mock posts
            userPosts.addAll(
                listOf(
                    "https://picsum.photos/300/300?random=1",
                    "https://picsum.photos/300/300?random=2",
                    "https://picsum.photos/300/300?random=3",
                    "https://picsum.photos/300/300?random=4",
                    "https://picsum.photos/300/300?random=5"
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            // Profile info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile picture
                Image(
                    painter = rememberAsyncImagePainter(
                        model = user?.profilePicture ?: "https://picsum.photos/200"
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Username
                Text(
                    text = user?.nickname ?: "Username",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Full name
                Text(
                    text = "${user?.firstName ?: ""} ${user?.lastName ?: ""}",
                    color = Color.Gray,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileStat(count = "120", label = "Posts")
                    ProfileStat(count = "450", label = "Followers")
                    ProfileStat(count = "300", label = "Following")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Edit Profile button
                Button(
                    onClick = { /* TODO: navigate to edit */ },
                    colors = ButtonDefaults.buttonColors(containerColor = RyderRed),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(40.dp)
                ) {
                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Posts grid
        items(userPosts.chunked(3)) { rowPosts ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (postUrl in rowPosts) {
                    Image(
                        painter = rememberAsyncImagePainter(postUrl),
                        contentDescription = "User Post",
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(Color.DarkGray),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.ProfileStat(count: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(text = count, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, color = Color.Gray)
    }
}