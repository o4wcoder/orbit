package com.fourthwardai.orbit.repository

import androidx.paging.PagingData
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ArticleRepository {

    /**
     * All articles currently loaded in memory.
     * ViewModels will filter this based on FeedFilter.
     */
    val articles: StateFlow<List<Article>?>

    /**
     * Update the bookmark status of an article.
     */
    suspend fun bookmarkArticle(id: String, isBookmarked: Boolean): ApiResult<Unit>

    /**
     * One-shot refresh from remote (n8n/Airtable).
     * For now: fetch *all* articles and store in [articles].
     */
    suspend fun refreshArticles(): ApiResult<Unit>

    /**
     * Get categories. For now just pass through to ArticleService.
     * In the future, you might cache these or move them to a CategoryRepository.
     */
    suspend fun getCategories(): ApiResult<List<Category>>

    /**
     * Sync any locally-dirty articles (e.g. bookmarks) with the server.
     * Implementations should return Success when all dirty items are synced,
     * or Failure when any transient error occurs (worker can retry).
     */
    suspend fun syncDirtyArticles(): ApiResult<Unit>

    /**
     * Returns a cold Flow of paged articles backed by Room.
     *
     * The Flow does not execute any queries until collected.
     * Paging controls when and how data is loaded.
     * Filtering is applied at the database query level for optimal performance.
     *
     * @param filter The filter criteria to apply at the database level
     */
    fun pagedArticles(filter: FeedFilter): Flow<PagingData<Article>>

    // 🔮 Future (when you add server-side filtering/paging):
    // suspend fun fetchArticlesPage(query: ArticleQuery): ApiResult<ArticlePage>
}
