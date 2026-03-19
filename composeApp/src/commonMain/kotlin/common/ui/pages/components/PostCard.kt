@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import common.data.PostRepository
import common.model.Comment
import common.model.Post
import common.model.User
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private val CardBackground = Color(0xFF1A1A1A)
private val DividerColor = Color(0xFF2D2D2D)

@Composable
fun PostCard(post: Post, currentUser: User?) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.likeCount) }
    var showComments by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showShareOptions by remember { mutableStateOf(false) }
    var showShareToUser by remember { mutableStateOf(false) }

    val repository = remember { PostRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Load whether current user has liked this post
    LaunchedEffect(post.id, currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        if (post.id.isNotEmpty()) {
            try { isLiked = repository.isLiked(post.id, uid) } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.5.dp, RyderRed, RoundedCornerShape(14.dp))
    ) {
        Column {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        post.user.profilePicture ?: "https://picsum.photos/200"
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.user.nickname,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    val time = PostCardTimeFormatter.formatTimeAgo(post.createdAt)
                    if (time.isNotEmpty()) {
                        Text(text = time, color = Color.Gray, fontSize = 11.sp)
                    }
                }
                // Three-dot menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Vairāk",
                            tint = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ziņot") },
                            onClick = { showMenu = false; showReportDialog = true }
                        )
                    }
                }
            }

            // ── Media ─────────────────────────────────────────────────────────
            if (post.mediaUrls.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })
                Box {
                    HorizontalPager(state = pagerState) { page ->
                        Image(
                            painter = rememberAsyncImagePainter(post.mediaUrls[page]),
                            contentDescription = "Ziņas medijs",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (post.mediaUrls.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(post.mediaUrls.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index) Color.White
                                            else Color.White.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // ── Description ───────────────────────────────────────────────────
            if (post.description.isNotEmpty()) {
                Text(
                    text = post.description,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 8.dp))

            // ── Action row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Like
                PostActionButton(
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    tint = if (isLiked) RyderRed else Color.Gray,
                    label = if (likeCount > 0) likeCount.toString() else ""
                ) {
                    val uid = currentUser?.uid ?: return@PostActionButton
                    val newLiked = !isLiked
                    isLiked = newLiked
                    likeCount += if (newLiked) 1 else -1
                    scope.launch {
                        try {
                            repository.toggleLike(post.id, uid, newLiked)
                        } catch (_: Exception) {
                            isLiked = !newLiked
                            likeCount += if (newLiked) -1 else 1
                        }
                    }
                }

                // Comment
                PostActionButton(
                    icon = Icons.Default.ChatBubble,
                    tint = Color.Gray,
                    label = if (post.commentCount > 0) post.commentCount.toString() else ""
                ) { showComments = true }

                // Share
                PostActionButton(
                    icon = Icons.Default.Share,
                    tint = Color.Gray,
                    label = ""
                ) {
                    if (currentUser != null) {
                        showShareOptions = true
                    } else {
                        val shareText = buildString {
                            if (post.description.isNotEmpty()) append(post.description).append("\n")
                            append("Apskatiet Ryder lietotnē!")
                        }
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                },
                                "Dalīties"
                            )
                        )
                    }
                }
            }
        }
    }

    // ── Comments bottom sheet ─────────────────────────────────────────────────
    if (showComments) {
        CommentsSheet(
            post = post,
            currentUser = currentUser,
            repository = repository,
            onDismiss = { showComments = false }
        )
    }

    // ── Report dialog ─────────────────────────────────────────────────────────
    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason ->
                showReportDialog = false
                val uid = currentUser?.uid ?: return@ReportDialog
                scope.launch {
                    try { repository.reportPost(post.id, uid, reason) } catch (_: Exception) {}
                }
            }
        )
    }

    // ── Share options dialog ───────────────────────────────────────────────────
    if (showShareOptions) {
        val shareText = buildString {
            if (post.description.isNotEmpty()) append(post.description).append("\n")
            append("Apskatiet Ryder lietotnē!")
        }
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            containerColor = CardBackground,
            title = { Text("Dalīties", color = Color.White) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showShareOptions = false
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    },
                                    "Dalīties"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Dalīties ārēji", color = Color.White, modifier = Modifier.fillMaxWidth()) }
                    HorizontalDivider(color = DividerColor)
                    TextButton(
                        onClick = { showShareOptions = false; showShareToUser = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sūtīt draugam", color = Color.White, modifier = Modifier.fillMaxWidth()) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareOptions = false }) { Text("Atcelt", color = Color.Gray) }
            }
        )
    }

    if (showShareToUser && currentUser != null) {
        ShareToUserDialog(
            post = post,
            currentUser = currentUser,
            onDismiss = { showShareToUser = false }
        )
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun PostActionButton(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CommentsSheet(
    post: Post,
    currentUser: User?,
    repository: PostRepository,
    onDismiss: () -> Unit
) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(post.id) {
        isLoading = true
        try { comments = repository.getComments(post.id) } catch (_: Exception) {}
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = "Komentāri",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when {
                isLoading -> CircularProgressIndicator(
                    color = RyderRed,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(24.dp)
                )
                comments.isEmpty() -> Text(
                    text = "Nav komentāru. Esi pirmais!",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(comments) { comment ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = comment.nickname,
                                color = RyderRed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(text = comment.text, color = Color.White, fontSize = 14.sp)
                            HorizontalDivider(
                                color = DividerColor,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (currentUser != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        shape = RoundedCornerShape(24.dp),
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Raksti komentāru...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RyderRed,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = RyderRed
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val text = commentText.trim()
                            if (text.isNotEmpty()) {
                                commentText = ""
                                scope.launch {
                                    try {
                                        val saved = repository.addComment(
                                            post.id,
                                            Comment(
                                                userId = currentUser.uid,
                                                nickname = currentUser.nickname,
                                                text = text
                                            )
                                        )
                                        comments = comments + saved
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Sūtīt", tint = RyderRed)
                    }
                }
            } else {
                Text(
                    text = "Piesakies, lai komentētu",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReportDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val reasons = listOf("Surogātpasts", "Nepiedienīgs saturs", "Uzmākšanās", "Viltus informācija", "Cits")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Ziņot par ziņu", color = Color.White) },
        text = {
            Column {
                reasons.forEach { reason ->
                    TextButton(
                        onClick = { onReport(reason) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = reason,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider(color = DividerColor)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Atcelt", color = Color.Gray)
            }
        }
    )
}

// ── Time formatter ────────────────────────────────────────────────────────────

object PostCardTimeFormatter {
    fun formatTimeAgo(timeMillis: Long): String {
        if (timeMillis == 0L) return ""
        val diff = System.currentTimeMillis() - timeMillis
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 1 -> "Tikko"
            minutes < 60 -> "Pirms $minutes min"
            hours < 24 -> "Pirms $hours st"
            else -> "Pirms $days d"
        }
    }
}
