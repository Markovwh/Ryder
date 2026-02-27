package common.data

import common.model.User

interface AuthService {

    suspend fun register(
        email: String,
        password: String,
        nickname: String,
        firstName: String,
        lastName: String
    ): Result<Unit>

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    fun getCurrentUserId(): String?

    suspend fun getUserData(uid: String): Result<User>

    suspend fun updateUserData(user: User): Result<Unit>

    fun logout()
}