package common.data

import com.google.firebase.firestore.FirebaseFirestore
import common.model.Event
import common.model.Group
import common.model.Post
import common.model.Report
import common.model.User
import kotlinx.coroutines.tasks.await

class AdminRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersRef = firestore.collection("users")
    private val postsRef = firestore.collection("posts")
    private val likesRef = firestore.collection("likes")
    private val groupsRef = firestore.collection("groups")
    private val eventsRef = firestore.collection("events")
    private val reportsRef = firestore.collection("reports")

    // Users

    suspend fun getAllUsers(): List<User> =
        usersRef.get().await()
            .toObjects(User::class.java)
            .sortedBy { it.nickname }

    suspend fun updateUser(user: User) {
        usersRef.document(user.uid).set(user).await()
    }

    suspend fun deleteUser(userId: String) {
        usersRef.document(userId).delete().await()
    }

    suspend fun setUserAdmin(userId: String, isAdmin: Boolean) {
        usersRef.document(userId).update("isAdmin", isAdmin).await()
    }

    suspend fun banUser(userId: String) {
        usersRef.document(userId).update("isBanned", true).await()
    }

    suspend fun unbanUser(userId: String) {
        usersRef.document(userId).update("isBanned", false).await()
    }

    // Posts

    suspend fun getAllPosts(): List<Post> {
        val snap = postsRef.get().await()
        return snap.documents
            .mapNotNull { doc -> try { doc.toObject(Post::class.java) } catch (_: Exception) { null } }
            .sortedByDescending { it.createdAt }
    }

    suspend fun deletePost(postId: String) {
        postsRef.document(postId).delete().await()
        val likes = likesRef.whereEqualTo("postId", postId).get().await()
        likes.documents.forEach { it.reference.delete().await() }
    }

    // Groups

    suspend fun getAllGroups(): List<Group> =
        groupsRef.get().await()
            .toObjects(Group::class.java)
            .sortedByDescending { it.createdAt }

    suspend fun deleteGroup(groupId: String) {
        val posts = postsRef.whereEqualTo("groupId", groupId).get().await()
        posts.documents.forEach { it.reference.delete().await() }
        groupsRef.document(groupId).delete().await()
    }

    // Events

    suspend fun getAllEvents(): List<Event> =
        eventsRef.get().await()
            .toObjects(Event::class.java)
            .sortedByDescending { it.createdAt }

    suspend fun deleteEvent(eventId: String) {
        eventsRef.document(eventId).delete().await()
    }

    // Reports

    // Returns all reports - pending first, then others
    suspend fun getAllReports(): List<Report> {
        val snap = reportsRef.get().await()
        return snap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            Report(
                id = doc.id,
                // Legacy reports stored postId; new reports store targetId + targetType
                targetId = (d["targetId"] as? String)?.takeIf { it.isNotEmpty() }
                    ?: (d["postId"] as? String) ?: "",
                targetType = (d["targetType"] as? String) ?: "post",
                targetOwnerNickname = (d["targetOwnerNickname"] as? String) ?: "",
                reporterId = (d["reporterId"] as? String) ?: "",
                reporterNickname = (d["reporterNickname"] as? String) ?: "",
                reason = (d["reason"] as? String) ?: "",
                description = (d["description"] as? String) ?: "",
                createdAt = (d["createdAt"] as? Long) ?: 0L,
                status = (d["status"] as? String) ?: "pending"
            )
        }.sortedWith(compareBy({ it.status != "pending" }, { -it.createdAt }))
    }

    suspend fun resolveReport(reportId: String) {
        reportsRef.document(reportId).update("status", "resolved").await()
    }

    suspend fun dismissReport(reportId: String) {
        reportsRef.document(reportId).update("status", "dismissed").await()
    }

    suspend fun submitReport(
        targetId: String,
        targetType: String,
        targetOwnerNickname: String,
        reporterId: String,
        reporterNickname: String,
        reason: String,
        description: String = ""
    ) {
        reportsRef.add(mapOf(
            "targetId" to targetId,
            "targetType" to targetType,
            "targetOwnerNickname" to targetOwnerNickname,
            "reporterId" to reporterId,
            "reporterNickname" to reporterNickname,
            "reason" to reason,
            "description" to description,
            "createdAt" to System.currentTimeMillis(),
            "status" to "pending"
        )).await()
    }
}
