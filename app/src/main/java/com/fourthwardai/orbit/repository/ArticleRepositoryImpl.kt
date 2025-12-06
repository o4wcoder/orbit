package com.fourthwardai.orbit.repository

import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.shuffleKey
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZoneOffset
import javax.inject.Inject

class ArticleRepositoryImpl @Inject constructor(
    private val service: ArticleService,
) : ArticleRepository {

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    override val articles: StateFlow<List<Article>> = _articles

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing

    override suspend fun refreshArticles(): ApiResult<Unit> {
        _isRefreshing.value = true
        return when (val result = service.fetchArticles()) {
            is ApiResult.Success -> {
                _articles.value = result.data
                    .sortedWith(
                        compareByDescending<Article> {
                            // group by calendar day
                            it.ingestedAt.atZone(ZoneOffset.UTC).toLocalDate()
                        }.thenBy {
                            // randomized but stable within the day
                            it.shuffleKey()
                        },
                    )

                _isRefreshing.value = false
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> {
                _isRefreshing.value = false
                ApiResult.Failure(result.error)
            }
        }
    }

    override suspend fun getCategories(): ApiResult<List<Category>> {
        return service.fetchArticleCategories()
    }
}
