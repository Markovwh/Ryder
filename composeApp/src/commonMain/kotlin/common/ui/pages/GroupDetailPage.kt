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
import common.data.GroupRepository
import common.model.Group
import common.model.Post
import common.model.User
import common.ui.pages.components.RyderAccent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupDetailPage(
    groupId: String,
    currentUser: User?,
    onBack: () -> Unit
) {
    val repo = remember { GroupRepository() }
    val scope = rememberCoroutineScope()
    var group by remember { mutableStateOf<Group?>(null) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPostDialog by remember { mutableStateOf(false) }
    var showDeleteGroupConfirm by remember { mutableStateOf(false) }

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
        containerColor = Color(0xFFEEEEEE),
        topBar = {
            Surface(color = Color(0xFFF5F5F5)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atpakaļ", tint = Color(0xFF1A1A1A))
                    }
                    Text(
                        text = g?.name ?: "Grupa",
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentUser != null && g != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Vairāk", tint = Color(0xFF757575))
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
                Text("Grupa nav atrasta", color = Color(0xFF757575))
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
                    onJoin = {
                        scope.launch {
                            try {
                                repo.addMember(groupId, currentUser!!.uid)
                                group = repo.getGroup(groupId)
                            } catch (_: Exception) {}
                        }
                    }
                )
                HorizontalDivider(color = Color(0xFFD9D9D9))
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
                            color = Color(0xFF1A1A1A),
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
                item { HorizontalDivider(color = Color(0xFFD9D9D9)) }
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
                            color = Color(0xFF757575),
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
            onDismiss = { showMembersDialog = false },
            onChanged = { scope.launch { group = repo.getGroup(groupId) } }
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
            containerColor = Color(0xFFF5F5F5),
            title = { Text("Dzēst grupu?", color = Color(0xFF1A1A1A)) },
            text = { Text("Šo darbību nevar atsaukt. Visi ieraksti tiks dzēsti.", color = Color(0xFF757575)) },
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
                    Text("Atcelt", color = Color(0xFF757575))
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
    onJoin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (group.pictureUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(group.pictureUrl),
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFD0D0D0)),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = RyderAccent) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        group.name.take(1).uppercase(),
                        color = Color(0xFF1A1A1A),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(group.name, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (group.description.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                group.description,
                color = Color(0xFF757575),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("${group.memberIds.size} biedri", color = Color(0xFF757575), fontSize = 13.sp)
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
            .background(if (isPinned) Color(0xFFECFCD3) else Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (post.user.profilePicture != null) {
                Image(
                    painter = rememberAsyncImagePainter(post.user.profilePicture),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFD0D0D0)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = RyderAccent) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            post.user.nickname.take(1).uppercase(),
                            color = Color(0xFF1A1A1A),
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
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(formatGroupPostTime(post.createdAt), color = Color(0xFF757575), fontSize = 11.sp)
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
                            tint = Color(0xFF757575),
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
            Text(post.description, color = Color(0xFF1A1A1A), fontSize = 14.sp)
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
                    .background(Color(0xFFD0D0D0)),
                contentScale = ContentScale.Crop
            )
        }
    }
    HorizontalDivider(color = Color(0xFFD9D9D9))
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
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { mediaUri = it }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            Text("Jauns ieraksts", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Ko vēlies dalīties?", color = Color(0xFF9E9E9E)) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1A1A1A),
                    unfocusedTextColor = Color(0xFF1A1A1A),
                    focusedBorderColor = RyderAccent,
                    unfocusedBorderColor = Color(0xFF9E9E9E),
                    cursorColor = RyderAccent
                ),
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
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF1A1A1A))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = "Pievienot attēlu", tint = Color(0xFF757575))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Atcelt", color = Color(0xFF757575)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (text.isBlank() && mediaUri == null) return@Button
                        isPosting = true
                        scope.launch {
                            try {
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
                            } catch (_: Exception) { isPosting = false }
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
            try { repo.searchUsers(query, group.memberIds + group.bannedIds) }
            catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            Text("Uzaicināt lietotāju", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; successMsg = null },
                placeholder = { Text("Meklēt lietotāju...", color = Color(0xFF9E9E9E)) },
                modifier = Modifier.fillMaxWidth(),
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
                                    repo.addMember(group.id, user.uid)
                                    successMsg = "${user.nickname} pievienots grupai"
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
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFD0D0D0)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = RyderAccent) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    user.nickname.take(1).uppercase(),
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(user.nickname, color = Color(0xFF1A1A1A), fontSize = 15.sp)
                }
                HorizontalDivider(color = Color(0xFFD9D9D9))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Aizvērt", color = Color(0xFF757575))
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
                .background(Color(0xFFF5F5F5))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Biedri (${members.size})",
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("Aizvērt", color = Color(0xFF757575)) }
            }
            HorizontalDivider(color = Color(0xFFD9D9D9))
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
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFD0D0D0)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = RyderAccent) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        member.nickname.take(1).uppercase(),
                                        color = Color(0xFF1A1A1A),
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
                                color = Color(0xFF1A1A1A),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = when {
                                    isThisOwner -> "Īpašnieks"
                                    isMemberAdmin -> "Administrators"
                                    else -> "Biedrs"
                                },
                                color = if (isThisOwner) RyderAccent else Color(0xFF757575),
                                fontSize = 12.sp
                            )
                        }
                        if (isOwner && !isThisOwner && member.uid != currentUserId) {
                            Box {
                                IconButton(onClick = { showMemberMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color(0xFF757575))
                                }
                                DropdownMenu(
                                    expanded = showMemberMenu,
                                    onDismissRequest = { showMemberMenu = false }
                                ) {
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
                    HorizontalDivider(color = Color(0xFFD9D9D9))
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
        focusedTextColor = Color(0xFF1A1A1A),
        unfocusedTextColor = Color(0xFF1A1A1A),
        focusedBorderColor = RyderAccent,
        unfocusedBorderColor = Color(0xFF9E9E9E),
        focusedLabelColor = RyderAccent,
        unfocusedLabelColor = Color(0xFF757575),
        cursorColor = RyderAccent
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F5F5),
        title = { Text("Rediģēt grupu", color = Color(0xFF1A1A1A)) },
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
            TextButton(onClick = onDismiss) { Text("Atcelt", color = Color(0xFF757575)) }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatGroupPostTime(timeMillis: Long): String {
    if (timeMillis == 0L) return ""
    val sdf = SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}
