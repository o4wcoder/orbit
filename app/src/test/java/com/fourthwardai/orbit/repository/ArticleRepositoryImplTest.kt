package com.fourthwardai.orbit.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.network.ApiError
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.time.Instant

class ArticleRepositoryImplTest {

    val fakeArticleService = mockk<ArticleService>()

    private fun sampleCategory(id: String = "c1") = Category(
        id = id,
        name = "Cat",
        group = "grp",
        colorLight = androidx.compose.ui.graphics.Color.Gray,
        colorDark = androidx.compose.ui.graphics.Color.DarkGray,
    )

    private fun sampleArticle(id: String = "a1", bookmarked: Boolean = false) = Article(
        id = id,
        title = "Title",
        url = "https://example.com",
        author = "Author",
        readTimeMinutes = 5,
        heroImageUrl = null,
        teaser = "Teaser",
        source = "Source",
        sourceAvatarUrl = null,
        createdTime = "2020-01-01T00:00:00Z",
        ingestedAt = Instant.parse("2020-01-01T00:00:00Z"),
        categories = listOf(sampleCategory()),
        isBookmarked = bookmarked,
    )

    @org.junit.Test
    fun `bookmarkArticle success updates state and returns success`() = runTest {
        val articles = listOf(sampleArticle("a1", bookmarked = false), sampleArticle("a2"))

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(articles)
        coEvery { fakeArticleService.bookmarkArticle("a1", true) } returns ApiResult.Success(Unit)
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(listOf(sampleCategory()))

        val repo = ArticleRepositoryImpl(fakeArticleService)

        // pre-populate articles via refreshArticles
        repo.refreshArticles()
        assertThat(repo.articles.value.size).isEqualTo(2)
        assertThat(repo.articles.value.first { it.id == "a1" }.isBookmarked).isFalse()

        val result = repo.bookmarkArticle("a1", true)
        assertThat(result).isEqualTo(ApiResult.Success(Unit))
        assertThat(repo.articles.value.first { it.id == "a1" }.isBookmarked).isTrue()
    }

    @org.junit.Test
    fun `bookmarkArticle failure rolls back state and returns failure`() = runTest {
        val articles = listOf(sampleArticle("a1", bookmarked = false))

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(articles)
        coEvery { fakeArticleService.bookmarkArticle("a1", true) } returns ApiResult.Failure(ApiError.Network("failed"))
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(emptyList())

        val repo = ArticleRepositoryImpl(fakeArticleService)
        repo.refreshArticles()
        assertThat(repo.articles.value.first().isBookmarked).isFalse()

        val result = repo.bookmarkArticle("a1", true)
        assertThat(result).isEqualTo(ApiResult.Failure(ApiError.Network("failed")))
        // should have rolled back
        assertThat(repo.articles.value.first().isBookmarked).isFalse()
    }

    @org.junit.Test
    fun `refreshArticles success updates articles and isRefreshing flag toggles`() = runTest {
        val articles = listOf(sampleArticle("a1"))

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(articles)
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(emptyList())
        coEvery { fakeArticleService.bookmarkArticle(any(), any()) } returns ApiResult.Success(Unit)

        val repo = ArticleRepositoryImpl(fakeArticleService)
        assertThat(repo.isRefreshing.value).isFalse()
        val result = repo.refreshArticles()
        assertThat(result).isEqualTo(ApiResult.Success(Unit))
        assertThat(repo.articles.value).isEqualTo(articles)
        assertThat(repo.isRefreshing.value).isFalse()
    }

    @org.junit.Test
    fun `getCategories delegates to service`() = runTest {
        val categories = listOf(sampleCategory("c1"))

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(emptyList())
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(categories)
        coEvery { fakeArticleService.bookmarkArticle(any(), any()) } returns ApiResult.Success(Unit)

        val repo = ArticleRepositoryImpl(fakeArticleService)
        val result = repo.getCategories()
        assertThat(result).isEqualTo(ApiResult.Success(categories))
    }
}
