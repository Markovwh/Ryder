package common.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore("user_prefs")

class UserPreferences(private val context: Context) {

    private val REMEMBER_ME = booleanPreferencesKey("remember_me")
    private val EMAIL = stringPreferencesKey("email")
    private val PASSWORD = stringPreferencesKey("password")

    suspend fun saveLogin(email: String, password: String, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[EMAIL] = if (remember) email else ""
            prefs[PASSWORD] = if (remember) password else ""
            prefs[REMEMBER_ME] = remember
        }
    }

    suspend fun getSavedLogin(): Triple<String, String, Boolean> {
        val prefs = context.dataStore.data.first()
        val email = prefs[EMAIL] ?: ""
        val password = prefs[PASSWORD] ?: ""
        val remember = prefs[REMEMBER_ME] ?: false
        return Triple(email, password, remember)
    }
}