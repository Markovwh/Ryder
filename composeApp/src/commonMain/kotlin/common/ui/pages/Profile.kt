@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import coil.compose.rememberAsyncImagePainter
import common.data.AuthService
import common.data.PostRepository
import common.model.Post
import common.model.User
import common.ui.pages.components.PostDetailDialog

private val RyderRed = Color(0xFFD32F2F)
private val TileBackground = Color(0xFF1A1A1A)

@Composable
fun ProfilePage(
    authService: AuthService,
    initialUser: User? = null,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit
) {
    var user by remember { mutableStateOf(initialUser) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoadingPosts by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    val repository = remember { PostRepository() }

    // Load user data if not pre-loaded, then fetch posts
    LaunchedEffect(initialUser?.uid) {
        val uid = initialUser?.uid ?: authService.getCurrentUserId() ?: return@LaunchedEffect

        if (initialUser == null) {
            val result = authService.getUserData(uid)
            if (result.isSuccess) user = result.getOrNull()
        }

        isLoadingPosts = true
        try {
            posts = repository.getPostsByUser(uid)
        } catch (_: Exception) {}
        isLoadingPosts = false
    }

    // Sync if the pre-loaded user changes (e.g. after edit profile)
    LaunchedEffect(initialUser) {
        if (initialUser != null) user = initialUser
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = { Text(user?.nickname ?: "Profils", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            actions = {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Iestatījumi", tint = Color.White)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rediģēt profilu") },
                        onClick = { showMenu = false; onEditProfile() }
                    )
                    DropdownMenuItem(
                        text = { Text("Iziet") },
                        onClick = { showMenu = false; authService.logout(); onLogout() }
                    )
                }
            }
        )

        val postRows = posts.chunked(3)

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Profile header ────────────────────────────────────────────────
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    // Avatar
                    Image(
                        painter = rememberAsyncImagePainter(
                            user?.profilePicture ?: "https://picsum.photos/200"
                        ),
                        contentDescription = "Profila bilde",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nickname
                    Text(
                        text = user?.nickname ?: "Lietotājvārds",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Full name
                    val fullName = "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim()
                    if (fullName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = fullName, color = Color.Gray, fontSize = 15.sp)
                    }

                    // Bike
                    val bike = user?.bike.orEmpty()
                    if (bike.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "🏍 $bike", color = Color.LightGray, fontSize = 14.sp)
                    }

                    // Bio
                    val bio = user?.bio.orEmpty()
                    if (bio.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bio,
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(vertical = 2.dp)
                        )
                    }

                    // Privacy badge
                    val privacy = user?.profilePrivacy.orEmpty()
                    if (privacy.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = privacy,
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(Color.DarkGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Stats row ─────────────────────────────────────────────
                    HorizontalDivider(color = Color(0xFF2D2D2D))
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
                            label = "Sekotāji"
                        )
                        ProfileStat(
                            count = (user?.followingCount ?: 0).toString(),
                            label = "Seko"
                        )
                    }
                    HorizontalDivider(color = Color(0xFF2D2D2D))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Edit profile button
                    Button(
                        onClick = onEditProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = RyderRed),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = "Rediģēt profilu",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Posts grid ────────────────────────────────────────────────────
            if (isLoadingPosts) {
                item {
                    CircularProgressIndicator(
                        color = RyderRed,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else if (posts.isEmpty()) {
                item {
                    Text(
                        text = "Vēl nav nevienas ziņas",
                        color = Color.Gray,
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
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                onClick = { selectedPost = post }
                            )
                        }
                        // Fill empty slots in the last row
                        repeat(3 - rowPosts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Post detail popup
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
}

@Composable
private fun PostGridTile(post: Post, modifier: Modifier, onClick: () -> Unit = {}) {
    Box(modifier = modifier.background(Color.DarkGray).clickable(onClick = onClick)) {
        if (post.mediaUrls.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(post.mediaUrls.first()),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Text-only post tile
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TileBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.description,
                    color = Color.White,
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
fun RowScope.ProfileStat(count: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(text = count, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}
