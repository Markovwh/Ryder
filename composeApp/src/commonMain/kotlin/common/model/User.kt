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
    val profilePrivacy: String = "Publisks", // Publisks / Privāts
    val followerCount: Int = 0,
    val followingCount: Int = 0
)