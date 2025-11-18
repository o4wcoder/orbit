package com.fourthwardai.orbit.service.newsfeed

import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.toDomain
import com.fourthwardai.orbit.network.dto.ArticleDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class ArticleService(
    private val client: HttpClient,
    private val feedUrl: String,
) {
    suspend fun fetchArticles(): List<Article> {
        val dtos: List<ArticleDto> = client.get(feedUrl).body()
        return dtos.map { it.toDomain() }
    }
}
