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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Settings
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
import coil.compose.rememberAsyncImagePainter
import common.data.AuthService
import common.data.PostRepository
import common.data.UserRepository
import common.model.Post
import common.model.User
import kotlinx.coroutines.launch
import common.ui.pages.components.AppColors
import common.ui.pages.components.PostDetailDialog
import common.ui.pages.components.RyderAccent
import common.ui.pages.components.UserAvatar

@Composable
fun ProfilePage(
    authService: AuthService,
    initialUser: User? = null,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: (() -> Unit)? = null,
    onUserRefreshed: ((User) -> Unit)? = null,
    onOpenUser: ((String, String) -> Unit)? = null
) {
    val bg = AppColors.background
    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val dividerColor = AppColors.divider
    val tileBg = AppColors.tileBackground

    var user by remember { mutableStateOf(initialUser) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoadingPosts by remember { mutableStateOf(true) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var showFollowList by remember { mutableStateOf<FollowListType?>(null) }
    var followRequestUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    val repository = remember { PostRepository() }
    val userRepo = remember { UserRepository() }

    LaunchedEffect(initialUser?.uid) {
        val uid = initialUser?.uid ?: authService.getCurrentUserId() ?: return@LaunchedEffect
        val result = authService.getUserData(uid)
        if (result.isSuccess) {
            val fresh = result.getOrNull()
            if (fresh != null) {
                user = fresh
                onUserRefreshed?.invoke(fresh)
            }
        }
        isLoadingPosts = true
        try { posts = repository.getPostsByUser(uid) } catch (_: Exception) {}
        isLoadingPosts = false
        if (result.getOrNull()?.profilePrivacy == "Privāts") {
            try { followRequestUsers = userRepo.getFollowRequestUsers(uid) } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        TopAppBar(
            title = { Text(user?.nickname ?: "Profils", color = textPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = surface),
            actions = {
                if (user?.isAdmin == true && onOpenAdmin != null) {
                    IconButton(onClick = onOpenAdmin) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = "Administratora panelis",
                            tint = RyderAccent
                        )
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Iestatījumi", tint = textPrimary)
                }
            }
        )

        val postRows = posts.chunked(3)

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(bg),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().background(surface).padding(top = 16.dp)
                ) {
                    UserAvatar(
                        profilePicture = user?.profilePicture,
                        nickname = user?.nickname ?: "",
                        size = 100.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user?.nickname ?: "Lietotājvārds",
                        color = textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val fullName = "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim()
                    if (fullName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = fullName, color = textSecondary, fontSize = 15.sp)
                    }

                    val bike = user?.bike.orEmpty()
                    if (bike.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "🏍 $bike", color = textSecondary, fontSize = 14.sp)
                    }

                    val exp = user?.experienceYears ?: 0
                    if (exp > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⏱ $exp ${if (exp == 1) "gads" else "gadi"} pieredze",
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    }

                    val bio = user?.bio.orEmpty()
                    if (bio.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bio,
                            color = textPrimary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(vertical = 2.dp)
                        )
                    }

                    val privacy = user?.profilePrivacy.orEmpty()
                    if (privacy.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = privacy,
                            color = textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(AppColors.tagBackground, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = dividerColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat(
                            count = if (isLoadingPosts) "…" else posts.size.toString(),
                            label = "Ziņas"
                        )
                        ProfileStat(
                            count = (user?.followerCount ?: 0).toString(),
                            label = "Sekotāji",
                            onClick = { showFollowList = FollowListType.FOLLOWERS }
                        )
                        ProfileStat(
                            count = (user?.followingCount ?: 0).toString(),
                            label = "Seko",
                            onClick = { showFollowList = FollowListType.FOLLOWING }
                        )
                    }
                    HorizontalDivider(color = dividerColor)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onEditProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = RyderAccent),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(40.dp)
                    ) {
                        Text(text = "Rediģēt profilu", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Follow requests section (private accounts only) ──────────────
            if (followRequestUsers.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "Sekošanas pieprasījumi (${followRequestUsers.size})",
                            color = textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    HorizontalDivider(color = dividerColor)
                }
                items(followRequestUsers, key = { "req_${it.uid}" }) { requester ->
                    val scope = rememberCoroutineScope()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surface)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            profilePicture = requester.profilePicture,
                            nickname = requester.nickname,
                            size = 44.dp,
                            modifier = if (onOpenUser != null) Modifier.clickable { onOpenUser.invoke(requester.uid, requester.nickname) } else Modifier
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(requester.nickname, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (requester.firstName.isNotEmpty() || requester.lastName.isNotEmpty()) {
                                Text("${requester.firstName} ${requester.lastName}".trim(), color = textSecondary, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val uid = user?.uid ?: return@TextButton
                                scope.launch {
                                    try {
                                        userRepo.declineFollowRequest(uid, requester.uid)
                                        followRequestUsers = followRequestUsers.filter { it.uid != requester.uid }
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = textSecondary)
                        ) { Text("Noraidīt", fontSize = 13.sp) }
                        Button(
                            onClick = {
                                val uid = user?.uid ?: return@Button
                                scope.launch {
                                    try {
                                        userRepo.acceptFollowRequest(uid, requester.uid)
                                        followRequestUsers = followRequestUsers.filter { it.uid != requester.uid }
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RyderAccent, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("Pieņemt", fontSize = 13.sp) }
                    }
                    HorizontalDivider(color = dividerColor)
                }
            }

            if (isLoadingPosts) {
                item {
                    CircularProgressIndicator(color = RyderAccent, modifier = Modifier.padding(32.dp))
                }
            } else if (posts.isEmpty()) {
                item {
                    Text(
                        text = "Vēl nav nevienas ziņas",
                        color = textSecondary,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                itemsIndexed(postRows) { _, rowPosts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        rowPosts.forEach { post ->
                            PostGridTile(
                                post = post,
                                tileBg = tileBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                onClick = { selectedPost = post }
                            )
                        }
                        repeat(3 - rowPosts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    selectedPost?.let { post ->
        PostDetailDialog(
            post = post,
            currentUser = user,
            onDismiss = { selectedPost = null },
            onDeleted = {
                posts = posts.filter { it.id != post.id }
                selectedPost = null
            }
        )
    }

    showFollowList?.let { type ->
        val uid = user?.uid
        if (uid != null) {
            FollowListDialog(
                userId = uid,
                type = type,
                userRepo = userRepo,
                onDismiss = { showFollowList = null },
                onOpenUser = { targetUid, nickname ->
                    showFollowList = null
                    onOpenUser?.invoke(targetUid, nickname)
                }
            )
        }
    }
}

@Composable
private fun PostGridTile(post: Post, tileBg: Color, modifier: Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .background(tileBg)
            .border(1.dp, Color.White)
            .clickable(onClick = onClick)
    ) {
        if (post.mediaUrls.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(post.mediaUrls.first()),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(tileBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.description,
                    color = AppColors.textPrimary,
                    fontSize = 11.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

@Composable
fun RowScope.ProfileStat(count: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(text = count, fontWeight = FontWeight.Bold, color = AppColors.textPrimary, fontSize = 18.sp)
        Text(text = label, color = AppColors.textSecondary, fontSize = 12.sp)
    }
}
