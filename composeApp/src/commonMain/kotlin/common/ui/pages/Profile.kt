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
import common.ui.pages.components.RyderAccent

private val TileBackground = Color(0xFFF0F0F0)

@Composable
fun ProfilePage(
    authService: AuthService,
    initialUser: User? = null,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenAdmin: (() -> Unit)? = null
) {
    var user by remember { mutableStateOf(initialUser) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoadingPosts by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    val repository = remember { PostRepository() }

    LaunchedEffect(initialUser?.uid) {
        val uid = initialUser?.uid ?: authService.getCurrentUserId() ?: return@LaunchedEffect
        if (initialUser == null) {
            val result = authService.getUserData(uid)
            if (result.isSuccess) user = result.getOrNull()
        }
        isLoadingPosts = true
        try { posts = repository.getPostsByUser(uid) } catch (_: Exception) {}
        isLoadingPosts = false
    }

    LaunchedEffect(initialUser) {
        if (initialUser != null) user = initialUser
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE))
    ) {
        TopAppBar(
            title = { Text(user?.nickname ?: "Profils", color = Color(0xFF1A1A1A)) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F5F5)),
            actions = {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Iestatījumi", tint = Color(0xFF1A1A1A))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rediģēt profilu") },
                        onClick = { showMenu = false; onEditProfile() }
                    )
                    if (user?.isAdmin == true && onOpenAdmin != null) {
                        DropdownMenuItem(
                            text = { Text("Administratora panelis") },
                            onClick = { showMenu = false; onOpenAdmin() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Iziet") },
                        onClick = { showMenu = false; authService.logout(); onLogout() }
                    )
                }
            }
        )

        val postRows = posts.chunked(3)

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(top = 16.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            user?.profilePicture ?: "https://picsum.photos/200"
                        ),
                        contentDescription = "Profila bilde",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD0D0D0)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user?.nickname ?: "Lietotājvārds",
                        color = Color(0xFF1A1A1A),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val fullName = "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim()
                    if (fullName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = fullName, color = Color(0xFF757575), fontSize = 15.sp)
                    }

                    val bike = user?.bike.orEmpty()
                    if (bike.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "🏍 $bike", color = Color(0xFF555555), fontSize = 14.sp)
                    }

                    val exp = user?.experienceYears ?: 0
                    if (exp > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⏱ $exp ${if (exp == 1) "gads" else "gadi"} pieredze",
                            color = Color(0xFF555555),
                            fontSize = 14.sp
                        )
                    }

                    val bio = user?.bio.orEmpty()
                    if (bio.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bio,
                            color = Color(0xFF1A1A1A),
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
                            color = Color(0xFF757575),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = Color(0xFFD9D9D9))
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
                    HorizontalDivider(color = Color(0xFFD9D9D9))

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onEditProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = RyderAccent),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = "Rediģēt profilu",
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (isLoadingPosts) {
                item {
                    CircularProgressIndicator(
                        color = RyderAccent,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else if (posts.isEmpty()) {
                item {
                    Text(
                        text = "Vēl nav nevienas ziņas",
                        color = Color(0xFF757575),
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
}

@Composable
private fun PostGridTile(post: Post, modifier: Modifier, onClick: () -> Unit = {}) {
    Box(modifier = modifier.background(Color(0xFFD0D0D0)).clickable(onClick = onClick)) {
        if (post.mediaUrls.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(post.mediaUrls.first()),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TileBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.description,
                    color = Color(0xFF1A1A1A),
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
        Text(text = count, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A), fontSize = 18.sp)
        Text(text = label, color = Color(0xFF757575), fontSize = 12.sp)
    }
}
