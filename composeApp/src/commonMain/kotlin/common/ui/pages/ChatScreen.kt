@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package common.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import common.data.MessageRepository
import common.model.Message
import common.model.User
import common.data.NotificationRepository
import common.model.AppNotification
import common.ui.pages.components.AppColors
import common.ui.pages.components.PostCardTimeFormatter
import common.ui.pages.components.RyderAccent
import common.ui.pages.components.UserAvatar
import common.ui.pages.components.VideoPlayer
import common.ui.pages.components.isVideoUrl
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chat: Screen.Chat,
    currentUser: User?,
    onBack: () -> Unit,
    onOpenUser: ((String) -> Unit)? = null
) {
    val repo = remember { MessageRepository() }
    val notifRepo = remember { NotificationRepository() }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSending by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showDeleteConversationConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val bg = AppColors.background
    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val inputBorder = AppColors.inputBorder
    val textHint = AppColors.textHint

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> selectedUris = uris }

    LaunchedEffect(chat.conversationId, currentUser?.uid) {
        val cu = currentUser ?: return@LaunchedEffect
        val otherUser = User(
            uid = chat.otherUserId,
            nickname = chat.otherUserNickname,
            profilePicture = chat.otherUserPicture
        )
        try { repo.getOrCreateConversation(cu, otherUser) } catch (_: Exception) {}
    }

    DisposableEffect(chat.conversationId) {
        val unsub = repo.listenToMessages(chat.conversationId) { msgs ->
            messages = msgs
            scope.launch {
                if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1)
            }
        }
        onDispose { unsub() }
    }

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
                    val headerClickModifier = if (onOpenUser != null)
                        Modifier.weight(1f).clickable { onOpenUser(chat.otherUserId) }
                    else Modifier.weight(1f)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = headerClickModifier
                    ) {
                        UserAvatar(
                            profilePicture = chat.otherUserPicture,
                            nickname = chat.otherUserNickname,
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = chat.otherUserNickname,
                            color = textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = textSecondary)
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Dzēst sarunu", color = Color(0xFFE53935)) },
                                onClick = { showTopMenu = false; showDeleteConversationConfirm = true }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = surface) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    if (selectedUris.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedUris.take(4).forEach { uri ->
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.avatarPlaceholder),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (selectedUris.size > 4) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+${selectedUris.size - 4}", color = textPrimary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            mediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "Pievienot mediju", tint = textSecondary)
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Raksti ziņu...", color = textHint) },
                            modifier = Modifier.weight(1f),
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
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            enabled = !isSending && (inputText.isNotBlank() || selectedUris.isNotEmpty()),
                            onClick = {
                                val uid = currentUser?.uid ?: return@IconButton
                                val text = inputText.trim()
                                val uris = selectedUris.toList()
                                inputText = ""
                                selectedUris = emptyList()
                                isSending = true
                                scope.launch {
                                    try {
                                        val mediaUrls = uris.map { uri ->
                                            repo.uploadMessageMedia(uri, uid, context)
                                        }
                                        repo.sendMessage(
                                            chat.conversationId,
                                            Message(
                                                senderId = uid,
                                                text = text,
                                                mediaUrls = mediaUrls
                                            )
                                        )
                                    } catch (_: Exception) {
                                        isSending = false
                                        return@launch
                                    }
                                    // Notification send is best-effort — a failure must not
                                    // surface as a message-send failure to the user.
                                    try {
                                        notifRepo.send(AppNotification(
                                            recipientId = chat.otherUserId,
                                            senderId = uid,
                                            senderNickname = currentUser?.nickname ?: "",
                                            senderPicture = currentUser?.profilePicture ?: "",
                                            type = "message",
                                            conversationId = chat.conversationId
                                        ))
                                    } catch (_: Exception) {}
                                    isSending = false
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Sūtīt",
                                tint = if (isSending) textSecondary else RyderAccent
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                val isMine = message.senderId == currentUser?.uid
                MessageBubble(
                    message = message,
                    isMine = isMine,
                    onOpenUser = onOpenUser,
                    onDelete = if (isMine) {
                        {
                            scope.launch {
                                try {
                                    repo.deleteMessage(chat.conversationId, message.id)
                                    messages = messages.filter { it.id != message.id }
                                } catch (_: Exception) {}
                            }
                        }
                    } else null
                )
            }
        }
    }

    if (showDeleteConversationConfirm) {
        val dialogSurface = AppColors.surface
        val dialogTextPrimary = AppColors.textPrimary
        AlertDialog(
            onDismissRequest = { showDeleteConversationConfirm = false },
            containerColor = dialogSurface,
            title = { Text("Dzēst sarunu?", color = dialogTextPrimary) },
            text = { Text("Visa saruna un ziņas tiks neatgriezeniski dzēstas.", color = AppColors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConversationConfirm = false
                    scope.launch {
                        try { repo.deleteConversation(chat.conversationId) } catch (_: Exception) {}
                        onBack()
                    }
                }) { Text("Dzēst", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConversationConfirm = false }) {
                    Text("Atcelt", color = AppColors.textSecondary)
                }
            }
        )
    }
}

