package com.fourthwardai.orbit.ui.newsfeed

data class NewsFeedDataState(
    val isRefreshing: Boolean = false,
)

fun NewsFeedDataState.toContentUiModel() = NewsFeedUiModel.Content(
    isRefreshing = isRefreshing,
)
