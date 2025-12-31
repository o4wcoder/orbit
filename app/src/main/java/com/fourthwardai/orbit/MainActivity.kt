package com.fourthwardai.orbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fourthwardai.orbit.extensions.isSystemInDarkTheme
import com.fourthwardai.orbit.ui.navigation.OrbitAppNavHost
import com.fourthwardai.orbit.ui.theme.LocalWindowClassSize
import com.fourthwardai.orbit.ui.theme.OrbitTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var themeSettings by mutableStateOf(
            OrbitThemeSettings(
                darkTheme = resources.configuration.isSystemInDarkTheme,
                false,
            ),
        )

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    isSystemInDarkTheme(),
                    viewModel.uiState,
                ) { systemDark, uiState ->
                    OrbitThemeSettings(
                        darkTheme = uiState.shouldUseDarkTheme(systemDark),
                        disableDynamicTheming = uiState.shouldDisableDynamicTheming,
                    )
                }
                    .distinctUntilChanged()
                    .onEach { themeSettings = it }
                    .collect()
            }
        }
        enableEdgeToEdge()
        setContent {
            OrbitTheme(
                darkTheme = themeSettings.darkTheme,
                dynamicColor = !themeSettings.disableDynamicTheming,
            ) {
                val windowSizeClass = calculateWindowSizeClass(this)
                CompositionLocalProvider(
                    LocalWindowClassSize provides windowSizeClass,
                ) {
                    OrbitAppNavHost()
                }
            }
        }
    }
}

data class OrbitThemeSettings(
    val darkTheme: Boolean,
    val disableDynamicTheming: Boolean,
)
