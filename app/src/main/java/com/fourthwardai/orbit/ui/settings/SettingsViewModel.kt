package com.fourthwardai.orbit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourthwardai.orbit.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    val theme: StateFlow<ThemePreference> = repository.themePreference()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.System)

    val dynamicColorEnabled: StateFlow<Boolean> = repository.dynamicColorEnabled()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setTheme(pref: ThemePreference) {
        viewModelScope.launch { repository.setThemePreference(pref) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColorEnabled(enabled) }
    }
}
