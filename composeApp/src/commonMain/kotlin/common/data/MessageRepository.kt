package common.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import common.model.Conversation
import common.model.Message
import common.model.User
import kotlinx.coroutines.tasks.await

class MessageRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val conversationsRef = firestore.collection("conversations")

    fun conversationId(uid1: String, uid2: String): String =
        listOf(uid1, uid2).sorted().joinToString("_")

    suspend fun getOrCreateConversation(currentUser: User, otherUser: User): String {
        val id = conversationId(currentUser.uid, otherUser.uid)
        // Always merge participant data — if sendMessage already created the document without
        // a participants array, this ensures the array gets written so the conversation
        // shows up in the whereArrayContains("participants") query.
        conversationsRef.document(id).set(
            mapOf(
                "id" to id,
                "participants" to listOf(currentUser.uid, otherUser.uid),
                "participantNicknames" to mapOf(
                    currentUser.uid to currentUser.nickname,
                    otherUser.uid to otherUser.nickname
                ),
                "participantPictures" to mapOf(
                    currentUser.uid to (currentUser.profilePicture ?: ""),
                    otherUser.uid to (otherUser.profilePicture ?: "")
                )
            ),
            SetOptions.merge()
        ).await()
        return id
    }

    suspend fun sendMessage(conversationId: String, message: Message) {
        val msgRef = conversationsRef.document(conversationId).collection("messages").document()
        val saved = message.copy(id = msgRef.id, createdAt = System.currentTimeMillis())
        msgRef.set(saved).await()

        val preview = when {
            message.hasSharedPost -> "📸 Kopīgots ieraksts"
            message.mediaUrls.isNotEmpty() -> "📷 Foto/video"
            else -> message.text.take(60)
        }
        // Use set+merge so this works even if the conversation document doesn't exist yet
        conversationsRef.document(conversationId).set(
            mapOf(
                "lastMessage" to preview,
                "lastMessageSenderId" to message.senderId,
                "lastUpdated" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    }

    fun listenToMessages(conversationId: String, onUpdate: (List<Message>) -> Unit): () -> Unit {
        val reg = conversationsRef.document(conversationId)
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snap, _ ->
                if (snap != null) onUpdate(snap.toObjects(Message::class.java))
            }
        return { reg.remove() }
    }

    fun listenToConversations(userId: String, onUpdate: (List<Conversation>) -> Unit): () -> Unit {
        val reg = conversationsRef
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    onUpdate(
                        snap.toObjects(Conversation::class.java)
                            .sortedByDescending { it.lastUpdated }
                    )
                }
            }
        return { reg.remove() }
    }

    suspend fun searchUsers(query: String, currentUserId: String): List<User> {
        if (query.isBlank()) return emptyList()
        val snap = firestore.collection("users").get().await()
        return snap.toObjects(User::class.java)
            .filter {
                it.uid != currentUserId &&
                (it.nickname.contains(query, ignoreCase = true) ||
                 it.firstName.contains(query, ignoreCase = true))
            }
            .take(20)
    }

    suspend fun deleteMessage(conversationId: String, messageId: String) {
        conversationsRef.document(conversationId)
            .collection("messages")
            .document(messageId)
            .delete()
            .await()
    }

    suspend fun deleteConversation(conversationId: String) {
        val messages = conversationsRef.document(conversationId).collection("messages").get().await()
        messages.documents.forEach { it.reference.delete().await() }
        conversationsRef.document(conversationId).delete().await()
    }

    suspend fun uploadMessageMedia(uri: Uri, senderId: String): String {
        val filename = "${System.currentTimeMillis()}_${uri.lastPathSegment}"
        val ref = storage.reference.child("messages/$senderId/$filename")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
