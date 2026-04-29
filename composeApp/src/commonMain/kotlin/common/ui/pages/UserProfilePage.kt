@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import common.ui.pages.components.isVideoUrl
import common.ui.pages.components.rememberVideoThumbnailLoader
import common.data.MessageRepository
import common.data.AdminRepository
import common.data.NotificationRepository
import common.data.PostRepository
import common.data.UserRepository
import common.model.AppNotification
import common.model.Post
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.PostDetailDialog
import common.ui.pages.components.RyderAccent
import common.ui.pages.components.UserAvatar
import kotlinx.coroutines.launch

@Composable
fun UserProfilePage(
    userId: String,
    currentUser: User?,
    onBack: () -> Unit,
    onOpenChat: (Screen.Chat) -> Unit,
    onOpenUser: (String, String) -> Unit
) {
    val postRepo = remember { PostRepository() }
    val userRepo = remember { UserRepository() }
    val msgRepo = remember { MessageRepository() }
    val adminRepo = remember { AdminRepository() }
    val notifRepo = remember { NotificationRepository() }
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<User?>(null) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var isRequested by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    var followerCount by remember { mutableStateOf(0) }
    var isFollowLoading by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showFollowList by remember { mutableStateOf<FollowListType?>(null) }

    val bg = AppColors.background
    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val divider = AppColors.divider

    LaunchedEffect(userId) {
        isLoading = true

        user = try { postRepo.getUserById(userId) } catch (_: Exception) { null }
        followerCount = try { userRepo.getFollowerCount(userId) } catch (_: Exception) { 0 }

        val cu = currentUser?.uid
        // Use local vars so a single failure doesn't block the rest of the load.
        val following = if (cu != null) {
            try { userRepo.isFollowing(cu, userId) } catch (_: Exception) { false }
        } else false
        val blocked = if (cu != null) {
            try { userRepo.isBlocked(cu, userId) } catch (_: Exception) { false }
        } else false
        val requested = if (cu != null && !following) {
            try { userRepo.hasSentFollowRequest(cu, userId) } catch (_: Exception) { false }
        } else false
        // Mutual-follow check for "Draugi" posts: does the owner also follow the viewer?
        val ownerFollowsViewer = if (cu != null && following) {
            try { userRepo.isFollowing(userId, cu) } catch (_: Exception) { false }
        } else false

        isFollowing = following
        isBlocked = blocked
        isRequested = requested

        posts = try {
            postRepo.getPostsByUser(
                userId = userId,
                viewerId = cu,
                mutualFollow = following && ownerFollowsViewer
            )
        } catch (_: Exception) { emptyList() }

        isLoading = false
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
                    Text(
                        text = user?.nickname ?: "",
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentUser != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = textSecondary)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isBlocked) "Atbloķēt" else "Bloķēt") },
                                    onClick = {
                                        showMenu = false
                                        if (isBlocked) {
                                            scope.launch {
                                                try {
                                                    userRepo.unblock(currentUser.uid, userId)
                                                    isBlocked = false
                                                } catch (_: Exception) {}
                                            }
                                        } else {
                                            showBlockConfirm = true
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ziņot") },
                                    onClick = { showMenu = false; showReportDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RyderAccent)
            }
            return@Scaffold
        }

        val u = user ?: run {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Lietotājs nav atrasts", color = textSecondary)
            }
            return@Scaffold
        }

        val isPrivate = u.profilePrivacy == "Privāts" && !isFollowing && currentUser?.uid != userId

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── Profile header ───────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(
                        profilePicture = u.profilePicture,
                        nickname = u.nickname,
                        size = 90.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(u.nickname, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    if (u.firstName.isNotEmpty() || u.lastName.isNotEmpty()) {
                        Text("${u.firstName} ${u.lastName}".trim(), color = textSecondary, fontSize = 14.sp)
                    }
                    if (u.bio.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(u.bio, color = textPrimary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                    if (u.bike.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🏍 ${u.bike}", color = textSecondary, fontSize = 13.sp)
                    }
                    if (u.experienceYears > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "⏱ ${u.experienceYears} ${if (u.experienceYears == 1) "gads" else "gadi"} pieredze",
                            color = textSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats row — followers/following clickable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ClickableStat(label = "Ieraksti", value = posts.size.toString(), onClick = null)
                        ClickableStat(
                            label = "Sekotāji",
                            value = followerCount.toString(),
                            onClick = { showFollowList = FollowListType.FOLLOWERS }
                        )
                        ClickableStat(
                            label = "Seko",
                            value = u.following.size.toString(),
                            onClick = { showFollowList = FollowListType.FOLLOWING }
                        )
                    }

                    // Action buttons (only shown when viewing someone else's profile)
                    if (currentUser != null && currentUser.uid != userId) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Follow / Unfollow / Request
                            val targetIsPrivate = u.profilePrivacy == "Privāts"
                            val buttonLabel = when {
                                isFollowing -> "Nesekot"
                                isRequested -> "Pieprasīts"
                                else -> "Sekot"
                            }
                            Button(
                                onClick = {
                                    if (isFollowLoading) return@Button
                                    val cu = currentUser.uid
                                    scope.launch {
                                        isFollowLoading = true
                                        try {
                                            when {
                                                isFollowing -> {
                                                    userRepo.unfollow(cu, userId)
                                                    isFollowing = false
                                                    followerCount = maxOf(0, followerCount - 1)
                                                }
                                                isRequested -> {
                                                    userRepo.cancelFollowRequest(cu, userId)
                                                    isRequested = false
                                                }
                                                targetIsPrivate -> {
                                                    userRepo.sendFollowRequest(cu, userId)
                                                    isRequested = true
                                                    notifRepo.send(AppNotification(
                                                        recipientId = userId,
                                                        senderId = currentUser.uid,
                                                        senderNickname = currentUser.nickname,
                                                        senderPicture = currentUser.profilePicture ?: "",
                                                        type = "follow_request"
                                                    ))
                                                }
                                                else -> {
                                                    userRepo.follow(cu, userId)
                                                    isFollowing = true
                                                    followerCount += 1
                                                    notifRepo.send(AppNotification(
                                                        recipientId = userId,
                                                        senderId = currentUser.uid,
                                                        senderNickname = currentUser.nickname,
                                                        senderPicture = currentUser.profilePicture ?: "",
                                                        type = "follow"
                                                    ))
                                                }
                                            }
                                        } catch (_: Exception) {
                                        } finally {
                                            isFollowLoading = false
                                        }
                                    }
                                },
                                enabled = !isFollowLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing || isRequested) Color.Transparent else RyderAccent,
                                    contentColor = if (isFollowing || isRequested) RyderAccent else Color.White
                                ),
                                border = if (isFollowing || isRequested) androidx.compose.foundation.BorderStroke(1.dp, RyderAccent) else null,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isFollowLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isFollowing || isRequested) RyderAccent else Color.White
                                    )
                                } else {
                                    Text(buttonLabel)
                                }
                            }

                            // Message
                            OutlinedButton(
                                onClick = {
                                    val convId = msgRepo.conversationId(currentUser.uid, u.uid)
                                    scope.launch {
                                        try { msgRepo.getOrCreateConversation(currentUser, u) } catch (_: Exception) {}
                                    }
                                    onOpenChat(
                                        Screen.Chat(
                                            conversationId = convId,
                                            otherUserId = u.uid,
                                            otherUserNickname = u.nickname,
                                            otherUserPicture = u.profilePicture
                                        )
                                    )
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.inputBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ChatBubble, contentDescription = "Ziņa", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ziņa")
                            }
                        }
                    }
                }
                HorizontalDivider(color = divider)
            }

            // ── Blocked state ────────────────────────────────────────────────
            if (isBlocked) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Šis lietotājs ir bloķēts.", color = textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
                return@LazyColumn
            }

            // ── Private profile gate ─────────────────────────────────────────
            if (isPrivate) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = textSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Privāts profils", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Seko šim lietotājam, lai redzētu viņa ierakstus.", color = textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
                return@LazyColumn
            }

            // ── Post grid ────────────────────────────────────────────────────
            if (posts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Nav ierakstu", color = textSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                val rows = posts.chunked(3)
                itemsIndexed(rows) { _, row ->
                    Row(modifier = Modifier.fillMaxWidth().background(AppColors.background)) {
                        row.forEach { post ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .background(AppColors.tileBackground)
                                    .clickable { selectedPost = post }
                            ) {
                                if (post.mediaUrls.isNotEmpty()) {
                                    val url = post.mediaUrls.first()
                                    val isVideo = url.isVideoUrl()
                                    val loader = if (isVideo) rememberVideoThumbnailLoader() else null
                                    Image(
                                        painter = if (loader != null)
                                            rememberAsyncImagePainter(model = url, imageLoader = loader)
                                        else
                                            rememberAsyncImagePainter(url),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (isVideo) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(28.dp).align(Alignment.Center)
                                        )
                                    }
                                } else if (post.description.isNotEmpty()) {
                                    Text(
                                        text = post.description,
                                        color = textPrimary,
                                        fontSize = 11.sp,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }
                        repeat(3 - row.size) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(1.dp))
                        }
                    }
                }
            }
        }
    }

    // ── Post detail ───────────────────────────────────────────────────────────
    selectedPost?.let { post ->
        PostDetailDialog(
            post = post,
            currentUser = currentUser,
            onDismiss = { selectedPost = null },
            onDeleted = {
                posts = posts.filter { it.id != post.id }
                selectedPost = null
            }
        )
    }

    // ── Block confirm dialog ──────────────────────────────────────────────────
    if (showBlockConfirm) {
        val dialogSurface = AppColors.surface
        val dialogTextPrimary = AppColors.textPrimary
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            containerColor = dialogSurface,
            title = { Text("Bloķēt lietotāju?", color = dialogTextPrimary) },
            text = { Text("Šis lietotājs vairs nevarēs redzēt tavu profilu un jūs abi pārstāsiet sekot viens otram.", color = AppColors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showBlockConfirm = false
                    val cu = currentUser?.uid ?: return@TextButton
                    scope.launch {
                        try {
                            userRepo.block(cu, userId)
                            isBlocked = true
                            isFollowing = false
                        } catch (_: Exception) {}
                    }
                }) { Text("Bloķēt", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) { Text("Atcelt", color = AppColors.textSecondary) }
            }
        )
    }

    // ── Report user dialog ────────────────────────────────────────────────────
    if (showReportDialog && currentUser != null) {
        val reasons = listOf("Surogātpasts", "Nepiedienīgs saturs", "Uzmākšanās", "Viltus informācija", "Naida runa", "Cits")
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = AppColors.surface,
            title = { Text("Ziņot par lietotāju", color = AppColors.textPrimary) },
            text = {
                Column {
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                showReportDialog = false
                                scope.launch {
                                    try {
                                        adminRepo.submitReport(
                                            targetId = userId,
                                            targetType = "user",
                                            targetOwnerNickname = user?.nickname ?: "",
                                            reporterId = currentUser.uid,
                                            reporterNickname = currentUser.nickname,
                                            reason = reason
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
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Atcelt", color = AppColors.textSecondary)
                }
            }
        )
    }

    // ── Followers / Following dialog ──────────────────────────────────────────
    showFollowList?.let { type ->
        FollowListDialog(
            userId = userId,
            type = type,
            userRepo = userRepo,
            onDismiss = { showFollowList = null },
            onOpenUser = { uid, nickname ->
                showFollowList = null
                onOpenUser(uid, nickname)
            }
        )
    }
}

// ── Follow list type ──────────────────────────────────────────────────────────

enum class FollowListType { FOLLOWERS, FOLLOWING }

// ── Follow list dialog ────────────────────────────────────────────────────────

@Composable
fun FollowListDialog(
    userId: String,
    type: FollowListType,
    userRepo: UserRepository,
    onDismiss: () -> Unit,
    onOpenUser: (String, String) -> Unit
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val divider = AppColors.divider

    LaunchedEffect(userId, type) {
        isLoading = true
        try {
            users = if (type == FollowListType.FOLLOWERS) userRepo.getFollowers(userId)
                    else userRepo.getFollowing(userId)
        } catch (_: Exception) {}
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(surface)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (type == FollowListType.FOLLOWERS) "Sekotāji" else "Seko",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("Aizvērt", color = textSecondary) }
            }
            HorizontalDivider(color = divider)

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RyderAccent, modifier = Modifier.size(28.dp))
                    }
                }
                users.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (type == FollowListType.FOLLOWERS) "Nav sekotāju" else "Neseko nevienam",
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(users, key = { it.uid }) { u ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenUser(u.uid, u.nickname) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    profilePicture = u.profilePicture,
                                    nickname = u.nickname,
                                    size = 44.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(u.nickname, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    if (u.firstName.isNotEmpty() || u.lastName.isNotEmpty()) {
                                        Text("${u.firstName} ${u.lastName}".trim(), color = textSecondary, fontSize = 13.sp)
                                    }
                                }
                            }
                            HorizontalDivider(color = divider)
                        }
                    }
                }
            }
        }
    }
}

// ── Stat composable ───────────────────────────────────────────────────────────

@Composable
private fun ClickableStat(label: String, value: String, onClick: (() -> Unit)?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Text(text = value, color = AppColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, color = AppColors.textSecondary, fontSize = 12.sp)
    }
}
