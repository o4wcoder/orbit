package com.fourthwardai.orbit.data.local

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
    colorLight = colorLight.toString(),
    colorDark = colorDark.toString(),
)
