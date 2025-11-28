package com.fourthwardai.orbit.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleDto(
    val id: String,
    val createdTime: String,
    val title: String,
    val url: String,
    val author: String? = null,
    val readTime: Int? = null,
    val heroImageUrl: String? = null,
    val teaser: String? = null,
    val source: String,
    val ingestedAt: String,
    val categories: List<CategoryDto>,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val colorLight: String,
    val colorDark: String,
)
