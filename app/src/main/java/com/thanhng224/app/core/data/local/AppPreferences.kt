package com.thanhng224.app.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val SNAKE_HIGH_SCORE = intPreferencesKey("snake_high_score")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
        }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
        }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] ?: false
        }
    
    val highScore: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SNAKE_HIGH_SCORE] ?: 0
        }

    suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = isFirstLaunch
        }
    }

    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }
    
    suspend fun updateHighScore(score: Int) {
        context.dataStore.edit { preferences ->
            val currentHigh = preferences[PreferencesKeys.SNAKE_HIGH_SCORE] ?: 0
            if (score > currentHigh) {
                preferences[PreferencesKeys.SNAKE_HIGH_SCORE] = score
            }
        }
    }
}
