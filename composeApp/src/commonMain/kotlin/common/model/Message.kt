package common.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val mediaUrls: List<String> = emptyList(),
    // Shared post info (all empty = no shared post)
    val sharedPostId: String = "",
    val sharedPostDescription: String = "",
    val sharedPostMediaUrl: String = "",
    val sharedPostUserNickname: String = "",
    val sharedPostUserId: String = "",
    val sharedPostUserPicture: String = "",
    val createdAt: Long = 0L
) {
    val hasSharedPost: Boolean get() = sharedPostId.isNotEmpty()
}
