package com.fourthwardai.orbit.data.local

import androidx.compose.ui.graphics.toArgb
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.toComposeColor
import java.time.Instant

// Mappers between Room entities and domain models

fun ArticleWithCategories.toDomain(): Article {
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

fun Article.toEntity(): ArticleEntity = ArticleEntity(
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
    // Articles mapped from domain/network are not dirty by default
    isDirty = false,
    lastModified = 0L,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    group = group,
    colorLight = colorLight.toComposeColor(),
    colorDark = colorDark.toComposeColor(),
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    group = group,
    // Persist as hex strings (include alpha) so we can parse them back with toComposeColor()
    colorLight = String.format("#%08X", colorLight.toArgb()),
    colorDark = String.format("#%08X", colorDark.toArgb()),
)
