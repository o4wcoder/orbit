package com.fourthwardai.orbit.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourthwardai.orbit.repository.ArticleRepository
import com.fourthwardai.orbit.ui.ArticleListViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SavedArticlesViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    private val delegate = ArticleListViewModelDelegate(
        articleRepository = articleRepository,
        viewModelScope = viewModelScope,
        bookmarkedOnlyDefault = true,
    )

    val categories = delegate.categories
    val filter = delegate.filter
    val uiState = delegate.uiState
    val pagedArticles = delegate.pagedArticles

    fun onFiltersApplied(selectedGroups: Set<String>, selectedCategoryIds: Set<String>) {
        delegate.applyFilters(selectedGroups, selectedCategoryIds)
    }

    fun onBookmarkClick(id: String, isBookmarked: Boolean) {
        delegate.bookmarkArticle(id, isBookmarked)
    }
}
