package common.model

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNicknames: Map<String, String> = emptyMap(),
    val participantPictures: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val lastUpdated: Long = 0L
)
