package com.fourthwardai.orbit.domain

data class FeedFilter(
    val selectedGroups: Set<String> = emptySet(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val bookmarkedOnly: Boolean = false,
) {
    val hasActiveFilters =
        selectedGroups.isNotEmpty() || selectedCategoryIds.isNotEmpty() || bookmarkedOnly
}
