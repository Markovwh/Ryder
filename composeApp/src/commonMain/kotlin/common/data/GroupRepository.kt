package common.data

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import common.model.Group
import common.model.Post
import common.model.User
import kotlinx.coroutines.tasks.await

class GroupRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val groupsRef = firestore.collection("groups")
    private val postsRef = firestore.collection("posts")

    // ── Group CRUD ────────────────────────────────────────────────────────────

    suspend fun createGroup(group: Group): Group {
        val doc = groupsRef.document()
        val newGroup = group.copy(id = doc.id, createdAt = System.currentTimeMillis())
        doc.set(newGroup).await()
        return newGroup
    }

    suspend fun getGroup(groupId: String): Group? =
        groupsRef.document(groupId).get().await().toObject(Group::class.java)

    suspend fun updateGroup(group: Group) {
        groupsRef.document(group.id).set(group).await()
    }

    suspend fun deleteGroup(groupId: String) {
        val posts = postsRef.whereEqualTo("groupId", groupId).get().await()
        posts.documents.forEach { it.reference.delete().await() }
        groupsRef.document(groupId).delete().await()
    }

    suspend fun getGroupsForUser(userId: String): List<Group> {
        val snap = groupsRef.whereArrayContains("memberIds", userId).get().await()
        return snap.toObjects(Group::class.java).sortedByDescending { it.createdAt }
    }

    suspend fun searchGroups(query: String): List<Group> {
        if (query.isBlank()) return emptyList()
        val snap = groupsRef.get().await()
        return snap.toObjects(Group::class.java)
            .filter {
                it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
            .take(20)
    }

    suspend fun getAllGroups(): List<Group> =
        groupsRef.get().await().toObjects(Group::class.java).sortedByDescending { it.createdAt }

    // ── Membership ────────────────────────────────────────────────────────────

    suspend fun addMember(groupId: String, userId: String) {
        groupsRef.document(groupId).update("memberIds", FieldValue.arrayUnion(userId)).await()
    }

    suspend fun removeMember(groupId: String, userId: String) {
        groupsRef.document(groupId).update(
            mapOf(
                "memberIds" to FieldValue.arrayRemove(userId),
                "adminIds" to FieldValue.arrayRemove(userId)
            )
        ).await()
    }

    suspend fun banUser(groupId: String, userId: String) {
        groupsRef.document(groupId).update(
            mapOf(
                "memberIds" to FieldValue.arrayRemove(userId),
                "adminIds" to FieldValue.arrayRemove(userId),
                "bannedIds" to FieldValue.arrayUnion(userId)
            )
        ).await()
    }

    suspend fun makeAdmin(groupId: String, userId: String) {
        groupsRef.document(groupId).update("adminIds", FieldValue.arrayUnion(userId)).await()
    }

    suspend fun removeAdmin(groupId: String, userId: String) {
        groupsRef.document(groupId).update("adminIds", FieldValue.arrayRemove(userId)).await()
    }

    suspend fun sendInvite(groupId: String, userId: String) {
        groupsRef.document(groupId).update("inviteIds", FieldValue.arrayUnion(userId)).await()
    }

    suspend fun acceptInvite(groupId: String, userId: String) {
        groupsRef.document(groupId).update(
            mapOf(
                "inviteIds" to FieldValue.arrayRemove(userId),
                "memberIds" to FieldValue.arrayUnion(userId)
            )
        ).await()
    }

    suspend fun declineInvite(groupId: String, userId: String) {
        groupsRef.document(groupId).update("inviteIds", FieldValue.arrayRemove(userId)).await()
    }

    suspend fun getPendingInvitesForUser(userId: String): List<Group> {
        val snap = groupsRef.whereArrayContains("inviteIds", userId).get().await()
        return snap.toObjects(Group::class.java).sortedByDescending { it.createdAt }
    }

    // ── User search ───────────────────────────────────────────────────────────

    suspend fun getUsersByIds(ids: List<String>): List<User> =
        ids.mapNotNull { uid ->
            try {
                firestore.collection("users").document(uid).get().await()
                    .toObject(User::class.java)
            } catch (_: Exception) { null }
        }

    suspend fun searchUsers(query: String, excludeIds: List<String>): List<User> {
        if (query.isBlank()) return emptyList()
        val snap = firestore.collection("users").get().await()
        return snap.toObjects(User::class.java)
            .filter {
                it.uid !in excludeIds &&
                (it.nickname.contains(query, ignoreCase = true) ||
                 it.firstName.contains(query, ignoreCase = true))
            }
            .take(20)
    }

    // ── Group posts ───────────────────────────────────────────────────────────

    suspend fun createGroupPost(groupId: String, post: Post): Post {
        val doc = postsRef.document()
        val newPost = post.copy(id = doc.id, createdAt = System.currentTimeMillis(), groupId = groupId)
        doc.set(newPost).await()
        return newPost
    }

    suspend fun getGroupPosts(groupId: String): List<Post> {
        val snap = postsRef
            .whereEqualTo("groupId", groupId)
            .get().await()
        return snap.documents
            .mapNotNull { doc -> try { doc.toObject(Post::class.java) } catch (_: Exception) { null } }
            .sortedByDescending { it.createdAt }
    }

    suspend fun deleteGroupPost(groupId: String, postId: String) {
        postsRef.document(postId).delete().await()
    }

    suspend fun pinGroupPost(groupId: String, postId: String) {
        groupsRef.document(groupId).update("pinnedPostIds", FieldValue.arrayUnion(postId)).await()
    }

    suspend fun unpinGroupPost(groupId: String, postId: String) {
        groupsRef.document(groupId).update("pinnedPostIds", FieldValue.arrayRemove(postId)).await()
    }

    // ── Media upload ──────────────────────────────────────────────────────────

    suspend fun uploadGroupPicture(uri: Uri, groupId: String): String {
        val ref = storage.reference.child("groups/$groupId/picture")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadGroupPostMedia(uri: Uri, groupId: String): String {
        val filename = "${System.currentTimeMillis()}_${uri.lastPathSegment}"
        val ref = storage.reference.child("groups/$groupId/posts/$filename")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
