package common.data

class UserRepository(
    private val authService: AuthService
) {

    suspend fun registerUser(
        email: String,
        password: String,
        nickname: String,
        firstName: String,
        lastName: String
    ): Result<Unit> {
        return authService.register(
            email = email,
            password = password,
            nickname = nickname,
            firstName = firstName,
            lastName = lastName
        )
    }

    suspend fun loginUser(
        email: String,
        password: String
    ): Result<Unit> {
        return authService.login(email, password)
    }
}
