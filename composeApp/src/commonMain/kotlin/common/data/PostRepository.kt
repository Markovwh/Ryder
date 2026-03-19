package common.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import common.model.Post
import kotlinx.coroutines.tasks.await

class PostRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val postsRef = firestore.collection("posts")

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

    suspend fun createPost(post: Post): Post {
        val doc = postsRef.document()
        val newPost = post.copy(
            id = doc.id,
            createdAt = System.currentTimeMillis()
        )
        doc.set(newPost).await()
        return newPost
    }

    suspend fun getPostsByUser(userId: String): List<Post> {
        val snapshot = postsRef
            .whereEqualTo("userId", userId)
            .orderBy("createdAt")
            .get()
            .await()
        return snapshot.toObjects(Post::class.java).reversed()
    }

    fun listenToPosts(onUpdate: (List<Post>) -> Unit) {
        postsRef
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Firestore error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull {
                        try { it.toObject(Post::class.java) }
                        catch (e: Exception) { null }
                    }
                    onUpdate(posts.reversed())
                }
            }
    }
}