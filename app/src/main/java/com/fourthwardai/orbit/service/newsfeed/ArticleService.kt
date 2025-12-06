package com.fourthwardai.orbit.service.newsfeed

import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.toDomain
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.network.dto.ArticleDto
import com.fourthwardai.orbit.network.dto.CategoryDto
import com.fourthwardai.orbit.network.toApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class ArticleService(
    private val client: HttpClient,
    private val orbitBaseUrl: String,
) {
    suspend fun fetchArticles(): ApiResult<List<Article>> = try {
        val dtos: List<ArticleDto> = client.get("$orbitBaseUrl${OrbitEndpoints.ARTICLE_FEED}").body()
        ApiResult.Success(dtos.map { it.toDomain() })
    } catch (e: Exception) {
        ApiResult.Failure(e.toApiError())
    }

    suspend fun fetchArticleCategories(): ApiResult<List<Category>> = try {
        val dtos: List<CategoryDto> = client.get("$orbitBaseUrl${OrbitEndpoints.ARTICLE_CATEGORIES}").body()
        ApiResult.Success(dtos.map { it.toDomain() })
    } catch (e: Exception) {
        ApiResult.Failure(e.toApiError())
    }
}
