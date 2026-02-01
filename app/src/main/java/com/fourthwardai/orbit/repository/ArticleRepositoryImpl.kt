package com.fourthwardai.orbit.repository

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.fourthwardai.orbit.data.local.ArticleDao
import com.fourthwardai.orbit.data.local.DatabaseConstants
import com.fourthwardai.orbit.data.local.OrbitDatabase
import com.fourthwardai.orbit.data.local.toDomain
import com.fourthwardai.orbit.data.paging.ArticlesRemoteMediator
import com.fourthwardai.orbit.di.IODispatcher
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.domain.asDelimiterList
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
    private val orbitDatabase: OrbitDatabase,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:ApplicationContext private val context: Context,
) : ArticleRepository {

    val pagingConfig = PagingConfig(
        pageSize = 30,
        initialLoadSize = 30,
        prefetchDistance = 5,
        enablePlaceholders = false,
    )

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
                // Mark as synced immediately since network call succeeded
                syncDirtyArticles()
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

    override suspend fun getCategories(): ApiResult<List<Category>> = withContext(ioDispatcher) {
        service.fetchArticleCategories()
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun pagedArticles(filter: FeedFilter): Flow<PagingData<Article>> {
        return Pager(
            config = pagingConfig,
            remoteMediator = ArticlesRemoteMediator(
                db = orbitDatabase,
                pageSize = 30,
                feedId = "feed",
                fetchPage = { limit, cursor -> service.fetchArticlesPage(limit, cursor) },
                clearOnRefresh = {
                    articleDao.clearArticles()
                    articleDao.clearCategories()
                    articleDao.clearCrossRefs()
                },
            ),
            pagingSourceFactory = {
                if (!filter.hasUserSelectedFilters && !filter.bookmarkedOnly) {
                    articleDao.pagingSource()
                } else {
                    articleDao.pagingSourceFiltered(buildFilteredArticlesQuery(filter, false))
                }
            },

        ).flow
            .map { pagingData ->
                pagingData.map { it.toDomain() }
            }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun pagedSavedArticles(filter: FeedFilter): Flow<PagingData<Article>> {
        return Pager(
            config = pagingConfig,
            remoteMediator = ArticlesRemoteMediator(
                db = orbitDatabase,
                pageSize = 30,
                feedId = "saved",
                fetchPage = { limit, cursor -> service.fetchSavedArticlesPage(limit, cursor) },
                clearOnRefresh = {},
            ),
            pagingSourceFactory = {
                if (!filter.hasUserSelectedFilters) {
                    articleDao.savedPagingSource()
                } else {
                    articleDao.pagingSourceFiltered(buildFilteredArticlesQuery(filter, true))
                }
            },
        ).flow
            .map { pagingData ->
                pagingData.map { it.toDomain() }
            }
    }

    private fun buildFilteredArticlesQuery(
        filter: FeedFilter,
        bookmarkedOnly: Boolean,
    ): SupportSQLiteQuery {
        val where = mutableListOf<String>()
        val args = mutableListOf<Any>()

        if (bookmarkedOnly) {
            where += "isBookmarked = 1"
        }

        // Category IDs filter: article must have ANY of the selected categories
        if (filter.selectedCategoryIds.isNotEmpty()) {
            val placeholders = filter.selectedCategoryIds.asDelimiterList()
            where += """
          EXISTS (
            ${DatabaseConstants.Query.SELECT_ARTICLES_BY_CATEGORY} IN ($placeholders)
          )
            """.trimIndent()
            args.addAll(filter.selectedCategoryIds.map { it as Any })
        }

        // Group filter: article must have ANY category whose group is in selected groups
        if (filter.selectedGroups.isNotEmpty()) {
            val placeholders = filter.selectedGroups.asDelimiterList()
            where += """
            EXISTS (
            ${DatabaseConstants.Query.SELECT_ARTICLES_BY_CATEGORY_GROUP} IN ($placeholders))
            """.trimIndent()

            args.addAll(filter.selectedGroups.map { it as Any })
        }

        val whereClause = if (where.isEmpty()) "" else "WHERE " + where.joinToString(" AND ")

        val sql = """
      ${DatabaseConstants.Query.SELECT_ALL_ARTICLES}
      $whereClause
      ${DatabaseConstants.Query.ORDER_BY_INGESTED_DESC}
        """.trimIndent()

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }
}
