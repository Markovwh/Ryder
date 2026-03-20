package common.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val pictureUrl: String? = null,
    val ownerId: String = "",
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val bannedIds: List<String> = emptyList(),
    val pinnedPostIds: List<String> = emptyList(),
    val createdAt: Long = 0L
)
