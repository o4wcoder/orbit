package com.fourthwardai.orbit.ui.newsfeed

import com.fourthwardai.orbit.domain.Article

sealed interface NewsFeedUiModel {
    val isRefreshing: Boolean

    data object Loading : NewsFeedUiModel {
        override val isRefreshing: Boolean = false
    }

    data class Content(
        val articles: List<Article> = emptyList(),
        override val isRefreshing: Boolean = false,
    ) : NewsFeedUiModel
}
