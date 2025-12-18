package com.fourthwardai.orbit.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fourthwardai.orbit.data.local.AppDatabase
import com.fourthwardai.orbit.data.local.ArticleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Migration from version 2 -> 3: add isDirty (INTEGER) and lastModified (INTEGER) columns to articles
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Be defensive: only add columns if they don't already exist. This avoids
            // "duplicate column name" errors when the DB schema already contains the
            // column (e.g. from a previous dev run or manual change).
            fun hasColumn(table: String, column: String): Boolean {
                val cursor = db.query("PRAGMA table_info($table)")
                cursor.use {
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == column) return true
                    }
                }
                return false
            }

            if (!hasColumn("articles", "isDirty")) {
                db.execSQL("ALTER TABLE articles ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
            }
            if (!hasColumn("articles", "lastModified")) {
                db.execSQL("ALTER TABLE articles ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "orbit-db",
        )
            // Add explicit migrations for schema changes so user data is preserved.
            .addMigrations(MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()
}
