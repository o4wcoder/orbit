package com.fourthwardai.orbit.domain

import androidx.compose.ui.graphics.Color
import com.fourthwardai.orbit.network.dto.ArticleDto
import java.time.Instant
import java.time.ZoneId

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
    val ingestedAt: Instant,
    val categories: List<Category>,
    val isBookmarked: Boolean,
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
        ingestedAt = Instant.parse(ingestedAt),
        categories = categories.map { category ->
            Category(
                id = category.id,
                name = category.name,
                group = category.group,
                colorLight = category.colorLight.toComposeColor(),
                colorDark = category.colorDark.toComposeColor(),
            )
        },
        isBookmarked = isBookmarked == true,
    )

fun String.toComposeColor(): Color {
    val s = trim().removePrefix("#")

    // Accept RRGGBB or AARRGGBB
    if (s.length != 6 && s.length != 8) return Color.Gray

    return try {
        val value = s.toLong(16)

        // Build ARGB as a 32-bit int
        val argb: Int = if (s.length == 6) {
            // Implicit alpha = FF
            (0xFF000000.toInt() or value.toInt())
        } else {
            value.toInt()
        }

        Color(argb)
    } catch (_: Throwable) {
        Color.Gray
    }
}

fun Article.shuffleKey(): Int {
    val localDate = ingestedAt.atZone(ZoneId.of("UTC")).toLocalDate()
    val dayKey = localDate.toEpochDay()

    val hashInput = "$dayKey|$source|$title|$url"
    return hashInput.hashCode()
}
