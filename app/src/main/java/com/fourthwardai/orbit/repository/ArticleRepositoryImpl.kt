package com.fourthwardai.orbit.repository

import com.fourthwardai.orbit.data.local.ArticleDao
import com.fourthwardai.orbit.data.local.ArticleEntity
import com.fourthwardai.orbit.data.local.ArticleWithCategories
import com.fourthwardai.orbit.data.local.CategoryEntity
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.toComposeColor
import com.fourthwardai.orbit.network.ApiError
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class ArticleRepositoryImpl @Inject constructor(
    private val service: ArticleService,
    private val articleDao: ArticleDao,
) : ArticleRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _articles = MutableStateFlow<List<Article>?>(null)
    override val articles: StateFlow<List<Article>?> = _articles

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        // Observe DB and keep in-memory state in sync with cached articles
        scope.launch {
            articleDao.getAllWithCategories()
                .map { awcs -> awcs.map { it.toDomain() } }
                .collect { domainArticles ->
                    _articles.value = domainArticles
                }
        }
    }

    override suspend fun bookmarkArticle(id: String, isBookmarked: Boolean): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val article = _articles.value?.find { it.id == id } ?: return@withContext ApiResult.Failure(Exception("Article not found") as ApiError)
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

    override suspend fun refreshArticles(): ApiResult<Unit> = withContext(Dispatchers.IO) {
        _isRefreshing.value = true
        when (val result = service.fetchArticles()) {
            is ApiResult.Success -> {
                // Persist fetched articles and categories into Room (replace all in a transaction)
                try {
                    val awcs = result.data.map { article ->
                        val entity = article.toEntity()
                        val categories = article.categories.map { it.toEntity() }
                        ArticleWithCategories(article = entity, categories = categories)
                    }
                    articleDao.replaceAll(awcs)
                } catch (e: Exception) {
                    _isRefreshing.value = false
                    return@withContext ApiResult.Failure(e as ApiError)
                }

                // _articles will be updated by the DAO flow collector we started in init
                _isRefreshing.value = false
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> {
                _isRefreshing.value = false
                ApiResult.Failure(result.error)
            }
        }
    }

    override suspend fun getCategories(): ApiResult<List<Category>> = withContext(Dispatchers.IO) {
        service.fetchArticleCategories()
    }
}

// Mapping helpers between Room entities and domain models
private fun ArticleWithCategories.toDomain(): Article {
    val articleEntity = this.article
    return Article(
        id = articleEntity.id,
        title = articleEntity.title,
        url = articleEntity.url,
        author = articleEntity.author,
        readTimeMinutes = articleEntity.readTime,
        heroImageUrl = articleEntity.heroImageUrl,
        teaser = articleEntity.teaser,
        source = articleEntity.source,
        sourceAvatarUrl = articleEntity.sourceAvatarUrl,
        createdTime = articleEntity.createdTime,
        ingestedAt = Instant.parse(articleEntity.ingestedAt),
        categories = this.categories.map { it.toDomain() },
        isBookmarked = articleEntity.isBookmarked,
    )
}

private fun Article.toEntity(): ArticleEntity = ArticleEntity(
    id = id,
    createdTime = createdTime,
    title = title,
    url = url,
    author = author,
    readTime = readTimeMinutes,
    heroImageUrl = heroImageUrl,
    teaser = teaser,
    source = source,
    sourceAvatarUrl = sourceAvatarUrl,
    ingestedAt = ingestedAt.toString(),
    isBookmarked = isBookmarked,
)

private fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    group = group,
    colorLight = colorLight.toComposeColor(),
    colorDark = colorDark.toComposeColor(),
)

private fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    group = group,
    colorLight = colorLight.toString(),
    colorDark = colorDark.toString(),
)
