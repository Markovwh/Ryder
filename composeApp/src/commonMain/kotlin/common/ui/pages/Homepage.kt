package common.ui.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.data.PostRepository
import common.data.UserRepository
import common.model.Post
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.PostCard
import common.ui.pages.components.RyderAccent

enum class SortOrder { NewestFirst, OldestFirst, MostPopular, LeastPopular }

@Composable
fun Homepage(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    isUserLoggedIn: Boolean,
    currentUser: User? = null,
    onUserClick: ((String, String) -> Unit)? = null
) {
    val repository = remember { PostRepository() }
    val userRepo = remember { UserRepository() }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var followingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var followerIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var friendsOnly by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.NewestFirst) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Fetch who the current user follows and who follows them so the feed can
    // apply "Privāts" / "Draugi" visibility rules correctly.
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            followingIds = currentUser.following.toSet()
            try { followerIds = userRepo.getFollowerIds(uid) } catch (_: Exception) {}
        } else {
            followingIds = emptySet()
            followerIds = emptySet()
            friendsOnly = false
        }
    }

    // Re-subscribe whenever the viewer's relationship sets change so the feed
    // immediately reflects new follows/unfollows.
    DisposableEffect(currentUser?.uid, followingIds, followerIds) {
        val unsub = repository.listenToPosts(
            currentUserId = currentUser?.uid,
            followingIds = followingIds,
            followerIds = followerIds
        ) { posts = it }
        onDispose { unsub() }
    }

    // Client-side friends filter + sort
    val displayedPosts: List<Post> = remember(posts, friendsOnly, followingIds, followerIds, currentUser, sortOrder) {
        val filtered = if (friendsOnly && currentUser != null) {
            val mutualIds = followingIds.intersect(followerIds)
            posts.filter { it.userId == currentUser.uid || it.userId in mutualIds }
        } else {
            posts
        }
        when (sortOrder) {
            SortOrder.NewestFirst  -> filtered
            SortOrder.OldestFirst  -> filtered.reversed()
            SortOrder.MostPopular  -> filtered.sortedByDescending { it.likeCount }
            SortOrder.LeastPopular -> filtered.sortedBy { it.likeCount }
        }
    }

    Scaffold(containerColor = AppColors.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ryder",
                    color = RyderAccent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isUserLoggedIn) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box {
                        Surface(
                            onClick = { showFilterMenu = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, AppColors.inputBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (friendsOnly) "Draugi" else "Visi",
                                    color = AppColors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AppColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            containerColor = AppColors.surface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Visi", color = AppColors.textPrimary) },
                                trailingIcon = if (!friendsOnly) {
                                    { Icon(Icons.Default.Check, null, tint = RyderAccent) }
                                } else null,
                                onClick = { friendsOnly = false; showFilterMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Draugi", color = AppColors.textPrimary) },
                                trailingIcon = if (friendsOnly) {
                                    { Icon(Icons.Default.Check, null, tint = RyderAccent) }
                                } else null,
                                onClick = { friendsOnly = true; showFilterMenu = false }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Surface(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, AppColors.inputBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (sortOrder) {
                                        SortOrder.NewestFirst  -> "Jaunākie"
                                        SortOrder.OldestFirst  -> "Vecākie"
                                        SortOrder.MostPopular  -> "Populārākie"
                                        SortOrder.LeastPopular -> "Mazāk populāri"
                                    },
                                    color = AppColors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AppColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = AppColors.surface
                        ) {
                            listOf(
                                SortOrder.NewestFirst  to "Jaunākie",
                                SortOrder.OldestFirst  to "Vecākie",
                                SortOrder.MostPopular  to "Populārākie",
                                SortOrder.LeastPopular to "Mazāk populāri"
                            ).forEach { (order, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = AppColors.textPrimary) },
                                    trailingIcon = if (sortOrder == order) {
                                        { Icon(Icons.Default.Check, null, tint = RyderAccent) }
                                    } else null,
                                    onClick = { sortOrder = order; showSortMenu = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!isUserLoggedIn) {
                    TextButton(onClick = onLoginClick) {
                        Text("Pieslēgties", color = AppColors.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRegisterClick,
                        colors = ButtonDefaults.buttonColors(containerColor = RyderAccent)
                    ) {
                        Text("Reģistrēties", color = Color.White)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(displayedPosts, key = { it.id }) { post ->
                    PostCard(post = post, currentUser = currentUser, onUserClick = onUserClick)
                }
            }
        }
    }
}
