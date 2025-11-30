package com.fourthwardai.orbit.di

import com.fourthwardai.orbit.BuildConfig
import com.fourthwardai.orbit.network.ktorHttpClient
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = ktorHttpClient()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideArticleService(client: HttpClient): ArticleService = ArticleService(client = client, orbitBaseUrl = BuildConfig.ARTICLES_ENDPOINT)
}
