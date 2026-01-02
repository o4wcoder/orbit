package com.fourthwardai.orbit.ui.categoryfilter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.theme.OrbitTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDialog(
    categories: List<Category>,
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
                    onApply = { onApply(selectedGroups, selectedCategoryIds) },
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
    onApply: () -> Unit,
    onGroupToggled: (String) -> Unit,
    onCategoryToggled: (String) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 140.dp,
            ),
        ) {
            item {
                VerticalSpacer(16.dp)

                Text(
                    text = stringResource(R.string.filters_group_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )

                VerticalSpacer(8.dp)
            }

            item {
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
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.filters_category_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
            if (visibleCategories.isEmpty()) {
                item {
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
                }
            } else {
                item {
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
                        visibleCategories.forEachIndexed { index, category ->
                            CategoryRow(
                                category = category,
                                checked = category.id in selectedCategoryIds,
                                onToggle = { onCategoryToggled(category.id) },
                            )
                            if (index < visibleCategories.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        BottomApplyBar(
            selectedCount = selectedCategoryIds.count() + selectedGroups.count(),
            onApply = onApply,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = category.group,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun BottomApplyBar(
    selectedCount: Int,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            modifier = Modifier.shadow(4.dp, ButtonDefaults.shape),
            onClick = onApply,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Apply Filters ($selectedCount)")
        }
    }
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
