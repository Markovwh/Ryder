package common.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import common.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersRef = firestore.collection("users")
    private val followsRef = firestore.collection("follows")
    private val blocksRef = firestore.collection("blocks")

    private fun followDocId(followerId: String, followedId: String) = "${followerId}_${followedId}"
    private fun blockDocId(blockerId: String, blockedId: String) = "${blockerId}_${blockedId}"

    // ── Follow ────────────────────────────────────────────────────────────────

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean =
        followsRef.document(followDocId(currentUserId, targetUserId)).get().await().exists()

    suspend fun follow(currentUserId: String, targetUserId: String) {
        followsRef.document(followDocId(currentUserId, targetUserId)).set(
            mapOf(
                "followerId" to currentUserId,
                "followedId" to targetUserId,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        usersRef.document(targetUserId).update("followerCount", FieldValue.increment(1)).await()
        usersRef.document(currentUserId).update("followingCount", FieldValue.increment(1)).await()
    }

    suspend fun unfollow(currentUserId: String, targetUserId: String) {
        followsRef.document(followDocId(currentUserId, targetUserId)).delete().await()
        usersRef.document(targetUserId).update("followerCount", FieldValue.increment(-1)).await()
        usersRef.document(currentUserId).update("followingCount", FieldValue.increment(-1)).await()
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

    suspend fun getFollowers(userId: String): List<User> {
        val snap = followsRef.whereEqualTo("followedId", userId).get().await()
        return snap.documents.mapNotNull { it.getString("followerId") }.mapNotNull { id ->
            try { usersRef.document(id).get().await().toObject(User::class.java) } catch (_: Exception) { null }
        }
    }

    suspend fun getFollowing(userId: String): List<User> {
        val snap = followsRef.whereEqualTo("followerId", userId).get().await()
        return snap.documents.mapNotNull { it.getString("followedId") }.mapNotNull { id ->
            try { usersRef.document(id).get().await().toObject(User::class.java) } catch (_: Exception) { null }
        }
    }
}
