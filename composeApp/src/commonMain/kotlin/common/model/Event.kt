package common.model

data class Event(
    val id: String = "",
    val name: String = "",
    val place: String = "",
    val dateTime: Long = 0L,
    val description: String = "",
    val creatorId: String = "",
    val creatorNickname: String = "",
    val attendeeIds: List<String> = emptyList(),
    val createdAt: Long = 0L
)
