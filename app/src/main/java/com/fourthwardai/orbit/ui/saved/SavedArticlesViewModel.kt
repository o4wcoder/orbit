package com.fourthwardai.orbit.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.repository.ArticleRepository
import com.fourthwardai.orbit.ui.ArticleListViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
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

    val pagedArticles: Flow<PagingData<Article>> =
        filter
            .flatMapLatest { filter ->
                articleRepository
                    .pagedSavedArticles(filter)
            }
            .cachedIn(viewModelScope)

    fun onFiltersApplied(selectedGroups: Set<String>, selectedCategoryIds: Set<String>) {
        delegate.applyFilters(selectedGroups, selectedCategoryIds)
    }

    fun onBookmarkClick(id: String, isBookmarked: Boolean) {
        delegate.bookmarkArticle(id, isBookmarked)
    }
}
