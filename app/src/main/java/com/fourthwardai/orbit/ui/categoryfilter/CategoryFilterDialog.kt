package com.fourthwardai.orbit.ui.categoryfilter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.theme.OrbitTheme
import com.fourthwardai.orbit.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDialog(
    categories: List<Category>,
    // Initial selections from ViewModel / caller
    initialSelectedGroups: Set<String> = emptySet(),
    initialSelectedCategoryIds: Set<String> = emptySet(),
    onApply: (selectedGroups: Set<String>, selectedCategoryIds: Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    // Local state inside dialog (hoisted out via onApply)
    var selectedGroups by remember { mutableStateOf(initialSelectedGroups) }
    var selectedCategoryIds by remember { mutableStateOf(initialSelectedCategoryIds) }

    // All distinct groups from category list
    val allGroups: List<String> = remember(categories) {
        categories.map { it.group }.distinct()
    }

    // Filter categories when groups are selected
    val visibleCategories = remember(categories, selectedGroups) {
        if (selectedGroups.isEmpty()) {
            categories
        } else {
            categories.filter { it.group in selectedGroups }
        }
    }

    val hasAnySelection = selectedGroups.isNotEmpty() || selectedCategoryIds.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.filters_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.filters_close_description),
                                )
                            }
                        },
                        actions = {
                            TextButton(
                                enabled = hasAnySelection,
                                onClick = {
                                    selectedGroups = emptySet()
                                    selectedCategoryIds = emptySet()
                                },
                            ) {
                                Text(stringResource(R.string.filters_clear_all))
                            }
                        },
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        tonalElevation = 3.dp,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                onApply(selectedGroups, selectedCategoryIds)
                                onDismiss()
                            },
                        ) {
                            Text(stringResource(R.string.filters_apply))
                        }
                    }
                },
            ) { innerPadding ->
                FilterContent(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    allGroups = allGroups,
                    visibleCategories = visibleCategories,
                    selectedGroups = selectedGroups,
                    selectedCategoryIds = selectedCategoryIds,
                    onGroupToggled = { group ->
                        selectedGroups = if (group in selectedGroups) {
                            selectedGroups - group
                        } else {
                            selectedGroups + group
                        }
                    },
                    onCategoryToggled = { categoryId ->
                        selectedCategoryIds = if (categoryId in selectedCategoryIds) {
                            selectedCategoryIds - categoryId
                        } else {
                            selectedCategoryIds + categoryId
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterContent(
    modifier: Modifier,
    allGroups: List<String>,
    visibleCategories: List<Category>,
    selectedGroups: Set<String>,
    selectedCategoryIds: Set<String>,
    onGroupToggled: (String) -> Unit,
    onCategoryToggled: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp),
    ) {
        VerticalSpacer(16.dp)

        // GROUP SECTION
        Text(
            text = stringResource(R.string.filters_group_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )

        VerticalSpacer(8.dp)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            allGroups.forEach { group ->
                FilterChip(
                    selected = group in selectedGroups,
                    onClick = { onGroupToggled(group) },
                    label = { Text(group) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.filters_category_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (visibleCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.filters_no_categories),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(
                    items = visibleCategories,
                    key = { it.id },
                ) { category ->
                    CategoryRow(
                        category = category,
                        selected = category.id in selectedCategoryIds,
                        onToggle = { onCategoryToggled(category.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(category.name) },
        supportingContent = {
            // Show group subtly under the name (nice when list is filtered)
            Text(
                category.group,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
            )
        },
        modifier = Modifier
            .fillMaxWidth(),
    )
    HorizontalDivider()
}

@Preview(showSystemUi = true)
@Composable
fun CategoryFilterDialogPreview() {
    val sample = listOf(
        Category("1", "Cloud & DevOps", "Tech", Color.Blue, Color.Red),
        Category("2", "Java", "Tech", Color.Blue, Color.Red),
        Category("3", "Machine Learning", "AI", Color.Blue, Color.Red),
        Category("4", "Web Development", "Tech", Color.Blue, Color.Red),
        Category("5", "Productivity", "Life", Color.Blue, Color.Red),
        Category("6", "Finance & Markets", "Money", Color.Blue, Color.Red),
    )

    OrbitTheme {
        CategoryFilterDialog(
            categories = sample,
            onApply = { groups, categories ->
                // TODO: send to ViewModel
                println("Selected groups: $groups, categories: $categories")
            },
            onDismiss = {},
        )
    }
}
