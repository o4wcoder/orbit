package com.fourthwardai.orbit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ArticleEntity::class, CategoryEntity::class, ArticleCategoryCrossRef::class, ArticleRemoteKeyEntity::class], version = 4, exportSchema = false)
abstract class OrbitDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao

    abstract fun articleRemoteKeyDao(): ArticleRemoteKeyDao
}
