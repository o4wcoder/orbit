package com.fourthwardai.orbit.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.fourthwardai.orbit.data.local.ArticleDao
import com.fourthwardai.orbit.data.local.ArticleWithCategories
import com.fourthwardai.orbit.data.local.toDomain
import com.fourthwardai.orbit.data.local.toEntity
import com.fourthwardai.orbit.di.IODispatcher
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.network.ApiError
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.network.isTransient
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import com.fourthwardai.orbit.work.scheduleArticleSync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
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
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:ApplicationContext private val context: Context,
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
        // Persist the change in Room so it's available to the worker later
        val dbArticle = articleDao.getById(id) ?: return@withContext ApiResult.Failure(ApiError.Unknown("Article not found"))
        val updatedEntity = dbArticle.copy(isBookmarked = isBookmarked, isDirty = true, lastModified = System.currentTimeMillis())
        articleDao.insert(updatedEntity)

        // Also keep optimistic in-memory update for immediate UI feedback
        val previousArticle = _articles.value?.find { it.id == id }?.copy()
        val updatedArticle = previousArticle?.copy(isBookmarked = isBookmarked)
        _articles.value = _articles.value?.map { if (it.id == id) (updatedArticle ?: it) else it }

        when (val result = service.bookmarkArticle(id, isBookmarked)) {
            is ApiResult.Success -> {
                // Mark as synced
                articleDao.insert(updatedEntity.copy(isDirty = false))
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> {
                Timber.d("Bookmark network failed: ${result.error}")
                if (result.error.isTransient()) {
                    // keep the local dirty flag and schedule background retry
                    scheduleArticleSync(context)
                    ApiResult.Failure(result.error)
                } else {
                    // Permanent failure (e.g., 4xx). Revert local DB state and optimistic UI.
                    Timber.d("Permanent bookmark failure for $id, reverting local change: ${result.error}")
                    // Re-insert original DB article (not dirty)
                    articleDao.insert(dbArticle.copy(isDirty = false))
                    // rollback in-memory
                    if (previousArticle != null) {
                        _articles.value = _articles.value?.map { if (it.id == id) previousArticle else it }
                    }
                    ApiResult.Failure(result.error)
                }
            }
        }
    }

    override suspend fun syncDirtyArticles(): ApiResult<Unit> = withContext(ioDispatcher) {
        try {
            val dirty = articleDao.getDirtyArticles()
            if (dirty.isEmpty()) return@withContext ApiResult.Success(Unit)

            // Try to sync each dirty article individually; collect failures
            dirty.forEach { entity ->
                val id = entity.id
                val desiredBookmark = entity.isBookmarked
                when (val result = service.bookmarkArticle(id, desiredBookmark)) {
                    is ApiResult.Success -> {
                        // mark as synced
                        articleDao.insert(entity.copy(isDirty = false))
                    }
                    is ApiResult.Failure -> {
                        Timber.d("Failed to sync article $id: ${result.error}")
                        if (result.error.isTransient()) {
                            // transient -> ask WorkManager to retry the entire job
                            return@withContext ApiResult.Failure(result.error)
                        } else {
                            // permanent -> mark as not dirty and continue to next item
                            Timber.d("Permanent failure syncing $id, marking as not dirty: ${result.error}")
                            articleDao.insert(entity.copy(isDirty = false))
                        }
                    }
                }
            }

            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Failure(ApiError.Unknown(e.message ?: "syncDirtyArticles failed"))
        }
    }

    override suspend fun refreshArticles(): ApiResult<Unit> = withContext(ioDispatcher) {
        when (val result = service.fetchArticles()) {
            is ApiResult.Success -> {
                try {
                    val articlesWithCategories = mapArticlesWithCategories(result.data)
                    articleDao.replaceAll(articlesWithCategories)
                    _articles.value = result.data
                    ApiResult.Success(Unit)
                } catch (e: Exception) {
                    ApiResult.Failure(ApiError.Unknown(e.message ?: "Failed to persist articles"))
                }
            }
            is ApiResult.Failure -> {
                ApiResult.Failure(result.error)
            }
        }
    }

    override suspend fun getCategories(): ApiResult<List<Category>> = withContext(ioDispatcher) {
        service.fetchArticleCategories()
    }

    override fun pagedArticles(): Flow<PagingData<Article>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                prefetchDistance = 10,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { articleDao.pagingSource() },
        ).flow
            .map { pagingData ->
                pagingData.map { it.toDomain() }
            }
    }

    private fun mapArticlesWithCategories(articles: List<Article>): List<ArticleWithCategories> =
        articles.map { article ->
            val entity = article.toEntity()
            val categories = article.categories.map { it.toEntity() }
            ArticleWithCategories(article = entity, categories = categories)
        }
}
