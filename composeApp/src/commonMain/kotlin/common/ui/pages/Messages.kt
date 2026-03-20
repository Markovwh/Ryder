@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import common.data.EventRepository
import common.data.GroupRepository
import common.data.MessageRepository
import common.model.Conversation
import common.model.Event
import common.model.Group
import common.model.User
import common.ui.pages.components.RyderAccent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun MessagesPage(
    currentUser: User?,
    onOpenChat: (Screen.Chat) -> Unit,
    onOpenGroup: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit
) {
    val repo = remember { MessageRepository() }
    val groupRepo = remember { GroupRepository() }
    val eventRepo = remember { EventRepository() }
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }
    var showNewChat by remember { mutableStateOf(false) }

    DisposableEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            val unsub = repo.listenToConversations(uid) { conversations = it }
            onDispose { unsub() }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(selectedTab, currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        when (selectedTab) {
            1 -> groups = try { groupRepo.getGroupsForUser(uid) } catch (_: Exception) { emptyList() }
            2 -> events = try { eventRepo.getEvents() } catch (_: Exception) { emptyList() }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Ziņas",
                color = RyderAccent,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF5F5F5),
                contentColor = RyderAccent,
                divider = {}
            ) {
                listOf("Ziņas", "Grupas", "Notikumi").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = RyderAccent,
                        unselectedContentColor = Color(0xFF757575),
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFD9D9D9))

            when (selectedTab) {
                0 -> {
                    if (conversations.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nav sarunu. Sāc jaunu!", color = Color(0xFF757575), fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(conversations, key = { it.id }) { conv ->
                                val otherId = conv.participants.firstOrNull { it != currentUser?.uid } ?: ""
                                val otherNickname = conv.participantNicknames[otherId] ?: "Lietotājs"
                                val otherPicture = conv.participantPictures[otherId]?.takeIf { it.isNotEmpty() }
                                ConversationRow(
                                    nickname = otherNickname,
                                    picture = otherPicture,
                                    lastMessage = conv.lastMessage,
                                    lastUpdated = conv.lastUpdated,
                                    onClick = {
                                        onOpenChat(
                                            Screen.Chat(
                                                conversationId = conv.id,
                                                otherUserId = otherId,
                                                otherUserNickname = otherNickname,
                                                otherUserPicture = otherPicture
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Nav grupu", color = Color(0xFF757575), fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Izveido vai pievienojies grupai!", color = Color(0xFF9E9E9E), fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(groups, key = { it.id }) { group ->
                                GroupRow(
                                    group = group,
                                    currentUserId = currentUser?.uid,
                                    onClick = { onOpenGroup(group.id) }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    val now = System.currentTimeMillis()
                    val futureEvents = events.filter { it.dateTime >= now }.sortedBy { it.dateTime }
                    val pastEvents = events.filter { it.dateTime < now }.sortedByDescending { it.dateTime }
                    if (events.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Nav notikumu", color = Color(0xFF757575), fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Izveido pirmo notikumu!", color = Color(0xFF9E9E9E), fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            if (futureEvents.isNotEmpty()) {
                                item {
                                    Text(
                                        "Gaidāmie",
                                        color = Color(0xFF757575),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                }
                                items(futureEvents, key = { it.id }) { ev ->
                                    EventRow(
                                        event = ev,
                                        currentUserId = currentUser?.uid,
                                        onClick = { onOpenEvent(ev.id) }
                                    )
                                }
                            }
                            if (pastEvents.isNotEmpty()) {
                                item {
                                    Text(
                                        "Pagājušie",
                                        color = Color(0xFF757575),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                }
                                items(pastEvents, key = { "past_${it.id}" }) { ev ->
                                    EventRow(
                                        event = ev,
                                        currentUserId = currentUser?.uid,
                                        onClick = { onOpenEvent(ev.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tab-aware FAB
        val fabIcon = when (selectedTab) {
            1 -> Icons.Default.GroupAdd
            2 -> Icons.Default.AddCircle
            else -> Icons.Default.Edit
        }
        FloatingActionButton(
            onClick = {
                when (selectedTab) {
                    0 -> showNewChat = true
                    1 -> onCreateGroup()
                    2 -> onCreateEvent()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 96.dp),
            containerColor = RyderAccent
        ) {
            Icon(fabIcon, contentDescription = null, tint = Color.White)
        }
    }

    if (showNewChat) {
        NewChatDialog(
            currentUser = currentUser,
            repo = repo,
            onDismiss = { showNewChat = false },
            onOpenChat = { screen ->
                showNewChat = false
                onOpenChat(screen)
            }
        )
    }
}

// ── Group row ─────────────────────────────────────────────────────────────────

@Composable
private fun GroupRow(
    group: Group,
    currentUserId: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (group.pictureUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(group.pictureUrl),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFD0D0D0)),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = RyderAccent) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        group.name.take(1).uppercase(),
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, color = Color(0xFF1A1A1A), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "${group.memberIds.size} biedri",
                color = Color(0xFF757575),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val role = when {
            currentUserId == group.ownerId -> "Īpašnieks"
            currentUserId != null && currentUserId in group.adminIds -> "Admin"
            else -> null
        }
        role?.let {
            Text(it, color = RyderAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    HorizontalDivider(
        color = Color(0xFFD9D9D9),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 84.dp)
    )
}

// ── Event row ─────────────────────────────────────────────────────────────────

@Composable
private fun EventRow(
    event: Event,
    currentUserId: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color(0xFFEEEEEE)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = RyderAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.name, color = Color(0xFF1A1A1A), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "${formatEventDate(event.dateTime)} · ${event.place}",
                color = Color(0xFF757575),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (currentUserId != null && currentUserId in event.attendeeIds) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RyderAccent.copy(alpha = 0.15f)
            ) {
                Text(
                    "Apmeklēju",
                    color = RyderAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
    HorizontalDivider(
        color = Color(0xFFD9D9D9),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 84.dp)
    )
}

// ── Conversation row ──────────────────────────────────────────────────────────

@Composable
private fun ConversationRow(
    nickname: String,
    picture: String?,
    lastMessage: String,
    lastUpdated: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (picture != null) {
            Image(
                painter = rememberAsyncImagePainter(picture),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD0D0D0)),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = RyderAccent) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = nickname.take(1).uppercase(),
                        color = Color(0xFF1A1A1A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nickname,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = lastMessage.ifEmpty { "..." },
                color = Color(0xFF757575),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = formatConvTime(lastUpdated), color = Color(0xFF757575), fontSize = 11.sp)
    }
    HorizontalDivider(
        color = Color(0xFFD9D9D9),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 84.dp)
    )
}

// ── New chat dialog ───────────────────────────────────────────────────────────

@Composable
private fun NewChatDialog(
    currentUser: User?,
    repo: MessageRepository,
    onDismiss: () -> Unit,
    onOpenChat: (Screen.Chat) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<User>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        results = if (query.length >= 2) {
            try { repo.searchUsers(query, uid) } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            Text(
                text = "Jauna saruna",
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
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
            Spacer(modifier = Modifier.height(8.dp))
            if (results.isEmpty() && query.length >= 2) {
                Text(
                    text = "Nav atrasts neviens lietotājs",
                    color = Color(0xFF757575),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            results.forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cu = currentUser ?: return@clickable
                            val convId = repo.conversationId(cu.uid, user.uid)
                            scope.launch {
                                try { repo.getOrCreateConversation(cu, user) } catch (_: Exception) {}
                            }
                            onOpenChat(
                                Screen.Chat(
                                    conversationId = convId,
                                    otherUserId = user.uid,
                                    otherUserNickname = user.nickname,
                                    otherUserPicture = user.profilePicture
                                )
                            )
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pic = user.profilePicture
                    if (pic != null) {
                        Image(
                            painter = rememberAsyncImagePainter(pic),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD0D0D0)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = RyderAccent) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.nickname.take(1).uppercase(),
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = user.nickname, color = Color(0xFF1A1A1A), fontSize = 15.sp)
                }
                HorizontalDivider(color = Color(0xFFD9D9D9))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Atcelt", color = Color(0xFF757575))
            }
        }
    }
}

// ── Formatters ────────────────────────────────────────────────────────────────

private fun formatConvTime(timeMillis: Long): String {
    if (timeMillis == 0L) return ""
    val diff = System.currentTimeMillis() - timeMillis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "Tikko"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}st"
        else -> "${days}d"
    }
}

private fun formatEventDate(millis: Long): String {
    if (millis == 0L) return "?"
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
}
