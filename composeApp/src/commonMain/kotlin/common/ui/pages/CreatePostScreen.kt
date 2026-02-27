package common.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import common.model.User
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.OutlinedTextFieldDefaults
import common.data.PostRepository
import common.model.Post
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private val RyderRed = Color(0xFFD32F2F)

@Composable
fun CreatePostScreen(
    currentUser: User,
    onPostCreated: (Post) -> Unit,
    onCancel: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var visibility by remember { mutableStateOf("Public") } // Public / Followers / Private
    val repository = PostRepository()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Top: User profile
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(currentUser.profilePicture ?: "https://picsum.photos/200"),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = currentUser.nickname, color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image/Video placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.DarkGray)
                .clickable { /* TODO: Open file picker */ },
            contentAlignment = Alignment.Center
        ) {
            Text(text = selectedImageUrl?.let { "Change Media" } ?: "Add Photo/Video", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Write a caption...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RyderRed,
                unfocusedBorderColor = Color.Gray,
                cursorColor = RyderRed,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Visibility
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Public", "Followers", "Private").forEach { option ->
                Button(
                    onClick = { visibility = option },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (visibility == option) RyderRed else Color.Gray
                    )
                ) {
                    Text(option, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Post / Cancel buttons
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        // Construct the Post object
                        val post = Post(
                            id = "", // Firestore will assign the ID
                            user = currentUser,
                            mediaUrl = selectedImageUrl ?: "",
                            description = description,
                            visibility = visibility,
                            createdAt = 0L // Will be overwritten in repository
                        )

                        scope.launch {
                            val savedPost = repository.createPost(post)
                            onPostCreated(savedPost)
                        }
                    }
                }
            ) {
                Text("Post", color = Color.White)
            }

            OutlinedButton(onClick = onCancel) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}