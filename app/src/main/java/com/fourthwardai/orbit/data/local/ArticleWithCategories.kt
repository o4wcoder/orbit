package com.fourthwardai.orbit.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ArticleWithCategories(
    @Embedded val article: ArticleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(value = ArticleCategoryCrossRef::class, parentColumn = "articleId", entityColumn = "categoryId"),
    )
    val categories: List<CategoryEntity>,
)
