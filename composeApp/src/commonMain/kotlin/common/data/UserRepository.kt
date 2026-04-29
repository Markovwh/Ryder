package common.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import common.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersRef = firestore.collection("users")
    private val blocksRef = firestore.collection("blocks")
    private val followRequestsRef = firestore.collection("followRequests")

    private fun blockDocId(blockerId: String, blockedId: String) = "${blockerId}_${blockedId}"
    private fun requestDocId(requesterId: String, targetId: String) = "${requesterId}_${targetId}"

    // ── Follow ────────────────────────────────────────────────────────────────

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        val doc = usersRef.document(currentUserId).get().await()
        @Suppress("UNCHECKED_CAST")
        val following = doc.get("following") as? List<String>
        return following?.contains(targetUserId) == true
    }

    suspend fun follow(currentUserId: String, targetUserId: String) {
        usersRef.document(currentUserId).update(
            "following", FieldValue.arrayUnion(targetUserId),
            "followingCount", FieldValue.increment(1)
        ).await()
        try {
            usersRef.document(targetUserId).update("followerCount", FieldValue.increment(1)).await()
        } catch (_: Exception) {}
    }

    suspend fun unfollow(currentUserId: String, targetUserId: String) {
        usersRef.document(currentUserId).update(
            "following", FieldValue.arrayRemove(targetUserId),
            "followingCount", FieldValue.increment(-1)
        ).await()
        try {
            usersRef.document(targetUserId).update("followerCount", FieldValue.increment(-1)).await()
        } catch (_: Exception) {}
    }

    // ── Follow requests (for private profiles) ───────────────────────────────
    // Stored in a separate `followRequests` collection so the requester writes
    // only their own document — no cross-user document write required.

    suspend fun hasSentFollowRequest(requesterId: String, targetId: String): Boolean =
        followRequestsRef.document(requestDocId(requesterId, targetId)).get().await().exists()

    suspend fun sendFollowRequest(requesterId: String, targetId: String) {
        followRequestsRef.document(requestDocId(requesterId, targetId)).set(
            mapOf(
                "requesterId" to requesterId,
                "targetId" to targetId,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun cancelFollowRequest(requesterId: String, targetId: String) {
        followRequestsRef.document(requestDocId(requesterId, targetId)).delete().await()
    }

    suspend fun acceptFollowRequest(targetId: String, requesterId: String) {
        // Follow first — if this fails the request doc stays so the user can retry.
        follow(requesterId, targetId)
        // Delete the request only after the follow is confirmed.
        followRequestsRef.document(requestDocId(requesterId, targetId)).delete().await()
    }

    suspend fun declineFollowRequest(targetId: String, requesterId: String) {
        followRequestsRef.document(requestDocId(requesterId, targetId)).delete().await()
    }

    // ── Notification timestamp ────────────────────────────────────────────────

    suspend fun updateLastNotifViewedAt(userId: String, timestamp: Long) {
        try {
            usersRef.document(userId).update("lastNotifViewedAt", timestamp).await()
        } catch (_: Exception) {}
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    suspend fun isBlocked(currentUserId: String, targetUserId: String): Boolean =
        blocksRef.document(blockDocId(currentUserId, targetUserId)).get().await().exists()

    suspend fun block(blockerId: String, blockedId: String) {
        blocksRef.document(blockDocId(blockerId, blockedId)).set(
            mapOf(
                "blockerId" to blockerId,
                "blockedId" to blockedId,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        try { unfollow(blockerId, blockedId) } catch (_: Exception) {}
        try { unfollow(blockedId, blockerId) } catch (_: Exception) {}
    }

    suspend fun unblock(blockerId: String, blockedId: String) {
        blocksRef.document(blockDocId(blockerId, blockedId)).delete().await()
    }

    // ── Followers / Following lists ───────────────────────────────────────────

    suspend fun getFollowerCount(userId: String): Int =
        usersRef.whereArrayContains("following", userId).get().await().size()

    suspend fun getFollowers(userId: String): List<User> {
        val snap = usersRef.whereArrayContains("following", userId).get().await()
        return snap.documents.mapNotNull {
            try { it.toObject(User::class.java) } catch (_: Exception) { null }
        }
    }

    suspend fun getFollowing(userId: String): List<User> {
        val doc = usersRef.document(userId).get().await()
        @Suppress("UNCHECKED_CAST")
        val ids = (doc.get("following") as? List<String>) ?: return emptyList()

        val deadIds = mutableListOf<String>()
        val users = ids.mapNotNull { id ->
            val fetched = try { usersRef.document(id).get().await() } catch (_: Exception) { null }
            if (fetched == null || !fetched.exists()) {
                deadIds.add(id)
                null
            } else {
                try { fetched.toObject(User::class.java) } catch (_: Exception) { null }
            }
        }

        if (deadIds.isNotEmpty()) {
            val ref = usersRef.document(userId)
            deadIds.forEach { deadId ->
                try { ref.update("following", FieldValue.arrayRemove(deadId)).await() } catch (_: Exception) {}
            }
            try {
                ref.update("followingCount", (ids.size - deadIds.size).coerceAtLeast(0)).await()
            } catch (_: Exception) {}
        }

        return users
    }

    suspend fun getFollowerIds(userId: String): Set<String> {
        val snap = usersRef.whereArrayContains("following", userId).get().await()
        return snap.documents.mapNotNull { it.getString("uid") }.toSet()
    }

    suspend fun getFollowRequestUsers(userId: String): List<User> {
        val snap = followRequestsRef.whereEqualTo("targetId", userId).get().await()
        val requesterIds = snap.documents.mapNotNull { it.getString("requesterId") }
        return requesterIds.mapNotNull { id ->
            try { usersRef.document(id).get().await().toObject(User::class.java) } catch (_: Exception) { null }
        }
    }
}
