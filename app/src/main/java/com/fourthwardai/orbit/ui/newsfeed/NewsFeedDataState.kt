package com.fourthwardai.orbit.ui.newsfeed

import com.fourthwardai.orbit.domain.Article

data class NewsFeedDataState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val showFilterDialog: Boolean = false,
)

fun NewsFeedDataState.toContentUiModel() = NewsFeedUiModel.Content(
    articles = articles,
    isRefreshing = isRefreshing,
    showFilterDialog = showFilterDialog,
)
