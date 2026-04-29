package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.data.NotificationRepository
import common.model.AppNotification
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.PostCardTimeFormatter
import common.ui.pages.components.RyderAccent
import common.ui.pages.components.UserAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsPage(
    currentUser: User,
    onBack: () -> Unit,
    onOpenUser: (userId: String, nickname: String) -> Unit,
    onOpenChat: (Screen.Chat) -> Unit
) {
    val repo = remember { NotificationRepository() }
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }

    DisposableEffect(currentUser.uid) {
        val unsub = repo.listen(currentUser.uid) { list ->
            // Opening this page means all visible notifications are seen — show them as read immediately.
            notifications = list.map { it.copy(isRead = true) }
        }
        onDispose { unsub() }
    }

    Scaffold(
        containerColor = AppColors.background,
        topBar = {
            TopAppBar(
                title = { Text("Paziņojumi", color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = AppColors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.surface)
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nav paziņojumu", color = AppColors.textSecondary, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationItem(
                        notification = notif,
                        onClick = {
                            scope.launch {
                                when (notif.type) {
                                    "message" -> onOpenChat(
                                        Screen.Chat(
                                            conversationId = notif.conversationId,
                                            otherUserId = notif.senderId,
                                            otherUserNickname = notif.senderNickname,
                                            otherUserPicture = notif.senderPicture.takeIf { it.isNotEmpty() }
                                        )
                                    )
                                    else -> onOpenUser(notif.senderId, notif.senderNickname)
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: AppNotification, onClick: () -> Unit) {
    val bg = if (!notification.isRead) AppColors.surface else AppColors.background

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            UserAvatar(
                profilePicture = notification.senderPicture.takeIf { it.isNotEmpty() },
                nickname = notification.senderNickname,
                size = 44.dp
            )
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(RyderAccent)
                        .align(Alignment.TopEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notificationText(notification),
                color = AppColors.textPrimary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = PostCardTimeFormatter.formatTimeAgo(notification.createdAt),
                color = AppColors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

private fun notificationText(n: AppNotification): String = when (n.type) {
    "follow"         -> "${n.senderNickname} sāka tev sekot"
    "follow_request" -> "${n.senderNickname} vēlas tev sekot"
    "comment"        -> if (n.commentPreview.isNotEmpty())
        "${n.senderNickname} komentēja tavu ierakstu: \"${n.commentPreview}\""
    else
        "${n.senderNickname} komentēja tavu ierakstu"
    "message"        -> "${n.senderNickname} nosūtīja tev ziņu"
    else             -> n.senderNickname
}
