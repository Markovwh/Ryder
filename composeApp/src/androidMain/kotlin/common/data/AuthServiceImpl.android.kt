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
                ?: return Result.failure(Exception("Lietotāja ID nav atrasts"))

            val userData = hashMapOf(
                "uid" to uid,
                "email" to email,
                "nickname" to nickname,
                "firstName" to firstName,
                "lastName" to lastName,
                "createdAt" to System.currentTimeMillis(),
                "followerCount" to 0,
                "followingCount" to 0,
                "following" to emptyList<String>()
            )

            firestore.collection("users")
                .document(uid)
                .set(userData)
                .await()

            Result.success(Unit)

        } catch (e: FirebaseAuthException) {

            val message = if (e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                // Check whether a Firestore doc exists — if not, the account was deleted by an admin
                val existingDoc = try {
                    firestore.collection("users").whereEqualTo("email", email).get().await()
                } catch (_: Exception) { null }
                if (existingDoc != null && existingDoc.isEmpty) {
                    "Šis e-pasts nav pieejams reģistrācijai."
                } else {
                    "Šis e-pasts jau ir reģistrēts"
                }
            } else {
                when (e.errorCode) {
                    "ERROR_WEAK_PASSWORD" -> "Parole ir pārāk vāja"
                    "ERROR_INVALID_EMAIL" -> "Nederīgs e-pasta formāts"
                    else -> "Reģistrācija neizdevās"
                }
            }

            Result.failure(Exception(message))

        } catch (e: Exception) {
            Result.failure(Exception("Nezināma kļūda"))
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<Unit> {
        return try {

            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Lietotāja ID nav atrasts"))

            // Verify the Firestore user document still exists (guards against deleted accounts)
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (!userDoc.exists()) {
                auth.signOut()
                return Result.failure(Exception("Šis konts ir dzēsts. Lūdzu, reģistrējieties ar jaunu kontu."))
            }

            // Also block banned users from logging in
            val isBanned = userDoc.getBoolean("isBanned") ?: false
            if (isBanned) {
                auth.signOut()
                return Result.failure(Exception("Šis konts ir bloķēts."))
            }

            Result.success(Unit)

        } catch (e: FirebaseAuthException) {

            val message = when (e.errorCode) {
                "ERROR_USER_NOT_FOUND" -> "Lietotājs nav atrasts"
                "ERROR_WRONG_PASSWORD" -> "Nepareiza parole"
                "ERROR_INVALID_EMAIL" -> "Nederīgs e-pasta formāts"
                "ERROR_INVALID_CREDENTIAL" -> "Nepareizs e-pasts vai parole"
                "ERROR_USER_DISABLED" -> "Šis konts ir bloķēts"
                "ERROR_TOO_MANY_REQUESTS" -> "Pārāk daudz mēģinājumu. Mēģiniet vēlāk"
                else -> "Pieslēgšanās neizdevās"
            }

            Result.failure(Exception(message))

        } catch (e: Exception) {
            Result.failure(Exception("Nezināma kļūda"))
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

    override fun logout() {
        auth.signOut()
    }
}

actual fun provideAuthService(): AuthService = AuthServiceImplAndroid()