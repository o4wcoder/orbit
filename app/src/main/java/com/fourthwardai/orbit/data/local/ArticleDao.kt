package com.fourthwardai.orbit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Transaction
    @Query("SELECT * FROM articles ORDER BY ingestedAt DESC")
    fun getAllWithCategories(): Flow<List<ArticleWithCategories>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: ArticleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: ArticleCategoryCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<ArticleCategoryCrossRef>)

    @Query("DELETE FROM article_category_cross_ref")
    suspend fun clearCrossRefs()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM articles")
    suspend fun clearArticles()

    @Transaction
    suspend fun replaceAll(articlesWithCategories: List<ArticleWithCategories>) {
        // Clear all related tables and insert fresh data atomically
        clearCrossRefs()
        clearCategories()
        clearArticles()

        val articles = articlesWithCategories.map { it.article }
        val categories = articlesWithCategories.flatMap { it.categories }.distinctBy { it.id }
        val crossRefs = articlesWithCategories.flatMap { awc -> awc.categories.map { cat -> ArticleCategoryCrossRef(awc.article.id, cat.id) } }

        insertAll(articles)
        insertCategories(categories)
        insertCrossRefs(crossRefs)
    }

    @Query("SELECT * FROM articles WHERE isDirty = 1")
    suspend fun getDirtyArticles(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ArticleEntity?
}
