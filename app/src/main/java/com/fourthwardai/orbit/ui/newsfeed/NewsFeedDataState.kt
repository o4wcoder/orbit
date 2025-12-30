package com.fourthwardai.orbit.ui.newsfeed

import com.fourthwardai.orbit.domain.Article

data class NewsFeedDataState(
    val articles: List<Article>? = null,
    // val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
)

fun NewsFeedDataState.toContentUiModel() = NewsFeedUiModel.Content(
    articles = articles ?: emptyList(),
    isRefreshing = isRefreshing,
)
