package com.fourthwardai.orbit.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fourthwardai.orbit.data.preferences.OrbitPreferencesDataStore
import com.fourthwardai.orbit.ui.settings.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsRepositoryTest {
    private lateinit var context: Context
    private lateinit var dataStore: OrbitPreferencesDataStore
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = OrbitPreferencesDataStore(context)
        repository = SettingsRepository(dataStore)
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
