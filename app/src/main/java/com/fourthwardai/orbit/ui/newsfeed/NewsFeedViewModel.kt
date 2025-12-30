package com.fourthwardai.orbit.ui.newsfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourthwardai.orbit.repository.ArticleRepository
import com.fourthwardai.orbit.ui.ArticleListViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    private val delegate = ArticleListViewModelDelegate(
        articleRepository = articleRepository,
        viewModelScope = viewModelScope,
        bookmarkedOnlyDefault = false,
    )

    val categories = delegate.categories
    val filter = delegate.filter
    val uiState = delegate.uiState

    fun onFiltersApplied(selectedGroups: Set<String>, selectedCategoryIds: Set<String>, bookmarkedOnly: Boolean) {
        delegate.applyFilters(selectedGroups, selectedCategoryIds, bookmarkedOnly)
    }

    fun onBookmarkClick(id: String, isBookmarked: Boolean) {
        delegate.bookmarkArticle(id, isBookmarked)
    }

    fun refreshArticles() {
        delegate.refreshArticles()
    }
}
