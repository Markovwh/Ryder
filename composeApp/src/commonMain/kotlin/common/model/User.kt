package common.model

data class User(
    val uid: String = "",
    val email: String = "",
    val nickname: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val createdAt: Long = 0L,
    val profilePicture: String? = null,
    val bio: String = "",
    val bike: String = "",
    val profilePrivacy: String = "Publisks", // Publisks vai Privāts
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val following: List<String> = emptyList(), // UIDs of users this person follows
    val isAdmin: Boolean = false,
    val experienceYears: Int = 0
)