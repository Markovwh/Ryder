package common.model

import com.google.firebase.firestore.PropertyName

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
    val followRequests: List<String> = emptyList(), // UIDs of users requesting to follow (private accounts)
    // @PropertyName is required: Kotlin compiles `isXxx: Boolean` with getter `isXxx()`,
    // which Java Beans introspection maps to property name "xxx" (strips the "is" prefix).
    // Without this annotation, Firestore reads/writes "admin" and "banned" instead of
    // "isAdmin" and "isBanned", so the fields never deserialize correctly.
    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false,
    @get:PropertyName("isBanned") @set:PropertyName("isBanned")
    var isBanned: Boolean = false,
    val experienceYears: Int = 0,
    val lastNotifViewedAt: Long = 0L
)