package common.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import common.model.User

class AuthServiceImplAndroid : AuthService {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun register(
        email: String,
        password: String,
        nickname: String,
        firstName: String,
        lastName: String
    ): Result<Unit> {
        return try {

            val result = auth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val uid = result.user?.uid
                ?: return Result.failure(Exception("User ID not found"))

            val userData = hashMapOf(
                "uid" to uid,
                "email" to email,
                "nickname" to nickname,
                "firstName" to firstName,
                "lastName" to lastName,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(uid)
                .set(userData)
                .await()

            Result.success(Unit)

        } catch (e: FirebaseAuthException) {

            val message = when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already registered"
                "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                else -> e.localizedMessage ?: "Registration failed"
            }

            Result.failure(Exception(message))

        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<Unit> {
        return try {

            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)

        } catch (e: FirebaseAuthException) {

            val message = when (e.errorCode) {
                "ERROR_USER_NOT_FOUND" -> "User not found"
                "ERROR_WRONG_PASSWORD" -> "Incorrect password"
                else -> e.localizedMessage ?: "Login failed"
            }

            Result.failure(Exception(message))

        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun getUserData(uid: String): Result<User> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(uid)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java)
                ?: return Result.failure(Exception("User not found"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserData(user: User): Result<Unit> {
        return try {
            firestore
                .collection("users")
                .document(user.uid)
                .set(user)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

actual fun provideAuthService(): AuthService = AuthServiceImplAndroid()