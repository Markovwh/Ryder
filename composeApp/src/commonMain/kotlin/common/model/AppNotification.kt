package common.model

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderNickname: String = "",
    val senderPicture: String = "",
    val type: String = "",          // "follow" | "follow_request" | "comment" | "message"
    val postId: String = "",
    val commentPreview: String = "",
    val conversationId: String = "",
    val createdAt: Long = 0L,
    val isRead: Boolean = false
)
