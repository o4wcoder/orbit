package com.fourthwardai.orbit.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleDto(
    val id: String,
    val createdTime: String,
    val title: String,
    val url: String,
    val author: String,
    val readTime: Int? = null,
    val heroImageUrl: String? = null,
    val source: String,
    val ingestedAt: String,
)
