package com.fourthwardai.orbit.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val createdTime: String,
    val title: String,
    val url: String,
    val author: String? = null,
    val readTime: Int? = null,
    val heroImageUrl: String? = null,
    val teaser: String? = null,
    val source: String,
    val sourceAvatarUrl: String? = null,
    val ingestedAt: String,
    val isBookmarked: Boolean = false,
    val isDirty: Boolean = false,
    val lastModified: Long = 0L,
)
