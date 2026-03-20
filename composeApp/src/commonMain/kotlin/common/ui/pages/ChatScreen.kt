@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package common.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
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
import common.data.MessageRepository
import common.model.Message
import common.model.User
import common.ui.pages.components.PostCardTimeFormatter
import common.ui.pages.components.RyderAccent
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    chat: Screen.Chat,
    currentUser: User?,
    onBack: () -> Unit
) {
    val repo = remember { MessageRepository() }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSending by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showDeleteConversationConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> selectedUris = uris }

    // Ensure the conversation document exists with full participant data.
    // Also keyed on currentUser?.uid so it retries if currentUser loads after the screen opens.
    LaunchedEffect(chat.conversationId, currentUser?.uid) {
        val cu = currentUser ?: return@LaunchedEffect
        val otherUser = common.model.User(
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
        containerColor = Color(0xFFEEEEEE),
        topBar = {
            Surface(color = Color(0xFFF5F5F5), tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = Color(0xFF1A1A1A))
                    }
                    val pic = chat.otherUserPicture
                    if (pic != null) {
                        Image(
                            painter = rememberAsyncImagePainter(pic),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD0D0D0)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = RyderAccent) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = chat.otherUserNickname.take(1).uppercase(),
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = chat.otherUserNickname,
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = Color(0xFF757575))
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
            Surface(color = Color(0xFFF5F5F5)) {
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
                                        .background(Color(0xFFD0D0D0)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (selectedUris.size > 4) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEEEEEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+${selectedUris.size - 4}", color = Color(0xFF1A1A1A), fontSize = 14.sp)
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
                            Icon(Icons.Default.Image, contentDescription = "Pievienot mediju", tint = Color(0xFF757575))
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Raksti ziņu...", color = Color(0xFF9E9E9E)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1A1A1A),
                                unfocusedTextColor = Color(0xFF1A1A1A),
                                focusedBorderColor = RyderAccent,
                                unfocusedBorderColor = Color(0xFF9E9E9E),
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
                                            repo.uploadMessageMedia(uri, uid)
                                        }
                                        repo.sendMessage(
                                            chat.conversationId,
                                            Message(
                                                senderId = uid,
                                                text = text,
                                                mediaUrls = mediaUrls
                                            )
                                        )
                                    } catch (_: Exception) {}
                                    isSending = false
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Sūtīt",
                                tint = if (isSending) Color(0xFF9E9E9E) else RyderAccent
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

    // Delete entire conversation dialog
    if (showDeleteConversationConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConversationConfirm = false },
            containerColor = Color(0xFFF5F5F5),
            title = { Text("Dzēst sarunu?", color = Color(0xFF1A1A1A)) },
            text = { Text("Visa saruna un ziņas tiks neatgriezeniski dzēstas.", color = Color(0xFF757575)) },
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
                TextButton(onClick = { showDeleteConversationConfirm = false }) { Text("Atcelt", color = Color(0xFF757575)) }
            }
        )
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean, onDelete: (() -> Unit)? = null) {
    val bubbleColor = if (isMine) RyderAccent else Color(0xFFE0E0E0)
    val alignment = if (isMine) Alignment.End else Alignment.Start
    val shape = if (isMine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }
    var showMenu by remember { mutableStateOf(false) }

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
                    SharedPostPreview(
                        nickname = message.sharedPostUserNickname,
                        description = message.sharedPostDescription,
                        mediaUrl = message.sharedPostMediaUrl.takeIf { it.isNotEmpty() }
                    )
                    if (message.text.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                }
                if (message.mediaUrls.isNotEmpty()) {
                    message.mediaUrls.forEach { url ->
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD0D0D0)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                if (message.text.isNotEmpty()) {
                    Text(text = message.text, color = Color(0xFF1A1A1A), fontSize = 14.sp)
                }
            }
            if (onDelete != null) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Dzēst", color = Color(0xFFE53935)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = PostCardTimeFormatter.formatTimeAgo(message.createdAt),
            color = Color(0xFF757575),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun SharedPostPreview(
    nickname: String,
    description: String,
    mediaUrl: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF000000).copy(alpha = 0.08f))
            .padding(8.dp)
    ) {
        Text(
            text = "📸 $nickname",
            color = Color(0xFF1A1A1A),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
        if (mediaUrl != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Image(
                painter = rememberAsyncImagePainter(mediaUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFD0D0D0)),
                contentScale = ContentScale.Crop
            )
        }
        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (description.length > 80) description.take(80) + "..." else description,
                color = Color(0xFF757575),
                fontSize = 12.sp,
                maxLines = 2
            )
        }
    }
}
