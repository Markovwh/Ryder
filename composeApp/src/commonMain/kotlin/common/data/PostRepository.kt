package common.data

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import common.model.Comment
import common.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val postsRef = firestore.collection("posts")
    private val likesRef = firestore.collection("likes")
    private val reportsRef = firestore.collection("reports")

    // ── Media upload ──────────────────────────────────────────────────────────

    suspend fun uploadMedia(uri: Uri, userId: String, context: Context): String {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when {
            mimeType.startsWith("video") -> "mp4"
            mimeType == "image/png" -> "png"
            mimeType == "image/gif" -> "gif"
            mimeType == "image/webp" -> "webp"
            else -> "jpg"
        }
        val filename = "${System.currentTimeMillis()}.$ext"
        val ref = storage.reference.child("posts/$userId/$filename")

        val tempFile = withContext(Dispatchers.IO) {
            val file = java.io.File.createTempFile("upload_", ".$ext", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: run { file.delete(); throw Exception("Nevar atvērt attēlu") }
            file
        }
        return try {
            ref.putFile(android.net.Uri.fromFile(tempFile)).await()
            ref.downloadUrl.await().toString()
        } finally {
            tempFile.delete()
        }
    }

    suspend fun uploadProfilePicture(uri: Uri, userId: String, context: Context): String {
        val ref = storage.reference.child("profiles/$userId/profile_picture")
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Nevar atvērt attēlu")
        stream.use { ref.putStream(it).await() }
        return ref.downloadUrl.await().toString()
    }

    // ── Posts ─────────────────────────────────────────────────────────────────

    suspend fun createPost(post: Post): Post {
        val doc = postsRef.document()
        val newPost = post.copy(id = doc.id, createdAt = System.currentTimeMillis())
        doc.set(newPost).await()
        return newPost
    }

    /**
     * Returns posts by [userId], filtering by visibility based on who is viewing.
     *
     * - [viewerId] == null or == [userId]  → all posts (own profile, no filtering)
     * - visibility "Publisks"              → always visible
     * - visibility "Privāts"              → hidden from everyone except the owner
     * - visibility "Draugi"               → visible only when both users follow each other ([mutualFollow] == true)
     */
    suspend fun getPostsByUser(
        userId: String,
        viewerId: String? = null,
        mutualFollow: Boolean = false
    ): List<Post> {
        val snapshot = postsRef.whereEqualTo("userId", userId).get().await()
        return snapshot.documents
            .mapNotNull { doc -> try { doc.toObject(Post::class.java) } catch (_: Exception) { null } }
            .filter { post ->
                if (post.groupId.isNotEmpty()) return@filter false
                when {
                    viewerId == null || viewerId == userId -> true
                    post.visibility == "Publisks" -> true
                    post.visibility == "Privāts" -> false
                    post.visibility == "Draugi" -> mutualFollow
                    else -> true
                }
            }
            .sortedByDescending { it.createdAt }
    }

    /**
     * Listens to the feed and delivers only posts the viewer is allowed to see:
     *
     * - "Publisks"  → everyone
     * - "Privāts"   → owner only
     * - "Draugi"    → owner + mutual followers
     *   (viewer follows owner AND owner follows viewer)
     *
     * Returns an unsubscribe lambda — call it when the composable leaves composition.
     */
    fun listenToPosts(
        currentUserId: String?,
        followingIds: Set<String>,   // UIDs that the current user follows
        followerIds: Set<String>,    // UIDs that follow the current user
        onUpdate: (List<Post>) -> Unit
    ): () -> Unit {
        val reg = postsRef
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val posts = snapshot.documents
                    .mapNotNull { try { it.toObject(Post::class.java) } catch (_: Exception) { null } }
                    .filter { post ->
                        if (post.groupId.isNotEmpty()) return@filter false
                        when (post.visibility) {
                            "Privāts" -> post.userId == currentUserId
                            "Draugi"  -> post.userId == currentUserId ||
                                (followingIds.contains(post.userId) && followerIds.contains(post.userId))
                            else -> true // "Publisks" or unset
                        }
                    }
                onUpdate(posts.reversed())
            }
        return { reg.remove() }
    }

    // ── Likes ─────────────────────────────────────────────────────────────────

    suspend fun isLiked(postId: String, userId: String): Boolean {
        return likesRef.document("${postId}_$userId").get().await().exists()
    }

    suspend fun toggleLike(postId: String, userId: String, liked: Boolean) {
        val likeDoc = likesRef.document("${postId}_$userId")
        val postDoc = postsRef.document(postId)
        if (liked) {
            likeDoc.set(mapOf("postId" to postId, "userId" to userId)).await()
            postDoc.update("likeCount", FieldValue.increment(1)).await()
        } else {
            likeDoc.delete().await()
            postDoc.update("likeCount", FieldValue.increment(-1)).await()
        }
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    suspend fun getComments(postId: String): List<Comment> {
        return postsRef.document(postId)
            .collection("comments")
            .orderBy("createdAt")
            .get()
            .await()
            .toObjects(Comment::class.java)
    }

    suspend fun addComment(postId: String, comment: Comment): Comment {
        val doc = postsRef.document(postId).collection("comments").document()
        val saved = comment.copy(id = doc.id, createdAt = System.currentTimeMillis())
        doc.set(saved).await()
        try { postsRef.document(postId).update("commentCount", FieldValue.increment(1)).await() }
        catch (_: Exception) {}
        return saved
    }

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun getUserById(userId: String): common.model.User? {
        return try {
            firestore.collection("users").document(userId).get().await()
                .toObject(common.model.User::class.java)
        } catch (_: Exception) { null }
    }

    suspend fun searchUsers(query: String): List<common.model.User> {
        if (query.isBlank()) return emptyList()
        val snap = firestore.collection("users").get().await()
        return snap.toObjects(common.model.User::class.java)
            .filter {
                it.nickname.contains(query, ignoreCase = true) ||
                it.firstName.contains(query, ignoreCase = true) ||
                it.lastName.contains(query, ignoreCase = true)
            }
            .take(20)
    }

    suspend fun searchHashtags(query: String): List<Pair<String, Int>> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = query.trimStart('#').lowercase()
        if (normalizedQuery.isEmpty()) return emptyList()
        val snap = postsRef.get().await()
        val hashtagRegex = Regex("#(\\w+)")
        val counts = mutableMapOf<String, Int>()
        snap.documents.forEach { doc ->
            val post = try { doc.toObject(Post::class.java) } catch (_: Exception) { null } ?: return@forEach
            if (post.groupId.isNotEmpty()) return@forEach
            hashtagRegex.findAll(post.description).forEach { match ->
                val tag = match.groupValues[1].lowercase()
                if (tag.contains(normalizedQuery)) {
                    counts[tag] = (counts[tag] ?: 0) + 1
                }
            }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .map { Pair(it.key, it.value) }
            .take(20)
    }

    suspend fun getPostsByHashtag(hashtag: String): List<Post> {
        val word = hashtag.trimStart('#').lowercase()
        val pattern = Regex("(?i)#${Regex.escape(word)}(?!\\w)")
        val snap = postsRef.get().await()
        return snap.documents
            .mapNotNull { doc -> try { doc.toObject(Post::class.java) } catch (_: Exception) { null } }
            .filter { it.groupId.isEmpty() && pattern.containsMatchIn(it.description) }
            .sortedByDescending { it.createdAt }
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    suspend fun updatePost(postId: String, description: String) {
        postsRef.document(postId).update("description", description).await()
    }

    suspend fun updateUserProfileInPosts(userId: String, profilePicture: String, nickname: String) {
        val snapshot = postsRef.whereEqualTo("userId", userId).get().await()
        snapshot.documents.forEach { doc ->
            doc.reference.update(
                mapOf(
                    "user.profilePicture" to profilePicture,
                    "user.nickname" to nickname
                )
            ).await()
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    suspend fun deletePost(postId: String) {
        postsRef.document(postId).delete().await()
        // Delete associated likes
        val likes = likesRef.whereEqualTo("postId", postId).get().await()
        likes.documents.forEach { it.reference.delete().await() }
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    suspend fun reportPost(
        postId: String,
        reporterId: String,
        reporterNickname: String,
        targetOwnerNickname: String,
        reason: String
    ) {
        reportsRef.add(
            mapOf(
                "targetId" to postId,
                "targetType" to "post",
                "targetOwnerNickname" to targetOwnerNickname,
                "reporterId" to reporterId,
                "reporterNickname" to reporterNickname,
                "reason" to reason,
                "createdAt" to System.currentTimeMillis(),
                "status" to "pending"
            )
        ).await()
    }
}
