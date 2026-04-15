@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import common.data.PostRepository
import common.model.Comment
import common.model.Post
import common.model.User
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit



private val HeartRed = Color(0xFFE53935)

@Composable
fun PostCard(post: Post, currentUser: User?, onDeleted: (() -> Unit)? = null) {
    val cardBg = AppColors.surface
    val divColor = AppColors.divider
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary

    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.likeCount) }
    var showComments by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showShareToUser by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(post.description) }

    val isOwner = currentUser?.uid == post.userId
    val repository = remember { PostRepository() }
    val scope = rememberCoroutineScope()

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
            .background(cardBg)
            .border(1.5.dp, RyderAccent, RoundedCornerShape(14.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
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
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = textSecondary)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (isOwner) {
                            DropdownMenuItem(
                                text = { Text("Rediģēt") },
                                onClick = { showMenu = false; editText = post.description; showEditDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Dzēst", color = Color(0xFFE53935)) },
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
            }

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
                                .background(AppColors.avatarPlaceholder),
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

            if (post.description.isNotEmpty()) {
                HashtagText(
                    text = post.description,
                    color = textPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            HorizontalDivider(color = divColor, modifier = Modifier.padding(horizontal = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PostActionButton(
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    tint = if (isLiked) HeartRed else textSecondary,
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

                PostActionButton(
                    icon = Icons.Default.ChatBubble,
                    tint = textSecondary,
                    label = if (post.commentCount > 0) post.commentCount.toString() else ""
                ) { showComments = true }

                PostActionButton(
                    icon = Icons.Default.Send,
                    tint = textSecondary,
                    label = ""
                ) {
                    if (currentUser != null) showShareToUser = true
                }
            }
        }
    }

    if (showComments) {
        CommentsSheet(
            post = post,
            currentUser = currentUser,
            repository = repository,
            onDismiss = { showComments = false }
        )
    }

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason ->
                showReportDialog = false
                val uid = currentUser?.uid ?: return@ReportDialog
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
            }
        )
    }

    if (showShareToUser && currentUser != null) {
        ShareToUserDialog(post = post, currentUser = currentUser, onDismiss = { showShareToUser = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = cardBg,
            title = { Text("Dzēst ierakstu?", color = textPrimary) },
            text = { Text("Šo darbību nevar atsaukt.", color = textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        try { repository.deletePost(post.id); onDeleted?.invoke() } catch (_: Exception) {}
                    }
                }) { Text("Dzēst", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Atcelt", color = textSecondary) }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = cardBg,
            title = { Text("Rediģēt ierakstu", color = textPrimary) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = RyderAccent,
                        unfocusedBorderColor = AppColors.inputBorder,
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
                TextButton(onClick = { showEditDialog = false }) { Text("Atcelt", color = textSecondary) }
            }
        )
    }
}

@Composable
private fun PostActionButton(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .indication(interactionSource, LocalIndication.current),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = AppColors.textSecondary, fontSize = 13.sp)
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
    val sheetBg = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val textHint = AppColors.textHint
    val divColor = AppColors.divider

    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var reportingComment by remember { mutableStateOf<Comment?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val adminRepo = remember { common.data.AdminRepository() }

    LaunchedEffect(post.id) {
        isLoading = true
        try { comments = repository.getComments(post.id) } catch (_: Exception) {}
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
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
                .imePadding()
        ) {
            Text(
                text = "Komentāri",
                color = textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when {
                isLoading -> CircularProgressIndicator(
                    color = RyderAccent,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp)
                )
                comments.isEmpty() -> Text(
                    text = "Nav komentāru. Esi pirmais!",
                    color = textSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(comments) { comment ->
                        val isOwnComment = currentUser?.uid == comment.userId
                        Row(verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                                Text(
                                    text = comment.nickname,
                                    color = RyderAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(text = comment.text, color = textPrimary, fontSize = 14.sp)
                            }
                            if (currentUser != null && !isOwnComment) {
                                Box {
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, null, tint = textSecondary, modifier = Modifier.size(16.dp))
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
                        HorizontalDivider(color = divColor)
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
                        placeholder = { Text("Raksti komentāru...", color = textHint) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = RyderAccent,
                            unfocusedBorderColor = AppColors.inputBorder,
                            cursorColor = RyderAccent
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrect = false),
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
                        Icon(Icons.Default.Send, contentDescription = "Sūtīt", tint = RyderAccent)
                    }
                }
            } else {
                Text(
                    text = "Piesakies, lai komentētu",
                    color = textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onReport: (String) -> Unit) {
    val reasons = listOf("Surogātpasts", "Nepiedienīgs saturs", "Uzmākšanās", "Viltus informācija", "Cits")
    val cardBg = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val divColor = AppColors.divider

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        title = { Text("Ziņot par ziņu", color = textPrimary) },
        text = {
            Column {
                reasons.forEach { reason ->
                    TextButton(onClick = { onReport(reason) }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = reason, color = textPrimary, modifier = Modifier.fillMaxWidth())
                    }
                    HorizontalDivider(color = divColor)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Atcelt", color = AppColors.textSecondary) }
        }
    )
}

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
