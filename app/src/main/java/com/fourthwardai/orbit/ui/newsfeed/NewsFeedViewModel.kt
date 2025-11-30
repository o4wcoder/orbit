package com.fourthwardai.orbit.ui.newsfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourthwardai.orbit.di.IODispatcher
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.network.onFailure
import com.fourthwardai.orbit.network.onSuccess
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
    private val articleService: ArticleService,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _categories = MutableStateFlow(emptyList<Category>())
    val categories = _categories.asStateFlow()

    private val _dataState = MutableStateFlow(NewsFeedDataState())
    private val dataState
        get() = _dataState.value

    val uiState: StateFlow<NewsFeedUiModel> =
        _dataState
            .map { dataState ->
                if (dataState.isLoading) {
                    NewsFeedUiModel.Loading
                } else {
                    NewsFeedUiModel.Content(articles = dataState.articles, isRefreshing = dataState.isRefreshing)
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
        loadCategories()
        loadArticles()
    }

    private fun loadCategories() {
        viewModelScope.launch(ioDispatcher) {
            val categoriesResult = articleService.fetchArticleCategories()
            categoriesResult.onSuccess { categories ->
                _categories.update { categories }
            }
            categoriesResult.onFailure { error ->
                Timber.e("Failed to fetch categories. Error = ${error.message}")
            }
        }
    }
    private fun loadArticles() {
        viewModelScope.launch(ioDispatcher) {
            showLoadingSpinner()
            fetchArticles()
            hideLoadingSpinner()
        }
    }

    fun refreshArticles() {
        viewModelScope.launch(ioDispatcher) {
            _dataState.update { it.copy(isRefreshing = true) }
            fetchArticles()
            _dataState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun fetchArticles() {
        val articlesResult = articleService.fetchArticles()
        articlesResult.onSuccess { articles ->
            _dataState.update { it.copy(articles = articles) }
        }
        articlesResult.onFailure { error ->
            Timber.e("Failed to fetch articles. Error = ${error.message}")
        }
    }

    private fun showLoadingSpinner() = _dataState.update { it.copy(isLoading = true) }

    private fun hideLoadingSpinner() = _dataState.update { it.copy(isLoading = false) }
}
