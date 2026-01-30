package com.fourthwardai.orbit.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "article_remote_keys")
data class ArticleRemoteKeyEntity(
    @PrimaryKey val feedId: String = "main",
    val nextBeforeIngestedAt: Long?,
    val nextBeforeId: String?,
    val updatedAt: Long,
)
