package common.model

data class Comment(
    val id: String = "",
    val userId: String = "",
    val nickname: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
