package com.fourthwardai.orbit.domain

import com.fourthwardai.orbit.network.dto.ArticleDto

data class Article(
    val id: String,
    val title: String,
    val url: String,
    val author: String?,
    val readTimeMinutes: Int?,
    val heroImageUrl: String?,
    val teaser: String?,
    val source: String,
    val createdTime: String,
    val ingestedAt: String,
)

fun ArticleDto.toDomain(): Article =
    Article(
        id = id,
        title = title,
        url = url,
        author = author,
        readTimeMinutes = readTime,
        heroImageUrl = heroImageUrl,
        teaser = teaser,
        source = source,
        createdTime = createdTime,
        ingestedAt = ingestedAt,
    )
