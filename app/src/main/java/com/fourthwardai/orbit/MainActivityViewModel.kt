package com.fourthwardai.orbit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourthwardai.orbit.repository.SettingsRepository
import com.fourthwardai.orbit.ui.settings.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<MainActivityUiState> = combine(
        settingsRepository.themePreference(),
        settingsRepository.dynamicColorEnabled(),
    ) { themePreference, dynamicColorEnabled ->
        MainActivityUiState.Success(themePreference, dynamicColorEnabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainActivityUiState.Loading,
    )
}

sealed interface MainActivityUiState {
    object Loading : MainActivityUiState

    data class Success(val themePreference: ThemePreference, val dynamicColorEnabled: Boolean) : MainActivityUiState {

        override val shouldDisableDynamicTheming = !dynamicColorEnabled

        override fun shouldUseDarkTheme(isSystemDarkTheme: Boolean): Boolean {
            return when (themePreference) {
                ThemePreference.Light -> false
                ThemePreference.Dark -> true
                ThemePreference.System -> isSystemDarkTheme
            }
        }
    }

    val shouldDisableDynamicTheming: Boolean get() = true
    fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) = isSystemDarkTheme
}
