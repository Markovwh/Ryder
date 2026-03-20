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
import common.data.MessageRepository
import common.data.PostRepository
import common.data.UserRepository
import common.model.Post
import common.model.User
import common.ui.pages.components.PostDetailDialog
import common.ui.pages.components.RyderRed
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
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<User?>(null) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    var followerCount by remember { mutableStateOf(0) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showFollowList by remember { mutableStateOf<FollowListType?>(null) }

    LaunchedEffect(userId) {
        isLoading = true
        try {
            val loaded = postRepo.getUserById(userId)
            user = loaded
            followerCount = loaded?.followerCount ?: 0
            posts = postRepo.getPostsByUser(userId)
            val cu = currentUser?.uid
            if (cu != null) {
                isFollowing = userRepo.isFollowing(cu, userId)
                isBlocked = userRepo.isBlocked(cu, userId)
            }
        } catch (_: Exception) {}
        isLoading = false
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
                    Text(
                        text = user?.nickname ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentUser != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = Color.Gray)
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
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RyderRed)
            }
            return@Scaffold
        }

        val u = user ?: run {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Lietotājs nav atrasts", color = Color.Gray)
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
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    val pic = u.profilePicture
                    if (pic != null) {
                        Image(
                            painter = rememberAsyncImagePainter(pic),
                            contentDescription = null,
                            modifier = Modifier.size(90.dp).clip(CircleShape).background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(90.dp), shape = CircleShape, color = RyderRed) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = u.nickname.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(u.nickname, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    if (u.firstName.isNotEmpty() || u.lastName.isNotEmpty()) {
                        Text("${u.firstName} ${u.lastName}".trim(), color = Color.Gray, fontSize = 14.sp)
                    }
                    if (u.bio.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(u.bio, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                    if (u.bike.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🏍 ${u.bike}", color = Color.Gray, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats row — followers/following clickable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ClickableStat(
                            label = "Ieraksti",
                            value = posts.size.toString(),
                            onClick = null
                        )
                        ClickableStat(
                            label = "Sekotāji",
                            value = followerCount.toString(),
                            onClick = { showFollowList = FollowListType.FOLLOWERS }
                        )
                        ClickableStat(
                            label = "Seko",
                            value = u.followingCount.toString(),
                            onClick = { showFollowList = FollowListType.FOLLOWING }
                        )
                    }

                    // Action buttons (only shown when viewing someone else's profile)
                    if (currentUser != null && currentUser.uid != userId) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Follow / Unfollow
                            Button(
                                onClick = {
                                    val cu = currentUser.uid
                                    scope.launch {
                                        try {
                                            if (isFollowing) {
                                                userRepo.unfollow(cu, userId)
                                                isFollowing = false
                                                followerCount -= 1
                                            } else {
                                                userRepo.follow(cu, userId)
                                                isFollowing = true
                                                followerCount += 1
                                            }
                                        } catch (_: Exception) {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color.Transparent else RyderRed,
                                    contentColor = if (isFollowing) RyderRed else Color.White
                                ),
                                border = if (isFollowing) androidx.compose.foundation.BorderStroke(1.dp, RyderRed) else null,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isFollowing) "Nesekot" else "Sekot")
                            }

                            // Message
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val convId = msgRepo.getOrCreateConversation(currentUser, u)
                                            onOpenChat(
                                                Screen.Chat(
                                                    conversationId = convId,
                                                    otherUserId = u.uid,
                                                    otherUserNickname = u.nickname,
                                                    otherUserPicture = u.profilePicture
                                                )
                                            )
                                        } catch (_: Exception) {}
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ChatBubble,
                                    contentDescription = "Ziņa",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ziņa")
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF2D2D2D))
            }

            // ── Blocked state ────────────────────────────────────────────────
            if (isBlocked) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Šis lietotājs ir bloķēts.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
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
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Privāts profils", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Seko šim lietotājam, lai redzētu viņa ierakstus.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
                return@LazyColumn
            }

            // ── Post grid ────────────────────────────────────────────────────
            if (posts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Nav ierakstu", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                val rows = posts.chunked(3)
                itemsIndexed(rows) { _, row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { post ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .background(Color(0xFF1A1A1A))
                                    .clickable { selectedPost = post }
                            ) {
                                if (post.mediaUrls.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(post.mediaUrls.first()),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (post.description.isNotEmpty()) {
                                    Text(
                                        text = post.description,
                                        color = Color.White,
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
        PostDetailDialog(post = post, currentUser = currentUser, onDismiss = { selectedPost = null })
    }

    // ── Block confirm dialog ──────────────────────────────────────────────────
    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Bloķēt lietotāju?", color = Color.White) },
            text = { Text("Šis lietotājs vairs nevarēs redzēt tavu profilu un jūs abi pārstāsiet sekot viens otram.", color = Color.Gray) },
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
                }) { Text("Bloķēt", color = RyderRed) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) { Text("Atcelt", color = Color.Gray) }
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
private fun FollowListDialog(
    userId: String,
    type: FollowListType,
    userRepo: UserRepository,
    onDismiss: () -> Unit,
    onOpenUser: (String, String) -> Unit
) {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

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
                .background(Color(0xFF1A1A1A))
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("Aizvērt", color = Color.Gray) }
            }
            HorizontalDivider(color = Color(0xFF2D2D2D))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RyderRed, modifier = Modifier.size(28.dp))
                    }
                }
                users.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (type == FollowListType.FOLLOWERS) "Nav sekotāju" else "Neseko nevienam",
                            color = Color.Gray,
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
                                val pic = u.profilePicture
                                if (pic != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(pic),
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.Gray),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = RyderRed) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = u.nickname.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(u.nickname, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    if (u.firstName.isNotEmpty() || u.lastName.isNotEmpty()) {
                                        Text("${u.firstName} ${u.lastName}".trim(), color = Color.Gray, fontSize = 13.sp)
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2D2D2D))
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
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, color = if (onClick != null) Color.LightGray else Color.Gray, fontSize = 12.sp)
    }
}
