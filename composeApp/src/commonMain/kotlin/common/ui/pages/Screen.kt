package common.ui.pages

sealed class Screen {
    object Home : Screen()
    object Search : Screen()
    object Messages : Screen()
    object Profile : Screen()
    object Registration : Screen()
    object Login : Screen()
    object CreatePost : Screen()
    object EditProfile : Screen()
    object CreateGroup : Screen()
    object CreateEvent : Screen()
    data class UserProfile(val userId: String, val userNickname: String) : Screen()
    data class HashtagFeed(val hashtag: String) : Screen()
    data class Chat(
        val conversationId: String,
        val otherUserId: String,
        val otherUserNickname: String,
        val otherUserPicture: String?
    ) : Screen()
    data class GroupDetail(val groupId: String) : Screen()
    data class EventDetail(val eventId: String) : Screen()
}