package com.fourthwardai.orbit.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.fourthwardai.orbit.data.local.ArticleRemoteKeyEntity
import com.fourthwardai.orbit.data.local.ArticleWithCategories
import com.fourthwardai.orbit.data.local.OrbitDatabase
import com.fourthwardai.orbit.data.local.toEntity
import com.fourthwardai.orbit.domain.ArticlesPageCursor
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.network.toThrowable
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import java.time.Instant

@OptIn(ExperimentalPagingApi::class)
class ArticlesRemoteMediator(
    private val db: OrbitDatabase,
    private val service: ArticleService,
    private val pageSize: Int,
) : RemoteMediator<Int, ArticleWithCategories>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ArticleWithCategories>,
    ): MediatorResult {
        try {
            val keyDao = db.articleRemoteKeyDao()
            val articleDao = db.articleDao()

            val cursor: ArticlesPageCursor? = when (loadType) {
                LoadType.REFRESH -> null

                LoadType.APPEND -> {
                    val key = keyDao.get()
                    val beforeAt = key?.nextBeforeIngestedAt
                    val beforeId = key?.nextBeforeId
                    if (beforeAt == null || beforeId == null) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    ArticlesPageCursor(
                        beforeIngestedAt = Instant.ofEpochMilli(beforeAt),
                        beforeId = beforeId,
                    )
                }

                LoadType.PREPEND -> {
                    // Feed is newest-first; we never prepend older items upward.
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            val limit = state.config.pageSize.coerceAtLeast(pageSize)
            val pageResult = service.fetchArticlesPage(limit = limit, cursor = cursor)

            val page = when (pageResult) {
                is ApiResult.Success -> pageResult.data
                is ApiResult.Failure -> return MediatorResult.Error(pageResult.error.toThrowable())
            }
            val entities = page.articles.map { it.toEntity() }

            db.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    keyDao.clear()
                    // If you have cross tables (article_categories), clear those too.
                    articleDao.clearArticles()
                }

                articleDao.insertAll(entities)

                if (page.articles.isNotEmpty()) {
                    val next = page.nextCursor
                    keyDao.upsert(
                        ArticleRemoteKeyEntity(
                            feedId = "main",
                            nextBeforeIngestedAt = next?.beforeIngestedAt?.toEpochMilli(),
                            nextBeforeId = next?.beforeId,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
//            Timber.d("MEDIATOR loadType=%s", loadType)
//
//            Timber.d("MEDIATOR cursor=%s/%s", cursor?.beforeIngestedAt, cursor?.beforeId)
//
//            Timber.d("MEDIATOR fetched=%d limit=%d next=%s/%s",
//                page.articles.size,
//                limit,
//                page.nextCursor?.beforeIngestedAt,
//                page.nextCursor?.beforeId
//            )
            val endReached = page.articles.isEmpty() || page.articles.size < limit
            return MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (t: Throwable) {
            return MediatorResult.Error(t)
        }
    }
}
