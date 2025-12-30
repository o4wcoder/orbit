package com.fourthwardai.orbit.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourthwardai.orbit.repository.ArticleRepository
import com.fourthwardai.orbit.ui.ArticleListViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
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

    fun onFiltersApplied(selectedGroups: Set<String>, selectedCategoryIds: Set<String>, bookmarkedOnly: Boolean) {
        delegate.applyFilters(selectedGroups, selectedCategoryIds, bookmarkedOnly)
    }

    fun onBookmarkClick(id: String, isBookmarked: Boolean) {
        delegate.bookmarkArticle(id, isBookmarked)
    }
}
