package com.fourthwardai.orbit.domain

data class FeedFilter(
    val selectedGroups: Set<String> = emptySet(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val bookmarkedOnly: Boolean = false,
) {
    val hasUserSelectedFilters =
        selectedGroups.isNotEmpty() || selectedCategoryIds.isNotEmpty()
}
