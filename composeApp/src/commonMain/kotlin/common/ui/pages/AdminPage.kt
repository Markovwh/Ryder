@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.data.AdminRepository
import common.model.Event
import common.model.Group
import common.model.Post
import common.model.Report
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.RyderAccent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Semantic accent colors — same in light and dark
private val AdminRed   = Color(0xFFE53935)
private val AdminGreen = Color(0xFF4CAF50)

private enum class AdminTab(val label: String) {
    USERS("Lietotāji"),
    POSTS("Ieraksti"),
    GROUPS("Grupas"),
    EVENTS("Pasākumi"),
    REPORTS("Ziņojumi")
}

@Composable
fun AdminPage(
    currentUser: User?,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit = {},
    onOpenGroup: (String) -> Unit = {},
    onOpenEvent: (String) -> Unit = {}
) {
    val repo  = remember { AdminRepository() }
    val scope = rememberCoroutineScope()

    val bg          = AppColors.background
    val surface     = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSec     = AppColors.textSecondary
    val divider     = AppColors.divider

    var selectedTab by remember { mutableStateOf(AdminTab.USERS) }

    var users   by remember { mutableStateOf<List<User>>(emptyList()) }
    var posts   by remember { mutableStateOf<List<Post>>(emptyList()) }
    var groups  by remember { mutableStateOf<List<Group>>(emptyList()) }
    var events  by remember { mutableStateOf<List<Event>>(emptyList()) }
    var reports by remember { mutableStateOf<List<Report>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    var editingUser          by remember { mutableStateOf<User?>(null) }
    var deleteUserTarget     by remember { mutableStateOf<User?>(null) }
    var banUserTarget        by remember { mutableStateOf<User?>(null) }
    var deletePostTarget     by remember { mutableStateOf<Post?>(null) }
    var deleteGroupTarget    by remember { mutableStateOf<Group?>(null) }
    var deleteEventTarget    by remember { mutableStateOf<Event?>(null) }
    var resolveReportTarget  by remember { mutableStateOf<Report?>(null) }
    var dismissReportTarget  by remember { mutableStateOf<Report?>(null) }
    var deleteFromReport     by remember { mutableStateOf<Report?>(null) }

    // Load all the counts of users, posts, groups, events and reports so stats are always accurate across the admin panel
    fun reloadAll() {
        scope.launch {
            isLoading = true
            errorMsg  = null
            try {
                coroutineScope {
                    launch { try { users   = repo.getAllUsers()   } catch (e: Exception) { errorMsg = e.message } }
                    launch { try { posts   = repo.getAllPosts()   } catch (e: Exception) { errorMsg = e.message } }
                    launch { try { groups  = repo.getAllGroups()  } catch (e: Exception) { errorMsg = e.message } }
                    launch { try { events  = repo.getAllEvents()  } catch (e: Exception) { errorMsg = e.message } }
                    launch { try { reports = repo.getAllReports() } catch (e: Exception) { errorMsg = e.message } }
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reloadAll() }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(errorMsg) {
        errorMsg?.let { snackbar.showSnackbar(it, duration = SnackbarDuration.Long) }
    }

    Scaffold(
        containerColor = bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Surface(color = surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Atpakaļ", tint = textPrimary)
                    }
                    Icon(Icons.Default.AdminPanelSettings, null, tint = RyderAccent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Administratora panelis", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = ::reloadAll) {
                        Icon(Icons.Default.Refresh, "Atjaunot", tint = textSec)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Stats bar — always reflects live loaded data for all tabs
            val pendingReports = reports.count { it.status == "pending" }
            Surface(color = surface, tonalElevation = 1.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AdminStat("Lietotāji", users.size.toString(),                    textPrimary,   textSec)
                    AdminStat("Bloķēti",   users.count { it.isBanned }.toString(),   AdminRed,      textSec)
                    AdminStat("Ieraksti",  posts.size.toString(),                    textPrimary,   textSec)
                    AdminStat("Grupas",    groups.size.toString(),                   textPrimary,   textSec)
                    AdminStat("Pasākumi",  events.size.toString(),                   textPrimary,   textSec)
                    AdminStat("Ziņojumi",  pendingReports.toString(),                textPrimary,   textSec)
                }
            }
            HorizontalDivider(color = divider)

            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor   = surface,
                contentColor     = RyderAccent,
                edgePadding      = 0.dp,
                divider          = { HorizontalDivider(color = divider) }
            ) {
                AdminTab.entries.forEach { tab ->
                    val badge = if (tab == AdminTab.REPORTS && pendingReports > 0) " ($pendingReports)" else ""
                    Tab(
                        selected = selectedTab == tab,
                        onClick  = { selectedTab = tab },
                        text = {
                            Text(
                                tab.label + badge,
                                color    = if (selectedTab == tab) RyderAccent else textSec,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RyderAccent)
                }
                return@Scaffold
            }

            when (selectedTab) {

                // Users admin tab
                AdminTab.USERS -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    val banned = users.filter { it.isBanned }
                    val active = users.filter { !it.isBanned }
                    if (banned.isNotEmpty()) {
                        item { AdminSectionHeader("Bloķēti (${banned.size})", AdminRed, bg) }
                        items(banned, key = { "b_${it.uid}" }) { u ->
                            AdminUserRow(
                                user = u, isCurrentUser = u.uid == currentUser?.uid,
                                surface = surface, textPrimary = textPrimary, textSec = textSec, divider = divider,
                                onView        = { onOpenUser(u.uid) },
                                onEdit        = { editingUser = u },
                                onDelete      = { deleteUserTarget = u },
                                onToggleAdmin = {
                                    scope.launch {
                                        try {
                                            repo.setUserAdmin(u.uid, !u.isAdmin)
                                            users = users.map { if (it.uid == u.uid) it.copy(isAdmin = !it.isAdmin) else it }
                                        } catch (e: Exception) { errorMsg = e.message }
                                    }
                                },
                                onToggleBan = { banUserTarget = u }
                            )
                            HorizontalDivider(color = divider)
                        }
                        item { AdminSectionHeader("Aktīvi (${active.size})", textSec, bg) }
                    }
                    items(active, key = { it.uid }) { u ->
                        AdminUserRow(
                            user = u, isCurrentUser = u.uid == currentUser?.uid,
                            surface = surface, textPrimary = textPrimary, textSec = textSec, divider = divider,
                            onView        = { onOpenUser(u.uid) },
                            onEdit        = { editingUser = u },
                            onDelete      = { deleteUserTarget = u },
                            onToggleAdmin = {
                                scope.launch {
                                    try {
                                        repo.setUserAdmin(u.uid, !u.isAdmin)
                                        users = users.map { if (it.uid == u.uid) it.copy(isAdmin = !it.isAdmin) else it }
                                    } catch (e: Exception) { errorMsg = e.message }
                                }
                            },
                            onToggleBan = { banUserTarget = u }
                        )
                        HorizontalDivider(color = divider)
                    }
                }

                // Posts admin tab
                AdminTab.POSTS -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    if (posts.isEmpty()) {
                        item { AdminEmptyState("Nav ierakstu", textSec) }
                    } else {
                        items(posts, key = { it.id }) { post ->
                            AdminPostRow(
                                post = post,
                                surface = surface, textPrimary = textPrimary, textSec = textSec,
                                onViewUser = { onOpenUser(post.userId) },
                                onDelete   = { deletePostTarget = post }
                            )
                            HorizontalDivider(color = divider)
                        }
                    }
                }

                // Groups admin tab
                AdminTab.GROUPS -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    if (groups.isEmpty()) {
                        item { AdminEmptyState("Nav grupu", textSec) }
                    } else {
                        items(groups, key = { it.id }) { group ->
                            AdminGroupRow(
                                group = group,
                                surface = surface, textPrimary = textPrimary, textSec = textSec,
                                onView   = { onOpenGroup(group.id) },
                                onDelete = { deleteGroupTarget = group }
                            )
                            HorizontalDivider(color = divider)
                        }
                    }
                }

                // Events admin tab
                AdminTab.EVENTS -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    if (events.isEmpty()) {
                        item { AdminEmptyState("Nav pasākumu", textSec) }
                    } else {
                        items(events, key = { it.id }) { event ->
                            AdminEventRow(
                                event = event,
                                surface = surface, textPrimary = textPrimary, textSec = textSec,
                                onView   = { onOpenEvent(event.id) },
                                onDelete = { deleteEventTarget = event }
                            )
                            HorizontalDivider(color = divider)
                        }
                    }
                }

                // Reports admin tab
                AdminTab.REPORTS -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    val pending  = reports.filter { it.status == "pending" }
                    val resolved = reports.filter { it.status != "pending" }
                    if (reports.isEmpty()) {
                        item { AdminEmptyState("Nav ziņojumu", textSec) }
                    } else {
                        if (pending.isNotEmpty()) {
                            item { AdminSectionHeader("Gaida izskatīšanu (${pending.size})", textSec, bg) }
                            items(pending, key = { "p_${it.id}" }) { report ->
                                AdminReportRow(
                                    report = report,
                                    users = users, posts = posts, groups = groups, events = events,
                                    surface = surface, textPrimary = textPrimary, textSec = textSec,
                                    onNavigate = {
                                        when (report.targetType) {
                                            "user"  -> onOpenUser(report.targetId)
                                            "group" -> onOpenGroup(report.targetId)
                                            "event" -> onOpenEvent(report.targetId)
                                            else    -> {} // posts have no standalone page
                                        }
                                    },
                                    onDeleteContent = { deleteFromReport = report },
                                    onResolve       = { resolveReportTarget = report },
                                    onDismiss       = { dismissReportTarget = report }
                                )
                                HorizontalDivider(color = divider)
                            }
                        }
                        if (resolved.isNotEmpty()) {
                            item { AdminSectionHeader("Izskatīti (${resolved.size})", AdminGreen, bg) }
                            items(resolved, key = { "r_${it.id}" }) { report ->
                                AdminReportRow(
                                    report = report,
                                    users = users, posts = posts, groups = groups, events = events,
                                    surface = surface, textPrimary = textPrimary, textSec = textSec,
                                    onNavigate      = null,
                                    onDeleteContent = null,
                                    onResolve       = null,
                                    onDismiss       = null
                                )
                                HorizontalDivider(color = divider)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit user dialog
    editingUser?.let { u ->
        AdminEditUserDialog(
            user = u,
            surface = AppColors.surface, textPrimary = AppColors.textPrimary, textSec = AppColors.textSecondary,
            onDismiss = { editingUser = null },
            onSave    = { updated ->
                editingUser = null
                scope.launch {
                    try {
                        repo.updateUser(updated)
                        users = users.map { if (it.uid == updated.uid) updated else it }
                    } catch (e: Exception) { errorMsg = e.message }
                }
            }
        )
    }

    // Confirm dialogs
    deleteUserTarget?.let { u ->
        AdminConfirmDialog(
            title = "Dzēst lietotāju?", text = "Tiks neatgriezeniski dzēsts: ${u.nickname}",
            confirmLabel = "Dzēst", confirmColor = AdminRed,
            surface = AppColors.surface, textPrimary = AppColors.textPrimary, textSec = AppColors.textSecondary,
            onConfirm = {
                deleteUserTarget = null
                scope.launch {
                    try { repo.deleteUser(u.uid); users = users.filter { it.uid != u.uid } }
                    catch (e: Exception) { errorMsg = e.message }
                }
            },
            onDismiss = { deleteUserTarget = null }
        )
    }

    banUserTarget?.let { u ->
        AdminConfirmDialog(
            title = if (u.isBanned) "Atcelt bloķēšanu?" else "Bloķēt lietotāju?",
            text  = if (u.isBanned) "${u.nickname} varēs atkal pieslēgties."
                    else            "${u.nickname} tiks bloķēts un nevarēs pieslēgties.",
            confirmLabel = if (u.isBanned) "Atcelt bloķēšanu" else "Bloķēt",
            confirmColor = if (u.isBanned) AdminGreen else AdminRed,
            surface = AppColors.surface, textPrimary = AppColors.textPrimary, textSec = AppColors.textSecondary,
            onConfirm = {
                banUserTarget = null
                scope.launch {
                    try {
                        if (u.isBanned) {
                            repo.unbanUser(u.uid)
                            users = users.map { if (it.uid == u.uid) it.copy(isBanned = false) else it }
                        } else {
                            repo.banUser(u.uid)
                            users = users.map { if (it.uid == u.uid) it.copy(isBanned = true) else it }
                        }
                    } catch (e: Exception) { errorMsg = e.message }
                }
            },
            onDismiss = { banUserTarget = null }
        )
    }

    deletePostTarget?.let { post ->
        AdminConfirmDialog(
            title = "Dzēst ierakstu?", text = "Ieraksts tiks neatgriezeniski dzēsts.",
            confirmLabel = "Dzēst", confirmColor = AdminRed,
            surface = AppColors.surface, textPrimary = AppColors.textPrimary, textSec = AppColors.textSecondary,
            onConfirm = {
                deletePostTarget = null
                scope.launch {
                    try { repo.deletePost(post.id); posts = posts.filter { it.id != post.id } }
                    catch (e: Exception) { errorMsg = e.message }
                }
            },
            onDismiss = { deletePostTarget = null }
        )
    }

    deleteGroupTarget?.let { group ->
        AdminConfirmDialog(
            title = "Dzēst grupu?",
            text  = "\"${group.name}\" un visi tās ieraksti tiks neatgriezeniski dzēsti.",
            confirmLabel = "Dzēst", confirmColor = AdminRed,
            surface = AppColors.surface, textPrimary = AppColors.textPrimary, textSec = AppColors.textSecondary,
            onConfirm = {
                deleteGroupTarget = null
                scope.launch {
                    try { repo.deleteGroup(group.id); groups = groups.filter { it.id != group.id } }
                    catch (e: Exception) { errorMsg = e.message }
                }
            },
            onDismiss = { deleteGroupTarget = null }
        )
    }

    deleteEventTarget?.let { event ->
        AdminConfirmDialog(
            title = "Dzēst pasākumu?", text = "\"${event.name}\" tiks neatgriezeniski dzēsts.",
            confirmLabel = "Dzēst", confirmColor = AdminRed,
            surface = AppColors.surface, textPrimary = AppColors.textPrimary, textSec = AppColors.textSecondary,
            onConfirm = {
                deleteEventTarget = null
                scope.launch {
                    try { repo.deleteEvent(event.id); events = events.filter { it.id != event.id } }
                    catch (e: Exception) { errorMsg = e.message }
                }
            },
            onDismiss = { deleteEventTarget = null }
        )
    }

    resolveReportTarget?.let { report ->
        AdminConfirmDialog(
            title = "Atzīmēt kā atrisinātu?", text = "Ziņojums tiks atzīmēts kā atrisināts.",
            confirmLabel = "Atrisināts", confirmColor = AdminGreen,
            surface = AppColors.surface, textPrimary = AppColors.textPrimary, textSec = AppColors.textSecondary,
            onConfirm = {
                resolveReportTarget = null
                scope.launch {
                    try {
                        repo.resolveReport(report.id)
                        reports = reports.map { if (it.id == report.id) it.copy(status = "resolved") else it }
                    } catch (e: Exception) { errorMsg = e.message }
                }
            },
            onDismiss = { resolveReportTarget = null }
        )
    }

    dismissReportTarget?.let { report ->
        AdminConfirmDialog(
            title = "Noraidīt ziņojumu?", text = "Saturs paliks. Ziņojums tiks noraidīts.",
            confirmLabel = "Noraidīt", confirmColor = AppColors.textSecondary,
            surface = AppColors.surface, textPrimary = AppColors.textPrimary, textSec = AppColors.textSecondary,
            onConfirm = {
                dismissReportTarget = null
                scope.launch {
                    try {
                        repo.dismissReport(report.id)
                        reports = reports.map { if (it.id == report.id) it.copy(status = "dismissed") else it }
                    } catch (e: Exception) { errorMsg = e.message }
                }
            },
            onDismiss = { dismissReportTarget = null }
        )
    }

    deleteFromReport?.let { report ->
        AdminConfirmDialog(
            title = "Dzēst ziņoto saturu?",
            text  = "Saturs tiks dzēsts un ziņojums atzīmēts kā atrisināts.",
            confirmLabel = "Dzēst saturu", confirmColor = AdminRed,
            surface = AppColors.surface, textPrimary = AppColors.textPrimary, textSec = AppColors.textSecondary,
            onConfirm = {
                deleteFromReport = null
                scope.launch {
                    try {
                        when (report.targetType) {
                            "post"  -> { repo.deletePost(report.targetId);  posts  = posts.filter  { it.id  != report.targetId } }
                            "group" -> { repo.deleteGroup(report.targetId); groups = groups.filter { it.id  != report.targetId } }
                            "event" -> { repo.deleteEvent(report.targetId); events = events.filter { it.id  != report.targetId } }
                            "user"  -> { repo.deleteUser(report.targetId);  users  = users.filter  { it.uid != report.targetId } }
                        }
                        repo.resolveReport(report.id)
                        reports = reports.map { if (it.id == report.id) it.copy(status = "resolved") else it }
                    } catch (e: Exception) { errorMsg = e.message }
                }
            },
            onDismiss = { deleteFromReport = null }
        )
    }
}

// Row composables

@Composable
private fun AdminUserRow(
    user: User, isCurrentUser: Boolean,
    surface: Color, textPrimary: Color, textSec: Color, divider: Color,
    onView: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    onToggleAdmin: () -> Unit, onToggleBan: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val rowBg = if (user.isBanned) AdminRed.copy(alpha = 0.08f) else surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onView)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(Modifier.size(44.dp), shape = CircleShape, color = if (user.isBanned) AdminRed else RyderAccent) {
            Box(contentAlignment = Alignment.Center) {
                Text(user.nickname.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.nickname, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (user.isAdmin)  AdminBadge("ADMIN",   Color(0xFFDCFFB4),              Color(0xFF2E7D32))
                if (user.isBanned) AdminBadge("BLOĶĒTS", AdminRed.copy(alpha = 0.18f),   AdminRed)
            }
            Text(user.email, color = textSec, fontSize = 12.sp)
        }
        if (!isCurrentUser) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Vairāk", tint = textSec)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false },
                    containerColor = AppColors.surface) {
                    DropdownMenuItem(
                        text = { Text("Skatīt profilu", color = AppColors.textPrimary) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = RyderAccent) },
                        onClick = { showMenu = false; onView() })
                    DropdownMenuItem(
                        text = { Text("Rediģēt", color = AppColors.textPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = AppColors.textPrimary) },
                        onClick = { showMenu = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text(if (user.isAdmin) "Noņemt admin" else "Piešķirt admin", color = AppColors.textPrimary) },
                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null, tint = AppColors.textPrimary) },
                        onClick = { showMenu = false; onToggleAdmin() })
                    DropdownMenuItem(
                        text = {
                            Text(if (user.isBanned) "Atcelt bloķēšanu" else "Bloķēt",
                                color = if (user.isBanned) AdminGreen else AdminRed)
                        },
                        leadingIcon = {
                            Icon(if (user.isBanned) Icons.Default.LockOpen else Icons.Default.Block,
                                null, tint = if (user.isBanned) AdminGreen else AdminRed)
                        },
                        onClick = { showMenu = false; onToggleBan() })
                    DropdownMenuItem(
                        text = { Text("Dzēst", color = AdminRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = AdminRed) },
                        onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun AdminPostRow(
    post: Post, surface: Color, textPrimary: Color, textSec: Color,
    onViewUser: () -> Unit, onDelete: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(surface).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(post.user.nickname, color = RyderAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onViewUser))
            if (post.description.isNotEmpty())
                Text(post.description, color = textPrimary, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (post.mediaUrls.isNotEmpty()) Text("${post.mediaUrls.size} medijs", color = textSec, fontSize = 12.sp)
                Text(formatDate(post.createdAt), color = textSec, fontSize = 12.sp)
            }
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Dzēst", tint = AdminRed) }
    }
}

@Composable
private fun AdminGroupRow(
    group: Group, surface: Color, textPrimary: Color, textSec: Color,
    onView: () -> Unit, onDelete: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(surface).clickable(onClick = onView)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(Modifier.size(44.dp), shape = CircleShape, color = RyderAccent) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Group, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(group.name, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (group.description.isNotEmpty())
                Text(group.description, color = textSec, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${group.memberIds.size} biedri  ·  ${formatDate(group.createdAt)}", color = textSec, fontSize = 12.sp)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Dzēst", tint = AdminRed) }
    }
}

@Composable
private fun AdminEventRow(
    event: Event, surface: Color, textPrimary: Color, textSec: Color,
    onView: () -> Unit, onDelete: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(surface).clickable(onClick = onView)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(Modifier.size(44.dp), shape = CircleShape, color = RyderAccent) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Event, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(event.name, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (event.place.isNotEmpty())
                Text(event.place, color = textSec, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${event.attendeeIds.size} dalībnieki  ·  ${formatDate(event.dateTime)}", color = textSec, fontSize = 12.sp)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Dzēst", tint = AdminRed) }
    }
}

@Composable
private fun AdminReportRow(
    report: Report,
    users: List<User>, posts: List<Post>, groups: List<Group>, events: List<Event>,
    surface: Color, textPrimary: Color, textSec: Color,
    onNavigate: (() -> Unit)?,
    onDeleteContent: (() -> Unit)?,
    onResolve: (() -> Unit)?,
    onDismiss: (() -> Unit)?
) {
    val isPending = report.status == "pending"
    val accentColor = when (report.status) {
        "resolved"  -> AdminGreen
        "dismissed" -> textSec
        else        -> textSec
    }
    val typeLabel = when (report.targetType) {
        "user"  -> "Lietotājs"
        "group" -> "Grupa"
        "event" -> "Pasākums"
        else    -> "Ieraksts"
    }
    val canNavigate = report.targetType != "post" && onNavigate != null
    val ownerLabel = report.targetOwnerNickname.takeIf { it.isNotEmpty() }
        ?: report.targetId.take(20).let { if (it.length == 20) "$it…" else it }

    // Look up the actual reported content
    val reportedPost  = if (report.targetType == "post")  posts.find  { it.id  == report.targetId } else null
    val reportedUser  = if (report.targetType == "user")  users.find  { it.uid == report.targetId } else null
    val reportedGroup = if (report.targetType == "group") groups.find { it.id  == report.targetId } else null
    val reportedEvent = if (report.targetType == "event") events.find { it.id  == report.targetId } else null

    Column(Modifier.fillMaxWidth().background(surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Flag, null, tint = accentColor, modifier = Modifier.size(18.dp).padding(top = 2.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AdminBadge(typeLabel, accentColor.copy(alpha = 0.18f), accentColor)
                    Text(report.reason, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                if (report.reporterNickname.isNotEmpty())
                    Text("Ziņotājs: ${report.reporterNickname}", color = textSec, fontSize = 12.sp)
                Text(
                    text = "Par: $ownerLabel",
                    color = if (canNavigate) RyderAccent else textSec,
                    fontSize = 12.sp,
                    modifier = if (canNavigate) Modifier.clickable { onNavigate?.invoke() } else Modifier
                )
                if (report.description.isNotEmpty())
                    Text("\"${report.description}\"", color = textSec, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(formatDate(report.createdAt), color = textSec, fontSize = 11.sp)
            }
        }

        // Content preview in admin panel
        val previewBg = AppColors.tileBackground
        when {
            reportedPost != null -> {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Ieraksta saturs:", color = textSec, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    val author = reportedPost.user.nickname.takeIf { it.isNotEmpty() } ?: ownerLabel
                    Text("@$author", color = RyderAccent, fontSize = 12.sp)
                    if (reportedPost.description.isNotEmpty())
                        Text(reportedPost.description, color = textPrimary, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    if (reportedPost.mediaUrls.isNotEmpty())
                        Text("${reportedPost.mediaUrls.size} medijs(-i) pievienots", color = textSec, fontSize = 11.sp)
                }
            }
            reportedUser != null -> {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Lietotāja informācija:", color = textSec, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    val fullName = "${reportedUser.firstName} ${reportedUser.lastName}".trim()
                    Text(reportedUser.nickname, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (fullName.isNotEmpty()) Text(fullName, color = textSec, fontSize = 12.sp)
                    Text(reportedUser.email, color = textSec, fontSize = 12.sp)
                    if (reportedUser.bio.isNotEmpty())
                        Text(reportedUser.bio, color = textPrimary, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (reportedUser.isAdmin) AdminBadge("ADMIN", Color(0xFFDCFFB4), Color(0xFF2E7D32))
                }
            }
            reportedGroup != null -> {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Grupas informācija:", color = textSec, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(reportedGroup.name, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (reportedGroup.description.isNotEmpty())
                        Text(reportedGroup.description, color = textSec, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text("${reportedGroup.memberIds.size} biedri", color = textSec, fontSize = 11.sp)
                }
            }
            reportedEvent != null -> {
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("Pasākuma informācija:", color = textSec, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(reportedEvent.name, color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (reportedEvent.place.isNotEmpty())
                        Text("Vieta: ${reportedEvent.place}", color = textSec, fontSize = 12.sp)
                    if (reportedEvent.dateTime > 0)
                        Text("Datums: ${formatDate(reportedEvent.dateTime)}", color = textSec, fontSize = 12.sp)
                    Text("${reportedEvent.attendeeIds.size} dalībnieki", color = textSec, fontSize = 11.sp)
                }
            }
            else -> {
                // Content not found in loaded data — show a note
                Spacer(Modifier.height(4.dp))
                Text("Saturs nav atrodams (iespējams dzēsts)", color = textSec, fontSize = 11.sp)
            }
        }

        if (isPending && (onDeleteContent != null || onResolve != null || onDismiss != null)) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                onDeleteContent?.let {
                    TextButton(onClick = it, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = AdminRed)) {
                        Text("Dzēst saturu", fontSize = 12.sp)
                    }
                }
                onResolve?.let {
                    TextButton(onClick = it, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = AdminGreen)) {
                        Text("Atrisināts", fontSize = 12.sp)
                    }
                }
                onDismiss?.let {
                    TextButton(onClick = it, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = textSec)) {
                        Text("Noraidīt", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// Edit user dialog

@Composable
private fun AdminEditUserDialog(
    user: User, surface: Color, textPrimary: Color, textSec: Color,
    onDismiss: () -> Unit, onSave: (User) -> Unit
) {
    var nickname  by remember { mutableStateOf(user.nickname) }
    var firstName by remember { mutableStateOf(user.firstName) }
    var lastName  by remember { mutableStateOf(user.lastName) }
    var bio       by remember { mutableStateOf(user.bio) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor   = textPrimary, unfocusedTextColor   = textPrimary,
        focusedBorderColor = RyderAccent, unfocusedBorderColor = AppColors.inputBorder,
        cursorColor        = RyderAccent,
        focusedLabelColor  = RyderAccent, unfocusedLabelColor  = textSec
    )
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = surface,
        title = { Text("Rediģēt lietotāju", color = textPrimary) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nickname,  onValueChange = { nickname  = it }, label = { Text("Lietotājvārds") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Vārds") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                OutlinedTextField(value = lastName,  onValueChange = { lastName  = it }, label = { Text("Uzvārds") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                OutlinedTextField(value = bio,       onValueChange = { bio       = it }, label = { Text("Bio") },
                    minLines = 2, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(user.copy(nickname = nickname.trim().ifEmpty { user.nickname },
                    firstName = firstName.trim(), lastName = lastName.trim(), bio = bio.trim()))
            }) { Text("Saglabāt", color = RyderAccent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atcelt", color = textSec) } }
    )
}

// Shared helpers

@Composable
private fun AdminConfirmDialog(
    title: String, text: String, confirmLabel: String, confirmColor: Color,
    surface: Color, textPrimary: Color, textSec: Color,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = surface,
        title = { Text(title, color = textPrimary) },
        text  = { Text(text,  color = textSec) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = confirmColor) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atcelt",     color = textSec) } }
    )
}

@Composable
private fun AdminSectionHeader(text: String, color: Color, bg: Color) {
    Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().background(bg).padding(horizontal = 16.dp, vertical = 6.dp))
}

@Composable
private fun AdminStat(label: String, value: String, valueColor: Color, labelColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(label, color = labelColor, fontSize = 10.sp)
    }
}

@Composable
private fun AdminBadge(text: String, bg: Color, textColor: Color) {
    Text(text, color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(bg, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
}

@Composable
private fun AdminEmptyState(message: String, textSec: Color) {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(message, color = textSec, fontSize = 14.sp)
    }
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return ""
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))
}
