@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import common.data.MessageRepository
import common.model.Message
import common.model.Post
import common.model.User
import kotlinx.coroutines.launch

@Composable
fun ShareToUserDialog(
    post: Post,
    currentUser: User,
    onDismiss: () -> Unit
) {
    val repo = remember { MessageRepository() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<User>>(emptyList()) }
    var sentToNickname by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        results = if (query.length >= 2) {
            try { repo.searchUsers(query, currentUser.uid) } catch (_: Exception) { emptyList() }
        } else {
            emptyList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A))
                .padding(16.dp)
        ) {
            Text(
                text = "Sūtīt draugam",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (sentToNickname != null) {
                Text(
                    text = "Nosūtīts uz $sentToNickname!",
                    color = RyderRed,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Aizvērt", color = Color.White)
                }
                return@Column
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Meklēt lietotāju...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RyderRed,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = RyderRed
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (results.isEmpty() && query.length >= 2) {
                Text(
                    text = "Nav atrasts neviens lietotājs",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            results.forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                try {
                                    val convId = repo.getOrCreateConversation(currentUser, user)
                                    repo.sendMessage(
                                        convId,
                                        Message(
                                            senderId = currentUser.uid,
                                            sharedPostId = post.id,
                                            sharedPostDescription = post.description,
                                            sharedPostMediaUrl = post.mediaUrls.firstOrNull() ?: "",
                                            sharedPostUserNickname = post.user.nickname
                                        )
                                    )
                                    sentToNickname = user.nickname
                                } catch (_: Exception) {}
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pic = user.profilePicture
                    if (pic != null) {
                        Image(
                            painter = rememberAsyncImagePainter(pic),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = RyderRed) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.nickname.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = user.nickname, color = Color.White, fontSize = 15.sp)
                }
                HorizontalDivider(color = Color(0xFF2D2D2D))
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Atcelt", color = Color.Gray)
            }
        }
    }
}
