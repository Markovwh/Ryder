package common.data

import common.model.User

interface UserRemoteDataSource {
    suspend fun createUser(user: User)
}
