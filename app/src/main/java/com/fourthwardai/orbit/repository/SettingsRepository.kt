package com.fourthwardai.orbit.repository

import com.fourthwardai.orbit.data.preferences.OrbitPreferencesDataStore
import com.fourthwardai.orbit.ui.settings.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: OrbitPreferencesDataStore,
) {
    fun themePreference(): Flow<ThemePreference> = dataStore.getTheme().map { raw ->
        when (raw) {
            ThemePreference.Light.name -> ThemePreference.Light
            ThemePreference.Dark.name -> ThemePreference.Dark
            else -> ThemePreference.System
        }
    }

    suspend fun setThemePreference(pref: ThemePreference) = dataStore.setTheme(pref.name)

    fun dynamicColorEnabled(): Flow<Boolean> = dataStore.isDynamicColorEnabled()

    suspend fun setDynamicColorEnabled(enabled: Boolean) = dataStore.setDynamicColorEnabled(enabled)
}
