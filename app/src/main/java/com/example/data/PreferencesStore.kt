package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "yaqeen_prefs")

class PreferencesStore(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("app_language")
    private val keyDarkMode = booleanPreferencesKey("dark_mode")

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[keyLanguage])
    }

    val darkModeFlow: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[keyDarkMode]
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[keyLanguage] = language.code
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyDarkMode] = enabled
        }
    }
}