// ── Message bubble ─────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    onDelete: (() -> Unit)? = null,
    onOpenUser: ((String) -> Unit)? = null
) {
    val bubbleColor = if (isMine) RyderAccent else AppColors.tileBackground
    val textColor = if (isMine) Color.White else AppColors.textPrimary
    val subTextColor = if (isMine) Color.White.copy(alpha = 0.75f) else AppColors.textSecondary
    val alignment = if (isMine) Alignment.End else Alignment.Start
    val shape = if (isMine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }
    var showMenu by remember { mutableStateOf(false) }
    var showSharedPostDialog by remember { mutableStateOf(false) }
    var previewUrl by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(shape)
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { if (onDelete != null) showMenu = true }
                    )
                    .padding(10.dp)
            ) {
                if (message.hasSharedPost) {
                    SharedPostCard(
                        nickname = message.sharedPostUserNickname,
                        userId = message.sharedPostUserId,
                        userPicture = message.sharedPostUserPicture.takeIf { it.isNotEmpty() },
                        description = message.sharedPostDescription,
                        mediaUrl = message.sharedPostMediaUrl.takeIf { it.isNotEmpty() },
                        isMine = isMine,
                        onClick = { showSharedPostDialog = true }
                    )
                    if (message.text.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                }
                if (message.mediaUrls.isNotEmpty()) {
                    message.mediaUrls.forEach { url ->
                        if (url.isVideoUrl()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                VideoPlayer(url = url, modifier = Modifier.fillMaxSize())
                                // Transparent overlay: intercepts tap to open preview
                                // (VideoPlayer's own play/pause is handled inside the dialog)
                                Box(modifier = Modifier.fillMaxSize().clickable { previewUrl = url })
                            }
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.avatarPlaceholder)
                                    .clickable { previewUrl = url },
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                if (message.text.isNotEmpty()) {
                    Text(text = message.text, color = textColor, fontSize = 14.sp)
                }
            }
            if (onDelete != null) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Dzēst", color = Color(0xFFE53935)) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = PostCardTimeFormatter.formatTimeAgo(message.createdAt),
            color = AppColors.textSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
    }

    if (showSharedPostDialog) {
        SharedPostDetailDialog(
            nickname = message.sharedPostUserNickname,
            userId = message.sharedPostUserId,
            userPicture = message.sharedPostUserPicture.takeIf { it.isNotEmpty() },
            description = message.sharedPostDescription,
            mediaUrl = message.sharedPostMediaUrl.takeIf { it.isNotEmpty() },
            onDismiss = { showSharedPostDialog = false },
            onOpenUser = if (message.sharedPostUserId.isNotEmpty() && onOpenUser != null) {
                {
                    showSharedPostDialog = false
                    onOpenUser(message.sharedPostUserId)
                }
            } else null
        )
    }

    previewUrl?.let { url ->
        MediaPreviewDialog(url = url, onDismiss = { previewUrl = null })
    }
}

// ── Shared post card (inside bubble) ──────────────────────────────────────────

@Composable
private fun SharedPostCard(
    nickname: String,
    userId: String,
    userPicture: String?,
    description: String,
    mediaUrl: String?,
    isMine: Boolean,
    onClick: () -> Unit
) {
    val overlayBg = if (isMine)
        Color.White.copy(alpha = 0.15f)
    else
        Color.Black.copy(alpha = 0.06f)
    val textColor = if (isMine) Color.White else AppColors.textPrimary
    val subTextColor = if (isMine) Color.White.copy(alpha = 0.8f) else AppColors.textSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(overlayBg)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top: avatar + nickname
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                profilePicture = userPicture,
                nickname = nickname,
                size = 22.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = nickname,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Middle: image
        if (mediaUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(mediaUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.avatarPlaceholder),
                contentScale = ContentScale.Crop
            )
        }
        // Bottom: description
        if (description.isNotEmpty()) {
            Text(
                text = if (description.length > 100) description.take(100) + "…" else description,
                color = subTextColor,
                fontSize = 11.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Shared post detail dialog ──────────────────────────────────────────────────

@Composable
private fun SharedPostDetailDialog(
    nickname: String,
    userId: String,
    userPicture: String?,
    description: String,
    mediaUrl: String?,
    onDismiss: () -> Unit,
    onOpenUser: (() -> Unit)?
) {
    val surface = AppColors.surface
    val divider = AppColors.divider
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(surface)
                .border(1.5.dp, RyderAccent, RoundedCornerShape(16.dp))
        ) {
            // Header: avatar + nickname (clickable) + close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val authorModifier = if (onOpenUser != null)
                    Modifier.weight(1f).clickable(onClick = onOpenUser)
                else Modifier.weight(1f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = authorModifier
                ) {
                    UserAvatar(profilePicture = userPicture, nickname = nickname, size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = nickname,
                            color = textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        if (onOpenUser != null) {
                            Text(
                                text = "Skatīt profilu →",
                                color = RyderAccent,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Aizvērt", tint = textPrimary)
                }
            }

            HorizontalDivider(color = divider)

            // Media
            if (mediaUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(mediaUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 320.dp)
                        .background(AppColors.avatarPlaceholder),
                    contentScale = ContentScale.Crop
                )
            }

            // Description
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    color = textPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            } else if (mediaUrl == null) {
                Text(
                    text = "Šajā ierakstā nav satura.",
                    color = textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

// ── Media preview dialog ───────────────────────────────────────────────────────

@Composable
private fun MediaPreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            if (url.isVideoUrl()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    VideoPlayer(url = url, modifier = Modifier.fillMaxSize())
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(url),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {},
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Aizvērt", tint = Color.White)
            }
        }
    }
}
