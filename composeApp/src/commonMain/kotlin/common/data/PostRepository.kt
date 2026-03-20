package common.data

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import common.model.Comment
import common.model.Post
import kotlinx.coroutines.tasks.await

class PostRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val postsRef = firestore.collection("posts")
    private val likesRef = firestore.collection("likes")
    private val reportsRef = firestore.collection("reports")

    // ── Media upload ──────────────────────────────────────────────────────────

    suspend fun uploadMedia(uri: Uri, userId: String): String {
        val filename = "${System.currentTimeMillis()}_${uri.lastPathSegment}"
        val ref = storage.reference.child("posts/$userId/$filename")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadProfilePicture(uri: Uri, userId: String): String {
        val ref = storage.reference.child("profiles/$userId/profile_picture")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    // ── Posts ─────────────────────────────────────────────────────────────────

    suspend fun createPost(post: Post): Post {
        val doc = postsRef.document()
        val newPost = post.copy(id = doc.id, createdAt = System.currentTimeMillis())
        doc.set(newPost).await()
        return newPost
    }

    suspend fun getPostsByUser(userId: String): List<Post> {
        val snapshot = postsRef
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.toObjects(Post::class.java).sortedByDescending { it.createdAt }
    }

    fun listenToPosts(onUpdate: (List<Post>) -> Unit) {
        postsRef
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull {
                        try { it.toObject(Post::class.java) } catch (_: Exception) { null }
                    }
                    onUpdate(posts.reversed())
                }
            }
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
        val snap = postsRef.get().await()
        val hashtagRegex = Regex("#(\\w+)")
        val counts = mutableMapOf<String, Int>()
        snap.toObjects(Post::class.java).forEach { post ->
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
        val tag = if (hashtag.startsWith("#")) hashtag.lowercase() else "#${hashtag.lowercase()}"
        val snap = postsRef.get().await()
        return snap.toObjects(Post::class.java)
            .filter { it.description.lowercase().contains(tag) }
            .sortedByDescending { it.createdAt }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    suspend fun deletePost(postId: String) {
        postsRef.document(postId).delete().await()
        // Delete associated likes
        val likes = likesRef.whereEqualTo("postId", postId).get().await()
        likes.documents.forEach { it.reference.delete().await() }
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    suspend fun reportPost(postId: String, reporterId: String, reason: String) {
        reportsRef.add(
            mapOf(
                "postId" to postId,
                "reporterId" to reporterId,
                "reason" to reason,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }
}
