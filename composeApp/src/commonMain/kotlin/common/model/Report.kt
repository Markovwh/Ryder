package common.model

data class Report(
    val id: String = "",
    val targetId: String = "",          // postId / userId / groupId / eventId
    val targetType: String = "post",    // "post", "user", "group", "event"
    val targetOwnerNickname: String = "",
    val reporterId: String = "",
    val reporterNickname: String = "",
    val reason: String = "",
    val description: String = "",
    val createdAt: Long = 0L,
    val status: String = "pending"      // "pending", "resolved", "dismissed"
)
