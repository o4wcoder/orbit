package com.fourthwardai.orbit.data.local

import androidx.room.Entity

@Entity(primaryKeys = ["articleId", "categoryId"], tableName = "article_category_cross_ref")
data class ArticleCategoryCrossRef(
    val articleId: String,
    val categoryId: String,
)
