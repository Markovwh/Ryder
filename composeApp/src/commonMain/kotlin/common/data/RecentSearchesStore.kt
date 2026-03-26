package common.data

import android.content.Context
import common.model.Event
import common.model.Group
import common.model.User

/**
 * Stores recent searches per category in SharedPreferences.
 * Fields separated by \u001F, records separated by \u001E.
 */
class RecentSearchesStore(context: Context) {

    private val prefs = context.getSharedPreferences("recent_searches", Context.MODE_PRIVATE)
    private val max = 7

    // ── Users ─────────────────────────────────────────────────────────────────

    fun getRecentUsers(): List<User> {
        val raw = prefs.getString("users", "") ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split('\u001E').mapNotNull { record ->
            val p = record.split('\u001F')
            if (p.size < 2) null
            else User(
                uid = p[0],
                nickname = p[1],
                profilePicture = p.getOrNull(2)?.takeIf { it.isNotEmpty() },
                firstName = p.getOrNull(3) ?: "",
                lastName = p.getOrNull(4) ?: ""
            )
        }
    }

    fun addRecentUser(user: User) {
        val updated = (listOf(user) + getRecentUsers().filter { it.uid != user.uid }).take(max)
        prefs.edit().putString("users", updated.joinToString("\u001E") {
            "${it.uid}\u001F${it.nickname}\u001F${it.profilePicture ?: ""}\u001F${it.firstName}\u001F${it.lastName}"
        }).apply()
    }

    fun removeRecentUser(uid: String) {
        val updated = getRecentUsers().filter { it.uid != uid }
        prefs.edit().putString("users", updated.joinToString("\u001E") {
            "${it.uid}\u001F${it.nickname}\u001F${it.profilePicture ?: ""}\u001F${it.firstName}\u001F${it.lastName}"
        }).apply()
    }

    // ── Hashtags ──────────────────────────────────────────────────────────────

    fun getRecentHashtags(): List<Pair<String, Int>> {
        val raw = prefs.getString("hashtags", "") ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split('\u001E').mapNotNull { record ->
            val p = record.split('\u001F')
            if (p.isEmpty()) null
            else Pair(p[0], p.getOrNull(1)?.toIntOrNull() ?: 0)
        }
    }

    fun addRecentHashtag(tag: String, count: Int) {
        val updated = (listOf(Pair(tag, count)) + getRecentHashtags().filter { it.first != tag }).take(max)
        prefs.edit().putString("hashtags", updated.joinToString("\u001E") {
            "${it.first}\u001F${it.second}"
        }).apply()
    }

    fun removeRecentHashtag(tag: String) {
        val updated = getRecentHashtags().filter { it.first != tag }
        prefs.edit().putString("hashtags", updated.joinToString("\u001E") {
            "${it.first}\u001F${it.second}"
        }).apply()
    }

    // ── Groups ────────────────────────────────────────────────────────────────

    fun getRecentGroups(): List<Group> {
        val raw = prefs.getString("groups", "") ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split('\u001E').mapNotNull { record ->
            val p = record.split('\u001F')
            if (p.size < 2) null
            else Group(
                id = p[0],
                name = p[1],
                memberIds = List(p.getOrNull(2)?.toIntOrNull() ?: 0) { "" }
            )
        }
    }

    fun addRecentGroup(group: Group) {
        val updated = (listOf(group) + getRecentGroups().filter { it.id != group.id }).take(max)
        prefs.edit().putString("groups", updated.joinToString("\u001E") {
            "${it.id}\u001F${it.name}\u001F${it.memberIds.size}"
        }).apply()
    }

    fun removeRecentGroup(id: String) {
        val updated = getRecentGroups().filter { it.id != id }
        prefs.edit().putString("groups", updated.joinToString("\u001E") {
            "${it.id}\u001F${it.name}\u001F${it.memberIds.size}"
        }).apply()
    }

    // ── Events ────────────────────────────────────────────────────────────────

    fun getRecentEvents(): List<Event> {
        val raw = prefs.getString("events", "") ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split('\u001E').mapNotNull { record ->
            val p = record.split('\u001F')
            if (p.size < 2) null
            else Event(
                id = p[0],
                name = p[1],
                place = p.getOrNull(2) ?: ""
            )
        }
    }

    fun addRecentEvent(event: Event) {
        val updated = (listOf(event) + getRecentEvents().filter { it.id != event.id }).take(max)
        prefs.edit().putString("events", updated.joinToString("\u001E") {
            "${it.id}\u001F${it.name}\u001F${it.place}"
        }).apply()
    }

    fun removeRecentEvent(id: String) {
        val updated = getRecentEvents().filter { it.id != id }
        prefs.edit().putString("events", updated.joinToString("\u001E") {
            "${it.id}\u001F${it.name}\u001F${it.place}"
        }).apply()
    }
}
