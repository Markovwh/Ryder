@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import common.data.AdminRepository
import common.data.GroupRepository
import common.model.Group
import common.model.Post
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.RyderAccent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupDetailPage(
    groupId: String,
    currentUser: User?,
    onBack: () -> Unit
) {
    val repo = remember { GroupRepository() }
    val adminRepo = remember { AdminRepository() }
    val scope = rememberCoroutineScope()
    var group by remember { mutableStateOf<Group?>(null) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showViewMembersDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPostDialog by remember { mutableStateOf(false) }
    var showDeleteGroupConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        isLoading = true
        try {
            group = repo.getGroup(groupId)
            posts = repo.getGroupPosts(groupId)
        } catch (_: Exception) {}
        isLoading = false
    }

    val g = group
    val isMember = g != null && currentUser != null && currentUser.uid in g.memberIds
    val isAdmin = g != null && currentUser != null && currentUser.uid in g.adminIds
    val isOwner = g != null && currentUser != null && g.ownerId == currentUser.uid
    val pinnedIds = g?.pinnedPostIds ?: emptyList()
    val pinnedPosts = posts.filter { it.id in pinnedIds }
    val regularPosts = posts.filter { it.id !in pinnedIds }

    Scaffold(
        containerColor = AppColors.background,
        topBar = {
            Surface(color = AppColors.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = AppColors.textPrimary)
                    }
                    Text(
                        text = g?.name ?: "Grupa",
                        color = AppColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentUser != null && g != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = AppColors.textSecondary)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (isAdmin) {
                                    DropdownMenuItem(
                                        text = { Text("Uzaicināt lietotāju") },
                                        onClick = { showMenu = false; showInviteDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pārvaldīt biedrus") },
                                        onClick = { showMenu = false; showMembersDialog = true }
                                    )
                                }
                                if (isOwner) {
                                    DropdownMenuItem(
                                        text = { Text("Rediģēt grupu") },
                                        onClick = { showMenu = false; showEditDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Dzēst grupu", color = Color(0xFFE53935)) },
                                        onClick = { showMenu = false; showDeleteGroupConfirm = true }
                                    )
                                } else if (isMember) {
                                    DropdownMenuItem(
                                        text = { Text("Atstāt grupu", color = Color(0xFFE53935)) },
                                        onClick = {
                                            showMenu = false
                                            scope.launch {
                                                try {
                                                    repo.removeMember(groupId, currentUser.uid)
                                                    onBack()
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                }
                                if (!isOwner) {
                                    DropdownMenuItem(
                                        text = { Text("Ziņot grupu") },
                                        leadingIcon = { Icon(Icons.Default.Flag, null, tint = AppColors.textSecondary) },
                                        onClick = { showMenu = false; showReportDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (isMember) {
                FloatingActionButton(
                    onClick = { showPostDialog = true },
                    containerColor = RyderAccent,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Jauns ieraksts", tint = Color.White)
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
        if (g == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Grupa nav atrasta", color = AppColors.textSecondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                GroupHeader(
                    group = g,
                    isMember = isMember,
                    currentUser = currentUser,
                    onViewMembers = { showViewMembersDialog = true },
                    onJoin = {
                        scope.launch {
                            try {
                                repo.addMember(groupId, currentUser!!.uid)
                                group = repo.getGroup(groupId)
                            } catch (_: Exception) {}
                        }
                    }
                )
                HorizontalDivider(color = AppColors.divider)
            }

            if (pinnedPosts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            tint = RyderAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Piespraustas ziņas",
                            color = AppColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                items(pinnedPosts, key = { "pinned_${it.id}" }) { post ->
                    GroupPostCard(
                        post = post,
                        isPinned = true,
                        isAdmin = isAdmin,
                        onPin = {},
                        onUnpin = {
                            scope.launch {
                                try {
                                    repo.unpinGroupPost(groupId, post.id)
                                    group = repo.getGroup(groupId)
                                } catch (_: Exception) {}
                            }
                        },
                        onRemove = {
                            scope.launch {
                                try {
                                    repo.deleteGroupPost(groupId, post.id)
                                    posts = posts.filter { it.id != post.id }
                                    group = repo.getGroup(groupId)
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
                item { HorizontalDivider(color = AppColors.divider) }
            }

            if (regularPosts.isEmpty() && pinnedPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isMember) "Nav ierakstu. Esi pirmais, kas publicē!"
                            else "Pievienojies grupai, lai redzētu ierakstus.",
                            color = AppColors.textSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(regularPosts, key = { it.id }) { post ->
                    GroupPostCard(
                        post = post,
                        isPinned = false,
                        isAdmin = isAdmin,
                        onPin = {
                            scope.launch {
                                try {
                                    repo.pinGroupPost(groupId, post.id)
                                    group = repo.getGroup(groupId)
                                } catch (_: Exception) {}
                            }
                        },
                        onUnpin = {},
                        onRemove = {
                            scope.launch {
                                try {
                                    repo.deleteGroupPost(groupId, post.id)
                                    posts = posts.filter { it.id != post.id }
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }

    if (showPostDialog && currentUser != null) {
        CreateGroupPostDialog(
            currentUser = currentUser,
            repo = repo,
            groupId = groupId,
            onDismiss = { showPostDialog = false },
            onPosted = { newPost ->
                posts = listOf(newPost) + posts
                showPostDialog = false
            }
        )
    }

    if (showInviteDialog && g != null) {
        InviteUserDialog(
            repo = repo,
            group = g,
            onDismiss = { showInviteDialog = false },
            onInvited = { scope.launch { group = repo.getGroup(groupId) } }
        )
    }

    if (showMembersDialog && g != null) {
        ManageMembersDialog(
            group = g,
            repo = repo,
            currentUserId = currentUser?.uid ?: "",
            isOwner = isOwner,
            isAdmin = isAdmin,
            onDismiss = { showMembersDialog = false },
            onChanged = { scope.launch { group = repo.getGroup(groupId) } }
        )
    }

    if (showViewMembersDialog && g != null) {
        ViewMembersDialog(
            group = g,
            repo = repo,
            onDismiss = { showViewMembersDialog = false }
        )
    }

    if (showEditDialog && g != null) {
        EditGroupDialog(
            group = g,
            repo = repo,
            onDismiss = { showEditDialog = false },
            onSaved = { updated -> group = updated; showEditDialog = false }
        )
    }

    if (showDeleteGroupConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirm = false },
            containerColor = AppColors.surface,
            title = { Text("Dzēst grupu?", color = AppColors.textPrimary) },
            text = { Text("Šo darbību nevar atsaukt. Visi ieraksti tiks dzēsti.", color = AppColors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteGroupConfirm = false
                    scope.launch {
                        try { repo.deleteGroup(groupId) } catch (_: Exception) {}
                        onBack()
                    }
                }) { Text("Dzēst", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupConfirm = false }) {
                    Text("Atcelt", color = AppColors.textSecondary)
                }
            }
        )
    }

    if (showReportDialog && currentUser != null && g != null) {
        val reasons = listOf("Surogātpasts", "Nepiedienīgs saturs", "Naida runa", "Viltus informācija", "Cits")
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = AppColors.surface,
            title = { Text("Ziņot par grupu", color = AppColors.textPrimary) },
            text = {
                Column {
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                showReportDialog = false
                                scope.launch {
                                    try {
                                        adminRepo.submitReport(
                                            targetId = groupId,
                                            targetType = "group",
                                            targetOwnerNickname = g.name,
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
}

// ── Group header ──────────────────────────────────────────────────────────────

@Composable
private fun GroupHeader(
    group: Group,
    isMember: Boolean,
    currentUser: User?,
    onViewMembers: () -> Unit,
    onJoin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(AppColors.surface).padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (group.pictureUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(group.pictureUrl),
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(CircleShape).background(AppColors.divider),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = RyderAccent) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        group.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(group.name, color = AppColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (group.description.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                group.description,
                color = AppColors.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${group.memberIds.size} biedri",
            color = RyderAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onViewMembers)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        if (!isMember && currentUser != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(containerColor = RyderAccent, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Pievienoties") }
        }
    }
}

// ── Group post card ───────────────────────────────────────────────────────────

@Composable
private fun GroupPostCard(
    post: Post,
    isPinned: Boolean,
    isAdmin: Boolean,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPinned) RyderAccent.copy(alpha = 0.12f) else AppColors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (post.user.profilePicture != null) {
                Image(
                    painter = rememberAsyncImagePainter(post.user.profilePicture),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(AppColors.divider),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = RyderAccent) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            post.user.nickname.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.user.nickname,
                    color = AppColors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(formatGroupPostTime(post.createdAt), color = AppColors.textSecondary, fontSize = 11.sp)
            }
            if (isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = null,
                    tint = RyderAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            if (isAdmin) {
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!isPinned) {
                            DropdownMenuItem(
                                text = { Text("Piespraust") },
                                onClick = { showMenu = false; onPin() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Atspraust") },
                                onClick = { showMenu = false; onUnpin() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Noņemt", color = Color(0xFFE53935)) },
                            onClick = { showMenu = false; onRemove() }
                        )
                    }
                }
            }
        }
        if (post.description.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(post.description, color = AppColors.textPrimary, fontSize = 14.sp)
        }
        if (post.mediaUrls.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Image(
                painter = rememberAsyncImagePainter(post.mediaUrls.first()),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.divider),
                contentScale = ContentScale.Crop
            )
        }
    }
    HorizontalDivider(color = AppColors.divider)
}

// ── Create group post dialog ──────────────────────────────────────────────────

@Composable
private fun CreateGroupPostDialog(
    currentUser: User,
    repo: GroupRepository,
    groupId: String,
    onDismiss: () -> Unit,
    onPosted: (Post) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var isPosting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { mediaUri = it }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.surface)
                .padding(16.dp)
        ) {
            Text("Jauns ieraksts", color = AppColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Ko vēlies dalīties?", color = AppColors.inputBorder) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.textPrimary,
                    unfocusedTextColor = AppColors.textPrimary,
                    focusedBorderColor = RyderAccent,
                    unfocusedBorderColor = AppColors.inputBorder,
                    cursorColor = RyderAccent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrect = false),
                maxLines = 6
            )
            if (mediaUri != null) {
                Spacer(Modifier.height(8.dp))
                Box {
                    Image(
                        painter = rememberAsyncImagePainter(mediaUri),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { mediaUri = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = AppColors.textPrimary)
                    }
                }
            }
            if (errorMsg != null) {
                Spacer(Modifier.height(6.dp))
                Text(errorMsg!!, color = Color(0xFFE53935), fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = "Pievienot attēlu", tint = AppColors.textSecondary)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Atcelt", color = AppColors.textSecondary) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (text.isBlank() && mediaUri == null) return@Button
                        isPosting = true
                        errorMsg = null
                        scope.launch {
                            try {
                                withTimeout(15_000L) {
                                    val urls = if (mediaUri != null) {
                                        listOf(repo.uploadGroupPostMedia(mediaUri!!, groupId))
                                    } else emptyList()
                                    val newPost = Post(
                                        userId = currentUser.uid,
                                        user = currentUser,
                                        description = text.trim(),
                                        mediaUrls = urls
                                    )
                                    val saved = repo.createGroupPost(groupId, newPost)
                                    onPosted(saved)
                                }
                            } catch (_: Exception) {
                                isPosting = false
                                errorMsg = "Neizdevās publicēt. Pārbaudi savienojumu."
                            }
                        }
                    },
                    enabled = !isPosting && (text.isNotBlank() || mediaUri != null),
                    colors = ButtonDefaults.buttonColors(containerColor = RyderAccent, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isPosting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    else Text("Publicēt")
                }
            }
        }
    }
}

// ── Invite user dialog ────────────────────────────────────────────────────────

@Composable
private fun InviteUserDialog(
    repo: GroupRepository,
    group: Group,
    onDismiss: () -> Unit,
    onInvited: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<User>>(emptyList()) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        results = if (query.length >= 2) {
            try { repo.searchUsers(query, group.memberIds + group.bannedIds + group.inviteIds) }
            catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.surface)
                .padding(16.dp)
        ) {
            Text("Uzaicināt lietotāju", color = AppColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; successMsg = null },
                placeholder = { Text("Meklēt lietotāju...", color = AppColors.inputBorder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.textPrimary,
                    unfocusedTextColor = AppColors.textPrimary,
                    focusedBorderColor = RyderAccent,
                    unfocusedBorderColor = AppColors.inputBorder,
                    cursorColor = RyderAccent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrect = false),
                singleLine = true
            )
            successMsg?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFF4CAF50), fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            results.forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                try {
                                    repo.sendInvite(group.id, user.uid)
                                    successMsg = "${user.nickname} saņēma ielūgumu"
                                    query = ""
                                    results = emptyList()
                                    onInvited()
                                } catch (_: Exception) {}
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pic = user.profilePicture
                    if (pic != null) {
                        Image(
                            painter = rememberAsyncImagePainter(pic),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(AppColors.divider),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = RyderAccent) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    user.nickname.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(user.nickname, color = AppColors.textPrimary, fontSize = 15.sp)
                }
                HorizontalDivider(color = AppColors.divider)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Aizvērt", color = AppColors.textSecondary)
            }
        }
    }
}

// ── Manage members dialog ─────────────────────────────────────────────────────

@Composable
private fun ManageMembersDialog(
    group: Group,
    repo: GroupRepository,
    currentUserId: String,
    isOwner: Boolean,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    var members by remember { mutableStateOf<List<User>>(emptyList()) }
    var localAdminIds by remember { mutableStateOf(group.adminIds) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(group.memberIds.size) {
        members = try { repo.getUsersByIds(group.memberIds) } catch (_: Exception) { emptyList() }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Biedri (${members.size})",
                    color = AppColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("Aizvērt", color = AppColors.textSecondary) }
            }
            HorizontalDivider(color = AppColors.divider)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(members, key = { it.uid }) { member ->
                    val isThisOwner = member.uid == group.ownerId
                    val isMemberAdmin = member.uid in localAdminIds
                    var showMemberMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pic = member.profilePicture
                        if (pic != null) {
                            Image(
                                painter = rememberAsyncImagePainter(pic),
                                contentDescription = null,
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(AppColors.divider),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = RyderAccent) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        member.nickname.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                member.nickname,
                                color = AppColors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = when {
                                    isThisOwner -> "Īpašnieks"
                                    isMemberAdmin -> "Administrators"
                                    else -> "Biedrs"
                                },
                                color = if (isThisOwner) RyderAccent else AppColors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        // Owner can manage everyone except themselves.
                        // Admins can only manage regular members (not owner, not other admins, not themselves).
                        val canManage = !isThisOwner && member.uid != currentUserId &&
                            (isOwner || (isAdmin && !isMemberAdmin))
                        if (canManage) {
                            Box {
                                IconButton(onClick = { showMemberMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = AppColors.textSecondary)
                                }
                                DropdownMenu(
                                    expanded = showMemberMenu,
                                    onDismissRequest = { showMemberMenu = false }
                                ) {
                                    if (isOwner) {
                                        if (!isMemberAdmin) {
                                            DropdownMenuItem(
                                                text = { Text("Padarīt par administratoru") },
                                                onClick = {
                                                    showMemberMenu = false
                                                    scope.launch {
                                                        try {
                                                            repo.makeAdmin(group.id, member.uid)
                                                            localAdminIds = localAdminIds + member.uid
                                                            onChanged()
                                                        } catch (_: Exception) {}
                                                    }
                                                }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text("Noņemt administratora tiesības") },
                                                onClick = {
                                                    showMemberMenu = false
                                                    scope.launch {
                                                        try {
                                                            repo.removeAdmin(group.id, member.uid)
                                                            localAdminIds = localAdminIds - member.uid
                                                            onChanged()
                                                        } catch (_: Exception) {}
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Izmest no grupas", color = Color(0xFFE53935)) },
                                        onClick = {
                                            showMemberMenu = false
                                            scope.launch {
                                                try {
                                                    repo.removeMember(group.id, member.uid)
                                                    members = members.filter { it.uid != member.uid }
                                                    onChanged()
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Aizliegt piekļuvi", color = Color(0xFFE53935)) },
                                        onClick = {
                                            showMemberMenu = false
                                            scope.launch {
                                                try {
                                                    repo.banUser(group.id, member.uid)
                                                    members = members.filter { it.uid != member.uid }
                                                    onChanged()
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = AppColors.divider)
                }
            }
        }
    }
}

// ── Edit group dialog ─────────────────────────────────────────────────────────

@Composable
private fun EditGroupDialog(
    group: Group,
    repo: GroupRepository,
    onDismiss: () -> Unit,
    onSaved: (Group) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var description by remember { mutableStateOf(group.description) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AppColors.textPrimary,
        unfocusedTextColor = AppColors.textPrimary,
        focusedBorderColor = RyderAccent,
        unfocusedBorderColor = AppColors.inputBorder,
        focusedLabelColor = RyderAccent,
        unfocusedLabelColor = AppColors.textSecondary,
        cursorColor = RyderAccent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surface,
        title = { Text("Rediģēt grupu", color = AppColors.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nosaukums") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Apraksts") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) return@TextButton
                    isSaving = true
                    scope.launch {
                        try {
                            val updated = group.copy(name = name.trim(), description = description.trim())
                            repo.updateGroup(updated)
                            onSaved(updated)
                        } catch (_: Exception) { isSaving = false }
                    }
                },
                enabled = !isSaving && name.isNotBlank()
            ) { Text("Saglabāt", color = RyderAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Atcelt", color = AppColors.textSecondary) }
        }
    )
}

// ── View members dialog ───────────────────────────────────────────────────────

@Composable
private fun ViewMembersDialog(
    group: Group,
    repo: GroupRepository,
    onDismiss: () -> Unit
) {
    var members by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(group.id) {
        members = try { repo.getUsersByIds(group.memberIds) } catch (_: Exception) { emptyList() }
        isLoading = false
    }

    // Sort: owner first, then admins, then regular members
    val sorted = remember(members, group.ownerId, group.adminIds) {
        members.sortedWith(compareBy {
            when (it.uid) {
                group.ownerId -> 0
                in group.adminIds -> 1
                else -> 2
            }
        })
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Biedri (${group.memberIds.size})",
                    color = AppColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("Aizvērt", color = AppColors.textSecondary) }
            }
            HorizontalDivider(color = AppColors.divider)
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = RyderAccent) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sorted, key = { it.uid }) { member ->
                        val isOwner = member.uid == group.ownerId
                        val isAdmin = !isOwner && member.uid in group.adminIds
                        val roleLabel = when {
                            isOwner -> "Īpašnieks"
                            isAdmin -> "Administrators"
                            else -> "Biedrs"
                        }
                        val roleColor = when {
                            isOwner -> RyderAccent
                            isAdmin -> Color(0xFF2E7D32)
                            else -> AppColors.textSecondary
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val pic = member.profilePicture
                            if (pic != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(pic),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.divider),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = RyderAccent) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            member.nickname.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    member.nickname,
                                    color = AppColors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                val fullName = "${member.firstName} ${member.lastName}".trim()
                                if (fullName.isNotEmpty()) {
                                    Text(fullName, color = AppColors.textSecondary, fontSize = 12.sp)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = roleColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    roleLabel,
                                    color = roleColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = AppColors.divider)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatGroupPostTime(timeMillis: Long): String {
    if (timeMillis == 0L) return ""
    val sdf = SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}
