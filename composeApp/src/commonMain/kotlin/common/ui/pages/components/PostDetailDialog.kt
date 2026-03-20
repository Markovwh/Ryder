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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import common.data.PostRepository
import common.model.Comment
import common.model.Post
import common.model.User
import kotlinx.coroutines.launch

private val DialogBackground = Color(0xFF1A1A1A)
private val DialogDivider = Color(0xFF2D2D2D)

@Composable
fun PostDetailDialog(
    post: Post,
    currentUser: User?,
    onDismiss: () -> Unit,
    onDeleted: (() -> Unit)? = null
) {
    val repository = remember { PostRepository() }
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.likeCount) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var isLoadingComments by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showShareOptions by remember { mutableStateOf(false) }
    var showShareToUser by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isOwner = currentUser?.uid == post.userId

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()

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
                .background(DialogBackground)
                .border(1.5.dp, RyderRed, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
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
                            Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = Color.Gray)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Dzēst", color = Color(0xFFFF4444)) },
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
                        Icon(Icons.Default.Close, contentDescription = "Aizvērt", tint = Color.White)
                    }
                }

                HorizontalDivider(color = DialogDivider)

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
                                    Image(
                                        painter = rememberAsyncImagePainter(post.mediaUrls[page]),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(260.dp)
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
                    }

                    // Description
                    if (post.description.isNotEmpty()) {
                        item {
                            Text(
                                text = post.description,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Action row: like, comment count, share
                    item {
                        HorizontalDivider(color = DialogDivider, modifier = Modifier.padding(horizontal = 8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Like
                            DialogActionButton(
                                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                tint = if (isLiked) RyderRed else Color.Gray,
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
                                tint = Color.Gray,
                                label = if (comments.isNotEmpty()) comments.size.toString() else ""
                            ) {
                                scope.launch { listState.animateScrollToItem(Int.MAX_VALUE) }
                            }
                            // Share
                            DialogActionButton(
                                icon = Icons.Default.Share,
                                tint = Color.Gray,
                                label = ""
                            ) {
                                if (currentUser != null) {
                                    showShareOptions = true
                                } else {
                                    val text = buildString {
                                        if (post.description.isNotEmpty()) append(post.description).append("\n")
                                        append("Apskatiet Ryder lietotnē!")
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, text)
                                            },
                                            "Dalīties"
                                        )
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = DialogDivider, modifier = Modifier.padding(horizontal = 8.dp))
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
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }

                    // Comments list
                    if (isLoadingComments) {
                        item {
                            CircularProgressIndicator(
                                color = RyderRed,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(24.dp)
                            )
                        }
                    } else if (comments.isEmpty()) {
                        item {
                            Text(
                                text = "Nav komentāru. Esi pirmais!",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(comments) { comment ->
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.nickname,
                                        color = RyderRed,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = PostCardTimeFormatter.formatTimeAgo(comment.createdAt),
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            HorizontalDivider(
                                color = DialogDivider,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                    }

                    // Bottom padding so last comment isn't hidden behind input
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // ── Comment input (pinned at bottom) ──────────────────────────
                HorizontalDivider(color = DialogDivider)
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
                                    } catch (_: Exception) {}
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
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = DialogBackground,
            title = { Text("Ziņot par ziņu", color = Color.White) },
            text = {
                Column {
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                showReportDialog = false
                                val uid = currentUser?.uid ?: return@TextButton
                                scope.launch {
                                    try { repository.reportPost(post.id, uid, reason) } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reason, color = Color.White, modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider(color = DialogDivider)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Atcelt", color = Color.Gray)
                }
            }
        )
    }

    if (showShareOptions) {
        val shareText = buildString {
            if (post.description.isNotEmpty()) append(post.description).append("\n")
            append("Apskatiet Ryder lietotnē!")
        }
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            containerColor = DialogBackground,
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
                    HorizontalDivider(color = DialogDivider)
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DialogBackground,
            title = { Text("Dzēst ierakstu?", color = Color.White) },
            text = { Text("Šo darbību nevar atsaukt.", color = Color.Gray) },
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
                }) { Text("Dzēst", color = Color(0xFFFF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Atcelt", color = Color.Gray) }
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
            Text(text = label, color = Color.Gray, fontSize = 13.sp)
        }
    }
}
