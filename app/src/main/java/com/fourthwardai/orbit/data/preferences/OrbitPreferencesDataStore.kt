package com.fourthwardai.orbit.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val DATASTORE_NAME = "orbit_prefs"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

@Singleton
class OrbitPreferencesDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
    }

    private val dataStore: DataStore<Preferences> = context.dataStore

    fun getTheme(): Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[Keys.THEME] }

    suspend fun setTheme(value: String) {
        dataStore.edit { prefs -> prefs[Keys.THEME] = value }
    }

    fun isDynamicColorEnabled(): Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[Keys.DYNAMIC_COLOR] ?: true }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DYNAMIC_COLOR] = enabled }
    }
}
