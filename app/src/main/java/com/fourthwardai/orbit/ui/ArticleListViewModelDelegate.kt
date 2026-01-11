package com.fourthwardai.orbit.ui

import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.network.onFailure
import com.fourthwardai.orbit.network.onSuccess
import com.fourthwardai.orbit.repository.ArticleRepository
import com.fourthwardai.orbit.ui.newsfeed.NewsFeedDataState
import com.fourthwardai.orbit.ui.newsfeed.NewsFeedUiModel
import com.fourthwardai.orbit.ui.newsfeed.toContentUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleListViewModelDelegate(
    private val articleRepository: ArticleRepository,
    private val viewModelScope: CoroutineScope,
    bookmarkedOnlyDefault: Boolean = false,
) {
    private val _categories = MutableStateFlow(emptyList<Category>())
    val categories = _categories.asStateFlow()

    private val _filter = MutableStateFlow(FeedFilter(bookmarkedOnly = bookmarkedOnlyDefault))
    val filter = _filter.asStateFlow()
    
    val pagedArticles: Flow<PagingData<Article>> =
        _filter
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


    private val _dataState = MutableStateFlow(NewsFeedDataState())

    val uiState: StateFlow<NewsFeedUiModel> =
        _dataState
            .map { dataState ->
                dataState.toContentUiModel()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NewsFeedUiModel.Loading,
            )

    init {
        loadCategories()
    }

    fun applyFilters(
        selectedGroups: Set<String>,
        selectedCategoryIds: Set<String>,
    ) {
        _filter.update {
            it.copy(
                selectedGroups = selectedGroups,
                selectedCategoryIds = selectedCategoryIds
            )
        }
    }

    fun bookmarkArticle(id: String, isBookmarked: Boolean) {
        viewModelScope.launch {
            articleRepository.bookmarkArticle(id, isBookmarked)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val categoriesResult = articleRepository.getCategories()
            categoriesResult.onSuccess { categories ->
                _categories.update { categories }
            }
            categoriesResult.onFailure { error ->
                Timber.e("Failed to fetch categories. Error = ${error.message}")
            }
        }
    }

    fun refreshArticles() {
        viewModelScope.launch {
            _dataState.update { it.copy(isRefreshing = true) }

            val result = articleRepository.refreshArticles()
            result.onFailure { error ->
                Timber.e("Failed to refresh articles. Error = ${error.message}")
            }
            _dataState.update { it.copy(isRefreshing = false) }
        }
    }
}
