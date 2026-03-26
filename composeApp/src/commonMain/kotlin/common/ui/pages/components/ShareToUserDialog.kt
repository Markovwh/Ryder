@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val textHint = AppColors.textHint
    val inputBorder = AppColors.inputBorder
    val divider = AppColors.divider

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
                .background(surface)
                .padding(16.dp)
        ) {
            Text(
                text = "Sūtīt draugam",
                color = textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (sentToNickname != null) {
                Text(
                    text = "Nosūtīts uz $sentToNickname!",
                    color = RyderAccent,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Aizvērt", color = textSecondary)
                }
                return@Column
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Meklēt lietotāju...", color = textHint) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedBorderColor = RyderAccent,
                    unfocusedBorderColor = inputBorder,
                    cursorColor = RyderAccent
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (results.isEmpty() && query.length >= 2) {
                Text(
                    text = "Nav atrasts neviens lietotājs",
                    color = textSecondary,
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
                                            sharedPostUserNickname = post.user.nickname,
                                            sharedPostUserId = post.userId,
                                            sharedPostUserPicture = post.user.profilePicture ?: ""
                                        )
                                    )
                                    sentToNickname = user.nickname
                                } catch (_: Exception) {}
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(profilePicture = user.profilePicture, nickname = user.nickname, size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = user.nickname, color = textPrimary, fontSize = 15.sp)
                }
                HorizontalDivider(color = divider)
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Atcelt", color = textSecondary)
            }
        }
    }
}
