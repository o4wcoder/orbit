package com.fourthwardai.orbit.ui.newsfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    //  @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _dataState = MutableStateFlow(NewsFeedDataState())
    private val dataState
        get() = _dataState.value

    val uiState: StateFlow<NewsFeedUiModel> =
        _dataState
            .map { dataState ->
                NewsFeedUiModel.Content(articles = dataState.articles)
            }
            .catch {
                currentCoroutineContext().ensureActive()
                Timber.e("Failed to set uiModel with data. Error = ${it.message}")
            }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NewsFeedUiModel.Loading,
            )

    init {
        fetchArticles()
    }

    private fun fetchArticles() {
        viewModelScope.launch(Dispatchers.IO) {
            val articles = articleService.fetchArticles()
            _dataState.update { it.copy(articles = articles) }
        }
    }
}
