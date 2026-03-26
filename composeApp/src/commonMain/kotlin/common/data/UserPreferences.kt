package common.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore("user_prefs")

class UserPreferences(private val context: Context) {

    private val REMEMBER_ME = booleanPreferencesKey("remember_me")
    private val DARK_THEME = booleanPreferencesKey("dark_theme")

    suspend fun setRememberMe(remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REMEMBER_ME] = remember
        }
    }

    suspend fun getRememberMe(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[REMEMBER_ME] ?: false
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_THEME] = dark
        }
    }

    suspend fun getDarkTheme(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[DARK_THEME] ?: false
    }
}