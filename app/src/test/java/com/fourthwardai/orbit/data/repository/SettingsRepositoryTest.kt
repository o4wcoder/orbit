package com.fourthwardai.orbit.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fourthwardai.orbit.data.preferences.OrbitPreferencesDataStore
import com.fourthwardai.orbit.repository.SettingsRepository
import com.fourthwardai.orbit.ui.settings.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsRepositoryTest {
    private lateinit var context: Context
    private lateinit var dataStore: OrbitPreferencesDataStore
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear any existing DataStore file before the test to ensure isolation
        clearDataStoreFile()
        
        dataStore = OrbitPreferencesDataStore(context)
        repository = SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        // Clean up the DataStore file after each test to prevent test pollution
        clearDataStoreFile()
    }

    private fun clearDataStoreFile() {
        // Remove the DataStore file to ensure tests start with a clean state
        val dataStoreFile = File(context.filesDir, "datastore/orbit_prefs.preferences_pb")
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }
        // Also remove the parent directory if it's empty
        val dataStoreDir = dataStoreFile.parentFile
        if (dataStoreDir?.exists() == true && dataStoreDir.list()?.isEmpty() == true) {
            dataStoreDir.delete()
        }
    }

    @Test
    fun `default theme is system`() = runTest {
        val theme = repository.themePreference().first()
        assertEquals(ThemePreference.System, theme)
    }

    @Test
    fun `set theme to light and read back`() = runTest {
        repository.setThemePreference(ThemePreference.Light)
        val theme = repository.themePreference().first()
        assertEquals(ThemePreference.Light, theme)
    }

    @Test
    fun `dynamic color default true and can be disabled`() = runTest {
        val defaultValue = repository.dynamicColorEnabled().first()
        assertEquals(true, defaultValue)

        repository.setDynamicColorEnabled(false)
        val updated = repository.dynamicColorEnabled().first()
        assertEquals(false, updated)
    }
}
