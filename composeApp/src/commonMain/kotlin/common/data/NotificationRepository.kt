package common.data

import com.google.firebase.firestore.FirebaseFirestore
import common.model.AppNotification
import kotlinx.coroutines.tasks.await

class NotificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val ref = db.collection("notifications")

    suspend fun send(notification: AppNotification) {
        if (notification.senderId == notification.recipientId) return
        val doc = ref.document()
        doc.set(notification.copy(id = doc.id, createdAt = System.currentTimeMillis())).await()
    }

    fun listen(userId: String, onUpdate: (List<AppNotification>) -> Unit): () -> Unit {
        val reg = ref
            .whereEqualTo("recipientId", userId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                onUpdate(
                    snap.toObjects(AppNotification::class.java)
                        .sortedByDescending { it.createdAt }
                        .take(60)
                )
            }
        return { reg.remove() }
    }

    suspend fun delete(notificationId: String) {
        try { ref.document(notificationId).delete().await() } catch (_: Exception) {}
    }

    suspend fun markAllAsRead(userId: String) {
        try {
            // Single-field query only — avoids requiring a composite Firestore index.
            val snap = ref
                .whereEqualTo("recipientId", userId)
                .get().await()
            val unread = snap.documents.filter { it.getBoolean("isRead") == false }
            if (unread.isEmpty()) return
            val batch = db.batch()
            unread.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
        } catch (_: Exception) {}
    }
}
