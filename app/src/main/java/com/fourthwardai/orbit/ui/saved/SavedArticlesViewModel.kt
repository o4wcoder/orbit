package com.fourthwardai.orbit.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.network.onFailure
import com.fourthwardai.orbit.network.onSuccess
import com.fourthwardai.orbit.repository.ArticleRepository
import com.fourthwardai.orbit.ui.newsfeed.NewsFeedUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SavedArticlesViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    private val _categories = MutableStateFlow(emptyList<Category>())
    val categories = _categories.asStateFlow()

    private val _filter = MutableStateFlow(FeedFilter())
    val filter = _filter.asStateFlow()

    private val _uiModel = MutableStateFlow<NewsFeedUiModel>(NewsFeedUiModel.Loading)
    val uiModel = _uiModel.asStateFlow()

    init {
        observeSavedArticles()
        loadCategories()
    }

    private fun observeSavedArticles() {
        viewModelScope.launch {
            combine(
                articleRepository.articles.filterNotNull().map { list -> list.filter { it.isBookmarked } },
                _filter,
            ) { savedArticles, filter ->
                savedArticles.filter { article ->
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
                _uiModel.value = NewsFeedUiModel.Content(
                    articles = filteredArticles,
                )
            }
        }
    }

    fun onFiltersApplied(
        selectedGroups: Set<String>,
        selectedCategoryIds: Set<String>,
        bookmarkedOnly: Boolean,
    ) {
        _filter.value = FeedFilter(
            selectedGroups = selectedGroups,
            selectedCategoryIds = selectedCategoryIds,
            bookmarkedOnly = bookmarkedOnly,
        )
    }

    fun onBookmarkClick(id: String, isBookmarked: Boolean) {
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
}
