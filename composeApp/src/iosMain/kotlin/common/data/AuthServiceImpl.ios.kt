package common.data

actual class AuthServiceImpl : AuthService {
    actual override suspend fun register(
        email: String,
        password: String
    ): String {
        error("iOS not implemented yet")
    }

    actual override suspend fun login(
        email: String,
        password: String
    ): String {
        error("iOS not implemented yet")
    }
}
