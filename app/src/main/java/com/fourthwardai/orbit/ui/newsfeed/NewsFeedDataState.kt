package com.fourthwardai.orbit.ui.newsfeed

data class NewsFeedDataState(
    //  val articles: PagingData<Article>? = null,
    // val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
)

fun NewsFeedDataState.toContentUiModel() = NewsFeedUiModel.Content(
    isRefreshing = isRefreshing,
)
