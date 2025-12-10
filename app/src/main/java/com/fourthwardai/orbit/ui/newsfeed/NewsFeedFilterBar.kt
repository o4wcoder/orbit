package com.fourthwardai.orbit.ui.newsfeed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.ui.theme.OrbitTheme

@Composable
fun NewsFeedActiveFiltersBar(
    categories: List<Category>,
    filters: FeedFilter,
    onApply: (selectedGroups: Set<String>, selectedCategoryIds: Set<String>, bookmarkedOnly: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = filters.hasActiveFilters,
        modifier = modifier,
    ) {
        val isDark = isSystemInDarkTheme()
        val categoriesById = remember(categories) {
            categories.associateBy { it.id }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp),
            ) {
                // Group filters
                items(filters.selectedGroups.toList(), key = { it }) { groupName ->
                    GroupFilterChip(
                        label = groupName,
                        onRemove = {
                            val newGroups = filters.selectedGroups - groupName
                            onApply(newGroups, filters.selectedCategoryIds, filters.bookmarkedOnly)
                        },
                    )
                }

                // Category filters
                items(filters.selectedCategoryIds.toList(), key = { it }) { categoryId ->
                    val category = categoriesById[categoryId]
                    if (category != null) {
                        CategoryFilterChip(
                            category = category,
                            isDarkTheme = isDark,
                            onRemove = {
                                val newCategoryIds = filters.selectedCategoryIds - category.id
                                onApply(filters.selectedGroups, newCategoryIds, filters.bookmarkedOnly)
                            },
                        )
                    }
                }
                // Bookmarked filter
                if (filters.bookmarkedOnly) {
                    item {
                        BookmarkFilterChip(
                            onRemove = {
                                onApply(filters.selectedGroups, filters.selectedCategoryIds, false)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupFilterChip(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )

    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.filters_remove_description),
                modifier = Modifier.padding(start = 4.dp),
            )
        },
        colors = colors,
        modifier = modifier,
    )
}

@Composable
private fun CategoryFilterChip(
    category: Category,
    isDarkTheme: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor: Color = if (isDarkTheme) category.colorDark else category.colorLight

    // Slightly translucent container so it doesn't scream on the background
    val containerColor = baseColor.copy(alpha = 0.18f)
    val selectedContainerColor = baseColor
    val onColor = contentColorFor(baseColor)

    val colors = FilterChipDefaults.filterChipColors(
        containerColor = containerColor,
        selectedContainerColor = selectedContainerColor,
        labelColor = onColor,
        selectedLabelColor = onColor,
        iconColor = onColor,
        selectedLeadingIconColor = onColor,
    )

    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(category.name) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.Close,
                tint = onColor,
                contentDescription = stringResource(R.string.filters_remove_description),
                modifier = Modifier.padding(start = 4.dp),
            )
        },
        colors = colors,
        modifier = modifier,
    )
}

@Composable
private fun BookmarkFilterChip(
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookmarkChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        labelColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.primary,
        iconColor = MaterialTheme.colorScheme.primary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
    )

    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(stringResource(R.string.filters_bookmarked)) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.filters_remove_description),
                modifier = Modifier.padding(start = 4.dp),
            )
        },
        colors = bookmarkChipColors,
        modifier = modifier,
    )
}

// Helper like Material's contentColorFor
@Composable
private fun contentColorFor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
}

@Preview(showBackground = true, name = "Active Filters Bar")
@Composable
fun NewsFeedActiveFiltersBarPreview() {
    OrbitTheme {
        val sampleCategories = listOf(
            Category(
                id = "android",
                name = "Android",
                group = "Mobile",
                colorLight = Color(0xFF2E7D32),
                colorDark = Color(0xFF81C784),
            ),
            Category(
                id = "kotlin",
                name = "Kotlin",
                group = "Programming",
                colorLight = Color(0xFF512DA8),
                colorDark = Color(0xFFB39DDB),
            ),
            Category(
                id = "ai",
                name = "AI",
                group = "AI & ML",
                colorLight = Color(0xFFF57C00),
                colorDark = Color(0xFFFFCC80),
            ),
        )

        val sampleFilters = FeedFilter(
            selectedGroups = setOf("Mobile", "AI & ML"),
            selectedCategoryIds = setOf("android", "ai"),
            bookmarkedOnly = true,

        )
        NewsFeedActiveFiltersBar(
            categories = sampleCategories,
            filters = sampleFilters,
            onApply = { _, _, _ -> },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background),
        )
    }
}
