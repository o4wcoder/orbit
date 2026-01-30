package com.fourthwardai.orbit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArticleRemoteKeyDao {
    @Query("SELECT * FROM article_remote_keys WHERE feedId = :feedId LIMIT 1")
    suspend fun get(feedId: String = "main"): ArticleRemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(key: ArticleRemoteKeyEntity)

    @Query("DELETE FROM article_remote_keys WHERE feedId = :feedId")
    suspend fun clear(feedId: String = "main")
}
