package com.fourthwardai.orbit.repository

import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Test-only fake implementation of [ArticleRepository].
 *
 * Behaves as an in-memory repository so unit tests can exercise
 * consumers (ViewModels, use cases) without touching Room, Paging, or
 * network/RemoteMediator internals.
 */
class FakeArticleRepository(
    initialArticles: List<Article> = emptyList(),
) : ArticleRepository {

    private val _articles = MutableStateFlow<List<Article>?>(initialArticles)
    override val articles: StateFlow<List<Article>?> = _articles.asStateFlow()

    /** Most recent categories response the fake will return. */
    var categoriesResult: ApiResult<List<Category>> = ApiResult.Success(emptyList())

    /** Result that [bookmarkArticle] should return. */
    var bookmarkResult: ApiResult<Unit> = ApiResult.Success(Unit)

    /** Result that [syncDirtyArticles] should return. */
    var syncDirtyResult: ApiResult<Unit> = ApiResult.Success(Unit)

    private val _pagedArticles = MutableStateFlow<PagingData<Article>>(PagingData.empty())
    private val _pagedSavedArticles = MutableStateFlow<PagingData<Article>>(PagingData.empty())

    /**
     * Replace the in-memory articles list.
     */
    fun setArticles(articles: List<Article>) {
        _articles.value = articles
    }

    /**
     * Convenience to set paging data for the main feed.
     */
    fun setPagedArticles(articles: List<Article>) {
        _pagedArticles.value = PagingData.from(articles)
    }

    /**
     * Convenience to set paging data for the saved feed.
     */
    fun setPagedSavedArticles(articles: List<Article>) {
        _pagedSavedArticles.value = PagingData.from(articles)
    }

    override suspend fun bookmarkArticle(id: String, isBookmarked: Boolean): ApiResult<Unit> {
        // Simple optimistic in-memory behavior
        _articles.update { current ->
            current?.map { article ->
                if (article.id == id) article.copy(isBookmarked = isBookmarked) else article
            }
        }
        return bookmarkResult
    }

    override suspend fun getCategories(): ApiResult<List<Category>> = categoriesResult

    override suspend fun syncDirtyArticles(): ApiResult<Unit> = syncDirtyResult

    override fun pagedArticles(filter: FeedFilter): Flow<PagingData<Article>> = _pagedArticles

    override fun pagedSavedArticles(filter: FeedFilter): Flow<PagingData<Article>> = _pagedSavedArticles
}
