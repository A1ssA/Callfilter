package com.callguard.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.callguard.app.data.local.FilterMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "callguard_settings")

data class FilterSettings(
    val activeMode: FilterMode = FilterMode.DISABLED,
    val blockHiddenNumbers: Boolean = true,
    val allowAllContactsInWhitelist: Boolean = false,
    val allowFavoriteContacts: Boolean = true,
    val allowRepeatedCalls: Boolean = true,
    val repeatedCallWindowMinutes: Int = 5
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ACTIVE_MODE = stringPreferencesKey("active_mode")
        val BLOCK_HIDDEN = booleanPreferencesKey("block_hidden")
        val ALLOW_ALL_CONTACTS = booleanPreferencesKey("allow_all_contacts")
        val ALLOW_FAVORITES = booleanPreferencesKey("allow_favorites")
        val ALLOW_REPEATED = booleanPreferencesKey("allow_repeated")
    }

    val settingsFlow: Flow<FilterSettings> = context.dataStore.data.map { prefs ->
        FilterSettings(
            activeMode = FilterMode.valueOf(prefs[Keys.ACTIVE_MODE] ?: FilterMode.DISABLED.name),
            blockHiddenNumbers = prefs[Keys.BLOCK_HIDDEN] ?: true,
            allowAllContactsInWhitelist = prefs[Keys.ALLOW_ALL_CONTACTS] ?: false,
            allowFavoriteContacts = prefs[Keys.ALLOW_FAVORITES] ?: true,
            allowRepeatedCalls = prefs[Keys.ALLOW_REPEATED] ?: true
        )
    }

    suspend fun getCurrentSettings(): FilterSettings = settingsFlow.first()

    suspend fun setFilterMode(mode: FilterMode) {
        context.dataStore.edit { it[Keys.ACTIVE_MODE] = mode.name }
    }

    suspend fun updateBooleanSetting(keyName: String, value: Boolean) {
        context.dataStore.edit { prefs ->
            when (keyName) {
                "block_hidden" -> prefs[Keys.BLOCK_HIDDEN] = value
                "allow_all_contacts" -> prefs[Keys.ALLOW_ALL_CONTACTS] = value
                "allow_favorites" -> prefs[Keys.ALLOW_FAVORITES] = value
                "allow_repeated" -> prefs[Keys.ALLOW_REPEATED] = value
            }
        }
    }
}
