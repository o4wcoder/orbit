package com.fourthwardai.orbit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getAll(): Flow<List<ArticleEntity>>

    @Insert
    suspend fun insert(article: ArticleEntity): Long

    @Query("DELETE FROM articles")
    suspend fun clear()
}

