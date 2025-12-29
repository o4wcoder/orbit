package com.fourthwardai.orbit.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArticleDto(
    val id: String,
    val title: String,
    val url: String,
    val author: String? = null,
    @SerialName("read_time")
    val readTime: Int? = null,
    @SerialName("hero_image_url")
    val heroImageUrl: String? = null,
    val teaser: String? = null,
    val source: String,
    @SerialName("source_avatar_url")
    val sourceAvatarUrl: String? = null,
    @SerialName("ingested_at")
    val ingestedAt: String,
    val categories: List<CategoryDto> = emptyList(),
    @SerialName("is_bookmarked")
    val isBookmarked: Boolean? = null,
)
