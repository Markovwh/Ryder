@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.data.MessageRepository
import common.data.UserRepository
import common.model.Message
import common.model.Post
import common.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ShareToUserDialog(
    post: Post,
    currentUser: User,
    onDismiss: () -> Unit
) {
    val msgRepo = remember { MessageRepository() }
    val userRepo = remember { UserRepository() }
    var query by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingFriends by remember { mutableStateOf(true) }
    var sentToNickname by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val textHint = AppColors.textHint
    val inputBorder = AppColors.inputBorder
    val divider = AppColors.divider

    // Load mutual friends (both follow each other)
    LaunchedEffect(currentUser.uid) {
        isLoadingFriends = true
        try {
            val followers = userRepo.getFollowers(currentUser.uid)
            val followingSet = currentUser.following.toSet()
            friends = followers.filter { it.uid in followingSet }
        } catch (_: Exception) {}
        isLoadingFriends = false
    }

    // Search all users when query >= 2 chars
    LaunchedEffect(query) {
        searchResults = if (query.length >= 2) {
            try { msgRepo.searchUsers(query, currentUser.uid) } catch (_: Exception) { emptyList() }
        } else {
            emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.divider)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Kopīgot",
                color = textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

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
                Spacer(Modifier.height(16.dp))
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
            Spacer(modifier = Modifier.height(12.dp))

            val isSearching = query.length >= 2

            if (isSearching) {
                if (searchResults.isEmpty()) {
                    Text(
                        text = "Nav atrasts neviens lietotājs",
                        color = textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(searchResults) { user ->
                            ShareUserRow(user = user, divider = divider, textPrimary = textPrimary) {
                                sendSharedPost(scope, msgRepo, currentUser, user, post) { sentToNickname = it }
                            }
                        }
                    }
                }
            } else {
                if (isLoadingFriends) {
                    CircularProgressIndicator(
                        color = RyderAccent,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    )
                } else if (friends.isEmpty()) {
                    Text(
                        text = "Nav neviena drauga. Meklē lietotāju augstāk.",
                        color = textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = "Draugi",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(friends) { user ->
                            ShareUserRow(user = user, divider = divider, textPrimary = textPrimary) {
                                sendSharedPost(scope, msgRepo, currentUser, user, post) { sentToNickname = it }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareUserRow(
    user: User,
    divider: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(profilePicture = user.profilePicture, nickname = user.nickname, size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = user.nickname, color = textPrimary, fontSize = 15.sp)
    }
    HorizontalDivider(color = divider)
}

private fun sendSharedPost(
    scope: CoroutineScope,
    msgRepo: MessageRepository,
    currentUser: User,
    targetUser: User,
    post: Post,
    onSent: (String) -> Unit
) {
    scope.launch {
        try {
            val convId = msgRepo.getOrCreateConversation(currentUser, targetUser)
            msgRepo.sendMessage(
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
            onSent(targetUser.nickname)
        } catch (_: Exception) {}
    }
}
