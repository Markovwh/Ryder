@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
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
import common.ui.pages.components.RyderRed
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
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> selectedUris = uris }

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
        containerColor = Color.Black,
        topBar = {
            Surface(color = Color(0xFF111111), tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = Color.White)
                    }
                    val pic = chat.otherUserPicture
                    if (pic != null) {
                        Image(
                            painter = rememberAsyncImagePainter(pic),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = RyderRed) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = chat.otherUserNickname.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = chat.otherUserNickname,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF111111)) {
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
                                        .background(Color.DarkGray),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (selectedUris.size > 4) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2A2A2A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+${selectedUris.size - 4}", color = Color.White, fontSize = 14.sp)
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
                            Icon(Icons.Default.Image, contentDescription = "Pievienot mediju", tint = Color.Gray)
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Raksti ziņu...", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
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
                                tint = if (isSending) Color.Gray else RyderRed
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
                MessageBubble(message = message, isMine = isMine)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean) {
    val bubbleColor = if (isMine) RyderRed else Color(0xFF2A2A2A)
    val alignment = if (isMine) Alignment.End else Alignment.Start
    val shape = if (isMine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
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
                            .background(Color.DarkGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            if (message.text.isNotEmpty()) {
                Text(text = message.text, color = Color.White, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = PostCardTimeFormatter.formatTimeAgo(message.createdAt),
            color = Color.Gray,
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
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(8.dp)
    ) {
        Text(
            text = "📸 $nickname",
            color = Color.White,
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
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )
        }
        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (description.length > 80) description.take(80) + "..." else description,
                color = Color.LightGray,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
    }
}
