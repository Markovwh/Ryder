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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import common.data.EventRepository
import common.data.GroupRepository
import common.data.PostRepository
import common.data.RecentSearchesStore
import common.model.Event
import common.model.Group
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.RyderAccent

private enum class SearchTab(val title: String) {
    USERS("Lietotāji"),
    HASHTAGS("Tēmturi"),
    GROUPS("Grupas"),
    EVENTS("Pasākumi")
}

@Composable
fun SearchPage(
    currentUser: User?,
    onOpenUser: (String, String) -> Unit,
    onOpenHashtag: (String) -> Unit,
    onOpenGroup: (String) -> Unit = {},
    onOpenEvent: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { PostRepository() }
    val groupRepo = remember { GroupRepository() }
    val eventRepo = remember { EventRepository() }
    // Keyed on uid so the store is re-created when the logged-in user changes
    val recentStore = remember(currentUser?.uid) { RecentSearchesStore(context, currentUser?.uid ?: "") }

    val bg = AppColors.background
    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val textHint = AppColors.textHint
    val divColor = AppColors.divider
    val tagBg = AppColors.tagBackground

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(SearchTab.USERS) }

    var userResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var hashtagResults by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var groupResults by remember { mutableStateOf<List<Group>>(emptyList()) }
    var eventResults by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var recentUsers by remember { mutableStateOf(recentStore.getRecentUsers()) }
    var recentHashtags by remember { mutableStateOf(recentStore.getRecentHashtags()) }
    var recentGroups by remember { mutableStateOf(recentStore.getRecentGroups()) }
    var recentEvents by remember { mutableStateOf(recentStore.getRecentEvents()) }

    fun reloadRecents() {
        recentUsers = recentStore.getRecentUsers()
        recentHashtags = recentStore.getRecentHashtags()
        recentGroups = recentStore.getRecentGroups()
        recentEvents = recentStore.getRecentEvents()
    }

    LaunchedEffect(selectedTab) { reloadRecents() }

    LaunchedEffect(query, selectedTab) {
        if (query.length < 2) {
            userResults = emptyList(); hashtagResults = emptyList()
            groupResults = emptyList(); eventResults = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        try {
            when (selectedTab) {
                SearchTab.USERS -> userResults = repository.searchUsers(query)
                SearchTab.HASHTAGS -> hashtagResults = repository.searchHashtags(query)
                SearchTab.GROUPS -> groupResults = groupRepo.searchGroups(query)
                SearchTab.EVENTS -> eventResults = eventRepo.searchEvents(query)
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    val showRecents = query.length < 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Meklēt",
            color = RyderAccent,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            shape = RoundedCornerShape(24.dp),
            value = query,
            onValueChange = {
                query = it
                if (it.startsWith("#")) selectedTab = SearchTab.HASHTAGS
            },
            placeholder = { Text("Meklēt lietotājus, grupas, pasākumus...", color = textHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Notīrīt", tint = textSecondary)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrect = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedBorderColor = RyderAccent,
                unfocusedBorderColor = AppColors.inputBorder,
                cursorColor = RyderAccent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = surface,
            contentColor = RyderAccent
        ) {
            SearchTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.title,
                            color = if (selectedTab == tab) RyderAccent else textSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RyderAccent, modifier = Modifier.size(24.dp))
                }
            }

            showRecents -> {
                val hasRecents = when (selectedTab) {
                    SearchTab.USERS -> recentUsers.isNotEmpty()
                    SearchTab.HASHTAGS -> recentHashtags.isNotEmpty()
                    SearchTab.GROUPS -> recentGroups.isNotEmpty()
                    SearchTab.EVENTS -> recentEvents.isNotEmpty()
                }

                if (!hasRecents) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Ievadi vismaz 2 rakstzīmes...", color = textSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            Text(
                                text = "Nesenie meklējumi",
                                color = textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                        when (selectedTab) {
                            SearchTab.USERS -> items(recentUsers, key = { "r_${it.uid}" }) { user ->
                                RecentUserRow(
                                    user = user,
                                    onClick = { recentStore.addRecentUser(user); reloadRecents(); onOpenUser(user.uid, user.nickname) },
                                    onRemove = { recentStore.removeRecentUser(user.uid); reloadRecents() }
                                )
                                HorizontalDivider(color = divColor)
                            }
                            SearchTab.HASHTAGS -> items(recentHashtags, key = { "r_${it.first}" }) { (tag, count) ->
                                RecentHashtagRow(
                                    tag = tag, count = count,
                                    onClick = { recentStore.addRecentHashtag(tag, count); reloadRecents(); onOpenHashtag(tag) },
                                    onRemove = { recentStore.removeRecentHashtag(tag); reloadRecents() }
                                )
                                HorizontalDivider(color = divColor)
                            }
                            SearchTab.GROUPS -> items(recentGroups, key = { "r_${it.id}" }) { group ->
                                RecentGroupRow(
                                    group = group,
                                    onClick = { recentStore.addRecentGroup(group); reloadRecents(); onOpenGroup(group.id) },
                                    onRemove = { recentStore.removeRecentGroup(group.id); reloadRecents() }
                                )
                                HorizontalDivider(color = divColor)
                            }
                            SearchTab.EVENTS -> items(recentEvents, key = { "r_${it.id}" }) { event ->
                                RecentEventRow(
                                    event = event,
                                    onClick = { recentStore.addRecentEvent(event); reloadRecents(); onOpenEvent(event.id) },
                                    onRemove = { recentStore.removeRecentEvent(event.id); reloadRecents() }
                                )
                                HorizontalDivider(color = divColor)
                            }
                        }
                    }
                }
            }

            selectedTab == SearchTab.USERS && userResults.isEmpty() -> EmptyResult("Nav atrasts neviens lietotājs")
            selectedTab == SearchTab.HASHTAGS && hashtagResults.isEmpty() -> EmptyResult("Nav atrasts neviens tēmturs")
            selectedTab == SearchTab.GROUPS && groupResults.isEmpty() -> EmptyResult("Nav atrasta neviena grupa")
            selectedTab == SearchTab.EVENTS && eventResults.isEmpty() -> EmptyResult("Nav atrasts neviens pasākums")

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    when (selectedTab) {
                        SearchTab.USERS -> items(userResults, key = { it.uid }) { user ->
                            UserSearchRow(user = user, onClick = {
                                recentStore.addRecentUser(user); reloadRecents(); onOpenUser(user.uid, user.nickname)
                            })
                            HorizontalDivider(color = divColor)
                        }
                        SearchTab.HASHTAGS -> items(hashtagResults, key = { it.first }) { (tag, count) ->
                            HashtagSearchRow(tag = tag, count = count, onClick = {
                                recentStore.addRecentHashtag(tag, count); reloadRecents(); onOpenHashtag(tag)
                            })
                            HorizontalDivider(color = divColor)
                        }
                        SearchTab.GROUPS -> items(groupResults, key = { it.id }) { group ->
                            GroupSearchRow(group = group, onClick = {
                                recentStore.addRecentGroup(group); reloadRecents(); onOpenGroup(group.id)
                            })
                            HorizontalDivider(color = divColor)
                        }
                        SearchTab.EVENTS -> items(eventResults, key = { it.id }) { event ->
                            EventSearchRow(event = event, onClick = {
                                recentStore.addRecentEvent(event); reloadRecents(); onOpenEvent(event.id)
                            })
                            HorizontalDivider(color = divColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyResult(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
        Text(text, color = AppColors.textSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun RecentUserRow(user: User, onClick: () -> Unit, onRemove: () -> Unit) {
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        val pic = user.profilePicture
        if (pic != null) {
            Image(
                painter = rememberAsyncImagePainter(pic),
                contentDescription = null,
                modifier = Modifier.size(38.dp).clip(CircleShape).background(AppColors.avatarPlaceholder),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = RyderAccent) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.nickname.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.nickname, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            val fullName = "${user.firstName} ${user.lastName}".trim()
            if (fullName.isNotEmpty()) Text(fullName, color = textSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Noņemt", tint = textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun RecentHashtagRow(tag: String, count: Int, onClick: () -> Unit, onRemove: () -> Unit) {
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val tagBg = AppColors.tagBackground
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(tagBg).border(1.dp, RyderAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("#", color = RyderAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("#$tag", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("$count ierakst${if (count == 1) "s" else "i"}", color = textSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Noņemt", tint = textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun RecentGroupRow(group: Group, onClick: () -> Unit, onRemove: () -> Unit) {
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = RyderAccent) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("${group.memberIds.size} dalībnieki", color = textSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Noņemt", tint = textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun RecentEventRow(event: Event, onClick: () -> Unit, onRemove: () -> Unit) {
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val tagBg = AppColors.tagBackground
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = tagBg) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Event, contentDescription = null, tint = RyderAccent, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.name, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (event.place.isNotEmpty()) Text(event.place, color = textSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Noņemt", tint = textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun UserSearchRow(user: User, onClick: () -> Unit) {
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pic = user.profilePicture
        if (pic != null) {
            Image(
                painter = rememberAsyncImagePainter(pic),
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(AppColors.avatarPlaceholder),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = RyderAccent) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.nickname.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.nickname, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (user.firstName.isNotEmpty() || user.lastName.isNotEmpty()) {
                Text("${user.firstName} ${user.lastName}".trim(), color = textSecondary, fontSize = 13.sp)
            }
            if (user.bio.isNotEmpty()) {
                Text(user.bio, color = textSecondary, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HashtagSearchRow(tag: String, count: Int, onClick: () -> Unit) {
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val tagBg = AppColors.tagBackground
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(tagBg).border(1.dp, RyderAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("#", color = RyderAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text("#$tag", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("$count ierakst${if (count == 1) "s" else "i"}", color = textSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GroupSearchRow(group: Group, onClick: () -> Unit) {
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = RyderAccent) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Group, contentDescription = null, tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("${group.memberIds.size} dalībnieki", color = textSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EventSearchRow(event: Event, onClick: () -> Unit) {
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val tagBg = AppColors.tagBackground
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = tagBg) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Event, contentDescription = null, tint = RyderAccent)
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.name, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(event.place, color = textSecondary, fontSize = 13.sp)
        }
    }
}
