package com.fourthwardai.orbit.ui.newsfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.repository.ArticleRepository
import com.fourthwardai.orbit.ui.ArticleListViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
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

    val pagedArticles: Flow<PagingData<Article>> =
        filter
            .flatMapLatest { filter ->
                articleRepository
                    .pagedArticles()
                    .map { pagingData ->
                        pagingData.filter { article ->
                            val matchesGroup = filter.selectedGroups.isEmpty() ||
                                article.categories.any { it.group in filter.selectedGroups }
                            val matchesCategory = filter.selectedCategoryIds.isEmpty() ||
                                article.categories.any { it.id in filter.selectedCategoryIds }
                            val matchesBookmarked = !filter.bookmarkedOnly || article.isBookmarked
                            matchesGroup && matchesCategory && matchesBookmarked
                        }
                    }
            }
            .cachedIn(viewModelScope)

    fun onFiltersApplied(selectedGroups: Set<String>, selectedCategoryIds: Set<String>) {
        delegate.applyFilters(selectedGroups, selectedCategoryIds)
    }

    fun onBookmarkClick(id: String, isBookmarked: Boolean) {
        delegate.bookmarkArticle(id, isBookmarked)
    }
}
