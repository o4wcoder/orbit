package com.fourthwardai.orbit.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourthwardai.orbit.BuildConfig
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.theme.OrbitTheme

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    version: String = BuildConfig.VERSION_NAME,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val selectedTheme by viewModel.theme.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            // Section label
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )

            VerticalSpacer(12.dp)

            // Dynamic Color card
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_dynamic_color_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_dynamic_color_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Switch(
                        checked = dynamicColorEnabled,
                        onCheckedChange = {
                            viewModel.setDynamicColorEnabled(it)
                        },
                    )
                }
            }

            VerticalSpacer(12.dp)

            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                    ),
            ) {
                Column {
                    ThemeOptionRow(
                        label = stringResource(R.string.settings_theme_light),
                        icon = Icons.Filled.LightMode,
                        selected = selectedTheme == ThemePreference.Light,
                        onClick = {
                            viewModel.setTheme(ThemePreference.Light)
                        },
                    )

                    HorizontalDivider()

                    ThemeOptionRow(
                        label = stringResource(R.string.settings_theme_dark),
                        icon = Icons.Filled.DarkMode,
                        selected = selectedTheme == ThemePreference.Dark,
                        onClick = {
                            viewModel.setTheme(ThemePreference.Dark)
                        },
                    )

                    HorizontalDivider()

                    ThemeOptionRow(
                        label = stringResource(R.string.settings_theme_system_default),
                        icon = Icons.Filled.Android,
                        selected = selectedTheme == ThemePreference.System,
                        onClick = {
                            viewModel.setTheme(ThemePreference.System)
                        },
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.settings_version_format, version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_built_by),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        RadioButton(selected = selected, onClick = onClick)
    }
}

enum class ThemePreference {
    Light,
    Dark,
    System,
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    OrbitTheme {
        SettingsScreen(version = "1.0.4-preview")
    }
}
