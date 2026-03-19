package common.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val user: User = User(),
    val mediaUrls: List<String> = emptyList(),
    val description: String = "",
    val visibility: String = "Publisks",
    val createdAt: Long = 0L,
    val likeCount: Int = 0,
    val commentCount: Int = 0
)
