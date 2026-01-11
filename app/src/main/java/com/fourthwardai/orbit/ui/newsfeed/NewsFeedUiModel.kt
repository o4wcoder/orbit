package com.fourthwardai.orbit.ui.newsfeed

import androidx.paging.PagingData
import com.fourthwardai.orbit.domain.Article

sealed interface NewsFeedUiModel {
    val isRefreshing: Boolean

    data object Loading : NewsFeedUiModel {
        override val isRefreshing: Boolean = false
    }

    data class Content(
        val articles: PagingData<Article> = PagingData.empty(),
        override val isRefreshing: Boolean = false,
    ) : NewsFeedUiModel

    data object Empty : NewsFeedUiModel {
        override val isRefreshing: Boolean = false
    }
}
