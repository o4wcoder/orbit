package com.fourthwardai.orbit.ui

import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.network.onFailure
import com.fourthwardai.orbit.network.onSuccess
import com.fourthwardai.orbit.repository.ArticleRepository
import com.fourthwardai.orbit.ui.newsfeed.NewsFeedDataState
import com.fourthwardai.orbit.ui.newsfeed.NewsFeedUiModel
import com.fourthwardai.orbit.ui.newsfeed.toContentUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ArticleListViewModelDelegate(
    private val articleRepository: ArticleRepository,
    private val viewModelScope: CoroutineScope,
    bookmarkedOnlyDefault: Boolean = false,
) {
    private val _categories = MutableStateFlow(emptyList<Category>())
    val categories = _categories.asStateFlow()

    private val _filter = MutableStateFlow(FeedFilter(bookmarkedOnly = bookmarkedOnlyDefault))
    val filter = _filter.asStateFlow()

    private val _dataState = MutableStateFlow(NewsFeedDataState())
    private val dataState
        get() = _dataState.value

    val uiState: StateFlow<NewsFeedUiModel> =
        _dataState
            .map { dataState ->
                val articles = dataState.articles

                when {
                    articles == null -> NewsFeedUiModel.Loading
                    articles.isEmpty() -> NewsFeedUiModel.Empty
                    else -> dataState.toContentUiModel()
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NewsFeedUiModel.Loading,
            )

    init {
        observeArticles()
        loadCategories()
    }

    private fun observeArticles() {
        viewModelScope.launch {
            combine(
                articleRepository.articles.filterNotNull(),
                _filter,
            ) { articles, filter ->
                articles.filter { article ->
                    val matchesGroup =
                        filter.selectedGroups.isEmpty() ||
                            article.categories.any { category ->
                                category.group in filter.selectedGroups
                            }

                    val matchesCategory =
                        filter.selectedCategoryIds.isEmpty() ||
                            article.categories.any { category ->
                                category.id in filter.selectedCategoryIds
                            }

                    val matchesBookmarked = !filter.bookmarkedOnly || article.isBookmarked

                    matchesGroup && matchesCategory && matchesBookmarked
                }
            }.collect { filteredArticles ->
                _dataState.update { it.copy(articles = filteredArticles) }
            }
        }
    }

    fun applyFilters(
        selectedGroups: Set<String>,
        selectedCategoryIds: Set<String>,
    ) {
        _filter.update { it.copy(selectedGroups = selectedGroups, selectedCategoryIds = selectedCategoryIds) }
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
