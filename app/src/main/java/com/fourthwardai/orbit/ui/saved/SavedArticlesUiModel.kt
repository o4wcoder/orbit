package com.fourthwardai.orbit.ui.saved

import com.fourthwardai.orbit.domain.Article

interface SavedArticlesUiModel {

    data object Loading : SavedArticlesUiModel

    data class Content(
        val articles: List<Article> = emptyList(),
    ) : SavedArticlesUiModel

    data object Empty : SavedArticlesUiModel
}
