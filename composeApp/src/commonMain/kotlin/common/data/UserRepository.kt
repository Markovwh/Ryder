package common.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import common.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersRef = firestore.collection("users")
    private val blocksRef = firestore.collection("blocks")

    private fun blockDocId(blockerId: String, blockedId: String) = "${blockerId}_${blockedId}"

    // ── Follow ────────────────────────────────────────────────────────────────
    // Follow relationships are stored as a `following` array on each user's own
    // document. This way a user only ever writes to their own document, which
    // works with standard Firestore auth rules without any extra rule additions.

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        val doc = usersRef.document(currentUserId).get().await()
        @Suppress("UNCHECKED_CAST")
        val following = doc.get("following") as? List<String>
        return following?.contains(targetUserId) == true
    }

    suspend fun follow(currentUserId: String, targetUserId: String) {
        // Write only to the current user's own document
        usersRef.document(currentUserId).update(
            "following", FieldValue.arrayUnion(targetUserId),
            "followingCount", FieldValue.increment(1)
        ).await()
        // Best-effort: update target's follower count (may fail if rules restrict cross-user writes)
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

    suspend fun hasSentFollowRequest(requesterId: String, targetId: String): Boolean {
        val doc = usersRef.document(targetId).get().await()
        @Suppress("UNCHECKED_CAST")
        val requests = doc.get("followRequests") as? List<String>
        return requests?.contains(requesterId) == true
    }

    suspend fun sendFollowRequest(requesterId: String, targetId: String) {
        usersRef.document(targetId).update(
            "followRequests", FieldValue.arrayUnion(requesterId)
        ).await()
    }

    suspend fun cancelFollowRequest(requesterId: String, targetId: String) {
        usersRef.document(targetId).update(
            "followRequests", FieldValue.arrayRemove(requesterId)
        ).await()
    }

    suspend fun acceptFollowRequest(targetId: String, requesterId: String) {
        // Perform the actual follow
        follow(requesterId, targetId)
        // Remove from pending requests
        usersRef.document(targetId).update(
            "followRequests", FieldValue.arrayRemove(requesterId)
        ).await()
    }

    suspend fun declineFollowRequest(targetId: String, requesterId: String) {
        usersRef.document(targetId).update(
            "followRequests", FieldValue.arrayRemove(requesterId)
        ).await()
    }

    suspend fun getFollowRequestUsers(userId: String): List<User> {
        val doc = usersRef.document(userId).get().await()
        @Suppress("UNCHECKED_CAST")
        val ids = (doc.get("followRequests") as? List<String>) ?: return emptyList()
        return ids.mapNotNull { id ->
            try { usersRef.document(id).get().await().toObject(User::class.java) } catch (_: Exception) { null }
        }
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
        // Query all users whose `following` array contains this userId
        val snap = usersRef.whereArrayContains("following", userId).get().await()
        return snap.documents.mapNotNull {
            try { it.toObject(User::class.java) } catch (_: Exception) { null }
        }
    }

    suspend fun getFollowing(userId: String): List<User> {
        val doc = usersRef.document(userId).get().await()
        @Suppress("UNCHECKED_CAST")
        val ids = (doc.get("following") as? List<String>) ?: return emptyList()
        return ids.mapNotNull { id ->
            try { usersRef.document(id).get().await().toObject(User::class.java) } catch (_: Exception) { null }
        }
    }

    /** Returns the UIDs of every user who follows [userId]. */
    suspend fun getFollowerIds(userId: String): Set<String> {
        val snap = usersRef.whereArrayContains("following", userId).get().await()
        return snap.documents.mapNotNull { it.getString("uid") }.toSet()
    }
}
