package common.data

import com.google.firebase.firestore.FieldValue
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
        // Delete all posts by this user (and their likes)
        val userPosts = postsRef.whereEqualTo("userId", userId).get().await()
        userPosts.documents.forEach { postDoc ->
            val likes = likesRef.whereEqualTo("postId", postDoc.id).get().await()
            likes.documents.forEach { it.reference.delete().await() }
            postDoc.reference.delete().await()
        }
        // Delete all groups created by this user
        val userGroups = groupsRef.whereEqualTo("ownerId", userId).get().await()
        userGroups.documents.forEach { groupDoc ->
            val groupPosts = postsRef.whereEqualTo("groupId", groupDoc.id).get().await()
            groupPosts.documents.forEach { postDoc ->
                val likes = likesRef.whereEqualTo("postId", postDoc.id).get().await()
                likes.documents.forEach { it.reference.delete().await() }
                postDoc.reference.delete().await()
            }
            groupDoc.reference.delete().await()
        }
        // Delete all events created by this user
        val userEvents = eventsRef.whereEqualTo("creatorId", userId).get().await()
        userEvents.documents.forEach { it.reference.delete().await() }

        // Fix follow counts:
        // 1. Users who follow the deleted user — remove from their `following` list and decrement their followingCount
        val followers = usersRef.whereArrayContains("following", userId).get().await()
        followers.documents.forEach { followerDoc ->
            followerDoc.reference.update(
                "following", FieldValue.arrayRemove(userId),
                "followingCount", FieldValue.increment(-1)
            ).await()
        }
        // 2. Users the deleted user was following — decrement their followerCount
        val deletedUserDoc = usersRef.document(userId).get().await()
        @Suppress("UNCHECKED_CAST")
        val followingIds = deletedUserDoc.get("following") as? List<String> ?: emptyList()
        followingIds.forEach { followedId ->
            try {
                usersRef.document(followedId).update("followerCount", FieldValue.increment(-1)).await()
            } catch (_: Exception) {}
        }
        // 3. Remove the deleted user from any pending followRequests on other users' documents
        val pendingRequests = usersRef.whereArrayContains("followRequests", userId).get().await()
        pendingRequests.documents.forEach { doc ->
            doc.reference.update("followRequests", FieldValue.arrayRemove(userId)).await()
        }

        // 4. Remove deleted user from groups they were a member/admin of (but didn't own)
        val groupMemberships = groupsRef.whereArrayContains("memberIds", userId).get().await()
        groupMemberships.documents.forEach { doc ->
            doc.reference.update(
                "memberIds", FieldValue.arrayRemove(userId),
                "adminIds", FieldValue.arrayRemove(userId)
            ).await()
        }

        // 5. Remove deleted user from events they were attending (but didn't create)
        val eventAttendances = eventsRef.whereArrayContains("attendeeIds", userId).get().await()
        eventAttendances.documents.forEach { doc ->
            doc.reference.update("attendeeIds", FieldValue.arrayRemove(userId)).await()
        }

        // Delete the user document itself
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
