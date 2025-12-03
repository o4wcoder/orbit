package com.fourthwardai.orbit.domain

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
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
    val sourceAvatarUrl: String?,
    val createdTime: String,
    val ingestedAt: String,
    val categories: List<Category>,
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
        sourceAvatarUrl = sourceAvatarUrl,
        createdTime = createdTime,
        ingestedAt = ingestedAt,
        categories = categories.map { category ->
            Category(
                id = category.id,
                name = category.name,
                group = category.group,
                colorLight = category.colorLight.toComposeColor(),
                colorDark = category.colorDark.toComposeColor(),
            )
        },
    )

fun String.toComposeColor(): Color {
    return try {
        val formattedHexString = if (this.startsWith("#")) this else "#$this"
        Color(formattedHexString.toColorInt())
    } catch (e: IllegalArgumentException) {
        Color.Gray
    }
}
