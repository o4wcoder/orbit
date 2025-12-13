package com.fourthwardai.orbit.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "orbit-db"
        ).build()
    }

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()
}

