package com.fourthwardai.orbit.repository

import com.fourthwardai.orbit.data.local.ArticleDao
import com.fourthwardai.orbit.data.local.ArticleWithCategories
import com.fourthwardai.orbit.data.local.toDomain
import com.fourthwardai.orbit.data.local.toEntity
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.network.ApiError
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class ArticleRepositoryImpl @Inject constructor(
    private val service: ArticleService,
    private val articleDao: ArticleDao,
    /**
     * Coroutine scope used for background work (collecting DB flows). Tests can inject a TestScope.
     */
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    /** Dispatcher used for IO-bound work. Tests can inject a TestDispatcher here. */
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ArticleRepository {

    private val _articles = MutableStateFlow<List<Article>?>(null)
    override val articles: StateFlow<List<Article>?> = _articles

    init {
        // Observe DB and keep in-memory state in sync with cached articles
        scope.launch {
            articleDao.getAllWithCategories()
                .map { articleWithCategories -> articleWithCategories.map { it.toDomain() } }
                .collect { domainArticles ->
                    _articles.value = domainArticles
                }
        }

        // Also perform an immediate background sync from the network so the DB is kept up to date.
        // We don't block emitting the DB flow above — the DB collector will immediately emit cached data.
        scope.launch {
            try {
                val result = withContext(ioDispatcher) { service.fetchArticles() }
                if (result is ApiResult.Success) {
                    val articlesWithCategories = mapArticlesWithCategories(result.data)
                    // replaceAll runs in a transaction on the DAO
                    articleDao.replaceAll(articlesWithCategories)
                }
            } catch (t: Throwable) {
                Timber.d("CGH: Failed to sync articles from network: $t")
                ensureActive()
            }
        }
    }

    override suspend fun bookmarkArticle(id: String, isBookmarked: Boolean): ApiResult<Unit> = withContext(ioDispatcher) {
        val article = _articles.value?.find { it.id == id } ?: return@withContext ApiResult.Failure(ApiError.Network("Article not found"))
        val updatedArticle = article.copy(isBookmarked = isBookmarked)
        val updatedArticles = _articles.value?.map { if (it.id == id) updatedArticle else it }
        _articles.value = updatedArticles

        when (val result = service.bookmarkArticle(id, isBookmarked)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> {
                // Rollback local state
                val rolledBackArticles = _articles.value?.map { if (it.id == id) article else it }
                _articles.value = rolledBackArticles
                ApiResult.Failure(result.error)
            }
        }
    }

    override suspend fun refreshArticles(): ApiResult<Unit> = withContext(ioDispatcher) {
        when (val result = service.fetchArticles()) {
            is ApiResult.Success -> {
                // Persist fetched articles and categories into Room (replace all in a transaction)
                try {
                    val articlesWithCategories = mapArticlesWithCategories(result.data)
                    articleDao.replaceAll(articlesWithCategories)
                    // Also update in-memory cache immediately so callers (and tests) see new data
                    _articles.value = result.data
                } catch (e: Exception) {
                    return@withContext ApiResult.Failure(ApiError.Unknown(e.message ?: "Failed to persist articles"))
                }

                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> {
                ApiResult.Failure(result.error)
            }
        }
    }

    override suspend fun getCategories(): ApiResult<List<Category>> = withContext(ioDispatcher) {
        service.fetchArticleCategories()
    }

    private fun mapArticlesWithCategories(articles: List<Article>): List<ArticleWithCategories> =
        articles.map { article ->
            val entity = article.toEntity()
            val categories = article.categories.map { it.toEntity() }
            ArticleWithCategories(article = entity, categories = categories)
        }
}
