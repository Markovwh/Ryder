@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import common.data.AdminRepository
import common.data.NotificationRepository
import common.data.PostRepository
import common.model.AppNotification
import common.model.Comment
import common.model.Post
import common.model.User
import kotlinx.coroutines.launch

private val HeartRed = Color(0xFFE53935)

@Composable
fun PostDetailDialog(
    post: Post,
    currentUser: User?,
    onDismiss: () -> Unit,
    onDeleted: (() -> Unit)? = null
) {
    val repository = remember { PostRepository() }
    val adminRepo = remember { AdminRepository() }
    val notifRepo = remember { NotificationRepository() }
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.likeCount) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var isLoadingComments by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportingComment by remember { mutableStateOf<Comment?>(null) }
    var showShareToUser by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(post.description) }
    val isOwner = currentUser?.uid == post.userId

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val surface = AppColors.surface
    val divider = AppColors.divider
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val inputBorder = AppColors.inputBorder
    val textHint = AppColors.textHint

    LaunchedEffect(post.id) {
        val uid = currentUser?.uid
        if (uid != null && post.id.isNotEmpty()) {
            try { isLiked = repository.isLiked(post.id, uid) } catch (_: Exception) {}
        }
        try { comments = repository.getComments(post.id) } catch (_: Exception) {}
        isLoadingComments = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .background(surface)
                .border(1.5.dp, RyderAccent, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        profilePicture = post.user.profilePicture,
                        nickname = post.user.nickname,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.user.nickname,
                            color = textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        val time = PostCardTimeFormatter.formatTimeAgo(post.createdAt)
                        if (time.isNotEmpty()) {
                            Text(text = time, color = textSecondary, fontSize = 11.sp)
                        }
                    }
                    // Three-dot menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = textSecondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Rediģēt") },
                                    onClick = { showMenu = false; editText = post.description; showEditDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dzēst", color = HeartRed) },
                                    onClick = { showMenu = false; showDeleteConfirm = true }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Ziņot") },
                                    onClick = { showMenu = false; showReportDialog = true }
                                )
                            }
                        }
                    }
                    // Close button
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Aizvērt", tint = textPrimary)
                    }
                }

                HorizontalDivider(color = divider)

                // ── Scrollable content ────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    // Media
                    if (post.mediaUrls.isNotEmpty()) {
                        item {
                            val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })
                            Box {
                                HorizontalPager(state = pagerState) { page ->
                                    val url = post.mediaUrls[page]
                                    if (url.isVideoUrl()) {
                                        VideoPlayer(
                                            url = url,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                        )
                                    } else {
                                        Image(
                                            painter = rememberAsyncImagePainter(url),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                                .background(AppColors.avatarPlaceholder),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
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
                                                        if (pagerState.currentPage == index) textPrimary
                                                        else textPrimary.copy(alpha = 0.3f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Description
                    if (post.description.isNotEmpty()) {
                        item {
                            HashtagText(
                                text = post.description,
                                color = textPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Action row: like, comment count, share
                    item {
                        HorizontalDivider(color = divider, modifier = Modifier.padding(horizontal = 8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Like
                            DialogActionButton(
                                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                tint = if (isLiked) HeartRed else textSecondary,
                                label = if (likeCount > 0) likeCount.toString() else ""
                            ) {
                                val uid = currentUser?.uid ?: return@DialogActionButton
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
                            // Comment count (display only — input is below)
                            DialogActionButton(
                                icon = Icons.Default.ChatBubble,
                                tint = textSecondary,
                                label = if (comments.isNotEmpty()) comments.size.toString() else ""
                            ) {
                                scope.launch { listState.animateScrollToItem(Int.MAX_VALUE) }
                            }
                            // Share
                            DialogActionButton(
                                icon = Icons.Default.Share,
                                tint = textSecondary,
                                label = ""
                            ) {
                                if (currentUser != null) showShareToUser = true
                            }
                        }
                        HorizontalDivider(color = divider, modifier = Modifier.padding(horizontal = 8.dp))
                    }

                    // Comments header
                    item {
                        val label = when {
                            isLoadingComments -> "Komentāri"
                            comments.isEmpty() -> "Komentāri"
                            else -> "Komentāri (${comments.size})"
                        }
                        Text(
                            text = label,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }

                    // Comments list
                    if (isLoadingComments) {
                        item {
                            CircularProgressIndicator(
                                color = RyderAccent,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(24.dp)
                            )
                        }
                    } else if (comments.isEmpty()) {
                        item {
                            Text(
                                text = "Nav komentāru. Esi pirmais!",
                                color = textSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(comments) { comment ->
                            val isOwnComment = currentUser?.uid == comment.userId
                            Row(
                                modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = comment.nickname,
                                            color = RyderAccent,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = PostCardTimeFormatter.formatTimeAgo(comment.createdAt),
                                            color = textSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(text = comment.text, color = textPrimary, fontSize = 14.sp)
                                }
                                if (currentUser != null && !isOwnComment) {
                                    Box {
                                        var menuExpanded by remember { mutableStateOf(false) }
                                        IconButton(
                                            onClick = { menuExpanded = true },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.MoreVert, null, tint = textSecondary, modifier = Modifier.size(15.dp))
                                        }
                                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false },
                                            containerColor = AppColors.surface) {
                                            DropdownMenuItem(
                                                text = { Text("Ziņot", color = textPrimary) },
                                                onClick = { menuExpanded = false; reportingComment = comment }
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = divider,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                    }

                    // Bottom padding so last comment isn't hidden behind input
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // ── Comment input (pinned at bottom) ──────────────────────────
                HorizontalDivider(color = divider)
                if (currentUser != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            shape = RoundedCornerShape(24.dp),
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Raksti komentāru...", color = textHint) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = RyderAccent,
                                unfocusedBorderColor = inputBorder,
                                cursorColor = RyderAccent
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val text = commentText.trim()
                                if (text.isEmpty()) return@IconButton
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
                                        listState.animateScrollToItem(Int.MAX_VALUE)
                                    } catch (_: Exception) {
                                        return@launch
                                    }
                                    if (post.userId != currentUser.uid) {
                                        try {
                                            notifRepo.send(AppNotification(
                                                recipientId = post.userId,
                                                senderId = currentUser.uid,
                                                senderNickname = currentUser.nickname,
                                                senderPicture = currentUser.profilePicture ?: "",
                                                type = "comment",
                                                postId = post.id,
                                                commentPreview = text.take(80)
                                            ))
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Sūtīt", tint = RyderAccent)
                        }
                    }
                } else {
                    Text(
                        text = "Piesakies, lai komentētu",
                        color = textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        val reasons = listOf("Surogātpasts", "Nepiedienīgs saturs", "Uzmākšanās", "Viltus informācija", "Cits")
        val dialogSurface = AppColors.surface
        val dialogTextPrimary = AppColors.textPrimary
        val dialogDivider = AppColors.divider
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = dialogSurface,
            title = { Text("Ziņot par ziņu", color = dialogTextPrimary) },
            text = {
                Column {
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                showReportDialog = false
                                val uid = currentUser?.uid ?: return@TextButton
                                scope.launch {
                                    try {
                                        repository.reportPost(
                                            postId = post.id,
                                            reporterId = uid,
                                            reporterNickname = currentUser?.nickname ?: "",
                                            targetOwnerNickname = post.user.nickname,
                                            reason = reason
                                        )
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reason, color = dialogTextPrimary, modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider(color = dialogDivider)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Atcelt", color = AppColors.textSecondary)
                }
            }
        )
    }

    reportingComment?.let { comment ->
        val reasons = listOf("Surogātpasts", "Nepiedienīgs saturs", "Uzmākšanās", "Viltus informācija", "Cits")
        AlertDialog(
            onDismissRequest = { reportingComment = null },
            containerColor = AppColors.surface,
            title = { Text("Ziņot par komentāru", color = AppColors.textPrimary) },
            text = {
                Column {
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                reportingComment = null
                                val reporter = currentUser ?: return@TextButton
                                scope.launch {
                                    try {
                                        adminRepo.submitReport(
                                            targetId = comment.id,
                                            targetType = "comment",
                                            targetOwnerNickname = comment.nickname,
                                            reporterId = reporter.uid,
                                            reporterNickname = reporter.nickname,
                                            reason = reason,
                                            description = comment.text
                                        )
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reason, color = AppColors.textPrimary, modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider(color = AppColors.divider)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { reportingComment = null }) {
                    Text("Atcelt", color = AppColors.textSecondary)
                }
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

    if (showDeleteConfirm) {
        val dialogSurface = AppColors.surface
        val dialogTextPrimary = AppColors.textPrimary
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = dialogSurface,
            title = { Text("Dzēst ierakstu?", color = dialogTextPrimary) },
            text = { Text("Šo darbību nevar atsaukt.", color = AppColors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        try {
                            repository.deletePost(post.id)
                            onDeleted?.invoke()
                            onDismiss()
                        } catch (_: Exception) {}
                    }
                }) { Text("Dzēst", color = HeartRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Atcelt", color = AppColors.textSecondary) }
            }
        )
    }

    if (showEditDialog) {
        val dialogSurface = AppColors.surface
        val dialogTextPrimary = AppColors.textPrimary
        val dialogInputBorder = AppColors.inputBorder
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = dialogSurface,
            title = { Text("Rediģēt ierakstu", color = dialogTextPrimary) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = dialogTextPrimary,
                        unfocusedTextColor = dialogTextPrimary,
                        focusedBorderColor = RyderAccent,
                        unfocusedBorderColor = dialogInputBorder,
                        cursorColor = RyderAccent
                    ),
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newText = editText
                    showEditDialog = false
                    scope.launch {
                        try { repository.updatePost(post.id, newText) } catch (_: Exception) {}
                    }
                }) { Text("Saglabāt", color = RyderAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Atcelt", color = AppColors.textSecondary) }
            }
        )
    }
}

@Composable
private fun DialogActionButton(
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
            Text(text = label, color = AppColors.textSecondary, fontSize = 13.sp)
        }
    }
}
