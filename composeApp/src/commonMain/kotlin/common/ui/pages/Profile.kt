@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import common.data.AuthService
import common.model.User

private val RyderRed = Color(0xFFD32F2F)

@Composable
fun ProfilePage(
    authService: AuthService,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    val userPosts = remember { mutableStateListOf<String>() }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = authService.getCurrentUserId() ?: return@LaunchedEffect
        val result = authService.getUserData(uid)
        if (result.isSuccess) {
            user = result.getOrNull()
            userPosts.addAll(
                listOf(
                    "https://picsum.photos/300/300?random=1",
                    "https://picsum.photos/300/300?random=2",
                    "https://picsum.photos/300/300?random=3",
                    "https://picsum.photos/300/300?random=4",
                    "https://picsum.photos/300/300?random=5"
                )
            )
        }
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
                        onClick = {
                            showMenu = false
                            onEditProfile()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Iziet") },
                        onClick = {
                            showMenu = false
                            authService.logout()
                            onLogout()
                        }
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile picture
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = user?.profilePicture ?: "https://picsum.photos/200"
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // Full name
                    val fullName = "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim()
                    if (fullName.isNotEmpty()) {
                        Text(text = fullName, color = Color.Gray, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Bike
                    val bike = user?.bike.orEmpty()
                    if (bike.isNotEmpty()) {
                        Text(
                            text = "🏍 $bike",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Bio
                    val bio = user?.bio.orEmpty()
                    if (bio.isNotEmpty()) {
                        Text(
                            text = bio,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(vertical = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Privacy badge
                    val privacy = user?.profilePrivacy.orEmpty()
                    if (privacy.isNotEmpty()) {
                        Text(
                            text = privacy,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(Color.DarkGray, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Stats row
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileStat(count = "120", label = "Ziņas")
                        ProfileStat(count = "450", label = "Sekotāji")
                        ProfileStat(count = "300", label = "Seko")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Edit Profile button
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

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Posts grid
            items(userPosts.chunked(3)) { rowPosts ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (postUrl in rowPosts) {
                        Image(
                            painter = rememberAsyncImagePainter(postUrl),
                            contentDescription = "Lietotāja ziņa",
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
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
        Text(text = count, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, color = Color.Gray)
    }
}