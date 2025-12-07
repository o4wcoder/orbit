package com.fourthwardai.orbit.ui.newsfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourthwardai.orbit.di.IODispatcher
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.network.onFailure
import com.fourthwardai.orbit.network.onSuccess
import com.fourthwardai.orbit.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _categories = MutableStateFlow(emptyList<Category>())
    val categories = _categories.asStateFlow()

    private val _filter = MutableStateFlow(FeedFilter())
    val filter = _filter.asStateFlow()

    private val _dataState = MutableStateFlow(NewsFeedDataState())
    private val dataState
        get() = _dataState.value

    val uiState: StateFlow<NewsFeedUiModel> =
        _dataState
            .map { dataState ->
                if (dataState.isLoading) {
                    NewsFeedUiModel.Loading
                } else {
                    dataState.toContentUiModel()
                }
            }
            .catch {
                currentCoroutineContext().ensureActive()
                Timber.e("Failed to set uiModel with data. Error = ${it.message}")
            }
            .flowOn(ioDispatcher)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NewsFeedUiModel.Loading,
            )

    init {
        observeArticles()
        loadCategories()
        refreshArticles()
    }

    private fun observeArticles() {
        viewModelScope.launch(ioDispatcher) {
            combine(
                articleRepository.articles,
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

                    matchesGroup && matchesCategory
                }
            }.collect { filteredArticles ->
                _dataState.update { it.copy(articles = filteredArticles) }
            }
        }
    }

    fun onFiltersApplied(
        selectedGroups: Set<String>,
        selectedCategoryIds: Set<String>,
    ) {
        _filter.value = FeedFilter(
            selectedGroups = selectedGroups,
            selectedCategoryIds = selectedCategoryIds,
        )
    }

    private fun loadCategories() {
        viewModelScope.launch(ioDispatcher) {
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
        viewModelScope.launch(ioDispatcher) {
            // If it's the very first load, show the big spinner
            if (dataState.articles.isEmpty()) {
                showLoadingSpinner()
            }
            _dataState.update { it.copy(isRefreshing = true) }

            val result = articleRepository.refreshArticles()
            result.onFailure { error ->
                Timber.e("Failed to refresh articles. Error = ${error.message}")
            }

            _dataState.update { it.copy(isRefreshing = false) }
            hideLoadingSpinner()
        }
    }

    private fun showLoadingSpinner() = _dataState.update { it.copy(isLoading = true) }

    private fun hideLoadingSpinner() = _dataState.update { it.copy(isLoading = false) }
}
