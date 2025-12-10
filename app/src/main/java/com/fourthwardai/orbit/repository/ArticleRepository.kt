package com.fourthwardai.orbit.repository

import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.network.ApiResult
import kotlinx.coroutines.flow.StateFlow

interface ArticleRepository {

    /**
     * All articles currently loaded in memory.
     * ViewModels will filter this based on FeedFilter.
     */
    val articles: StateFlow<List<Article>>

    /**
     * Whether a refresh is in progress.
     */
    val isRefreshing: StateFlow<Boolean>

    /**
     * Update the bookmark status of an article.
     */
    suspend fun bookmarkArticle(id: String, isBookmarked: Boolean)

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

    // 🔮 Future (when you add server-side filtering/paging):
    // suspend fun fetchArticlesPage(query: ArticleQuery): ApiResult<ArticlePage>
}
