package com.fourthwardai.orbit.ui.newsfeed

import com.fourthwardai.orbit.domain.Article

sealed interface NewsFeedUiModel {

    data object Loading : NewsFeedUiModel
    data class Content(
        val articles: List<Article> = emptyList(),
    ) : NewsFeedUiModel
}
