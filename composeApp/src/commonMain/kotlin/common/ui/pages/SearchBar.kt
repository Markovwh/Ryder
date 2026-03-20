@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import common.data.PostRepository
import common.model.User
import common.ui.pages.components.RyderRed

private enum class SearchTab(val title: String) {
    USERS("Lietotāji"),
    HASHTAGS("Tēmturi")
}

@Composable
fun SearchPage(
    currentUser: User?,
    onOpenUser: (String, String) -> Unit,
    onOpenHashtag: (String) -> Unit
) {
    val repository = remember { PostRepository() }
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(SearchTab.USERS) }
    var userResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var hashtagResults by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(query, selectedTab) {
        if (query.length < 2) {
            userResults = emptyList()
            hashtagResults = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        try {
            when (selectedTab) {
                SearchTab.USERS -> userResults = repository.searchUsers(query)
                SearchTab.HASHTAGS -> hashtagResults = repository.searchHashtags(query)
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Meklēt",
            color = RyderRed,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            shape = RoundedCornerShape(24.dp),
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Meklēt lietotājus vai #tēmturus", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = RyderRed,
                unfocusedBorderColor = Color.Gray,
                cursorColor = RyderRed
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Black,
            contentColor = RyderRed
        ) {
            SearchTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.title,
                            color = if (selectedTab == tab) RyderRed else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RyderRed, modifier = Modifier.size(24.dp))
                }
            }
            query.length < 2 -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ievadi vismaz 2 rakstzīmes...",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            selectedTab == SearchTab.USERS && userResults.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nav atrasts neviens lietotājs", color = Color.Gray, fontSize = 14.sp)
                }
            }
            selectedTab == SearchTab.HASHTAGS && hashtagResults.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nav atrasts neviens tēmturs", color = Color.Gray, fontSize = 14.sp)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    when (selectedTab) {
                        SearchTab.USERS -> {
                            items(userResults, key = { it.uid }) { user ->
                                UserSearchRow(
                                    user = user,
                                    onClick = { onOpenUser(user.uid, user.nickname) }
                                )
                                HorizontalDivider(color = Color(0xFF1E1E1E))
                            }
                        }
                        SearchTab.HASHTAGS -> {
                            items(hashtagResults, key = { it.first }) { (tag, count) ->
                                HashtagSearchRow(
                                    tag = tag,
                                    count = count,
                                    onClick = { onOpenHashtag(tag) }
                                )
                                HorizontalDivider(color = Color(0xFF1E1E1E))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSearchRow(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pic = user.profilePicture
        if (pic != null) {
            Image(
                painter = rememberAsyncImagePainter(pic),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = RyderRed) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.nickname.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.nickname,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            if (user.firstName.isNotEmpty() || user.lastName.isNotEmpty()) {
                Text(
                    text = "${user.firstName} ${user.lastName}".trim(),
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            if (user.bio.isNotEmpty()) {
                Text(
                    text = user.bio,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HashtagSearchRow(tag: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
                .border(1.dp, RyderRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("#", color = RyderRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "#$tag",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = "$count ierakst${if (count == 1) "s" else "i"}",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}
