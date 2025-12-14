package com.fourthwardai.orbit.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fourthwardai.orbit.data.local.ArticleDao
import com.fourthwardai.orbit.data.local.ArticleWithCategories
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.network.ApiError
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleRepositoryImplTest {

    val fakeArticleService = mockk<ArticleService>()
    val fakeArticleDao = mockk<ArticleDao>()

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

    @Suppress("UNCHECKED_CAST")
    private fun wireDaoFlow(): MutableStateFlow<List<ArticleWithCategories>> {
        val store = MutableStateFlow<List<ArticleWithCategories>>(emptyList())
        every { fakeArticleDao.getAllWithCategories() } returns store
        coEvery { fakeArticleDao.replaceAll(any()) } answers {
            val arg = args[0] as List<ArticleWithCategories>
            store.value = arg
        }
        return store
    }

    @Test
    fun `bookmarkArticle success updates state and returns success`() = runTest {
        val articles = listOf(sampleArticle("a1", bookmarked = false), sampleArticle("a2"))

        wireDaoFlow()

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(articles)
        coEvery { fakeArticleService.bookmarkArticle("a1", true) } returns ApiResult.Success(Unit)
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(listOf(sampleCategory()))

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        // pre-populate articles via refreshArticles
        val refreshResult = repo.refreshArticles()
        // allow background collector on the repository's scope to process the DAO flow
        advanceUntilIdle()
        assertThat(refreshResult).isEqualTo(ApiResult.Success(Unit))
        assertThat(repo.articles.value!!.size).isEqualTo(2)
        assertThat(repo.articles.value!!.first { it.id == "a1" }.isBookmarked).isFalse()

        val result = repo.bookmarkArticle("a1", true)
        // allow IO dispatcher work (bookmark network call & potential rollback) to run
        advanceUntilIdle()
        assertThat(result).isEqualTo(ApiResult.Success(Unit))
        assertThat(repo.articles.value!!.first { it.id == "a1" }.isBookmarked).isTrue()
    }

    @Test
    fun `bookmarkArticle failure rolls back state and returns failure`() = runTest {
        val articles = listOf(sampleArticle("a1", bookmarked = false))

        wireDaoFlow()

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(articles)
        coEvery { fakeArticleService.bookmarkArticle("a1", true) } returns ApiResult.Failure(ApiError.Network("failed"))
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(emptyList())

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher)
        val refreshResult = repo.refreshArticles()
        // allow background collector on the repository's scope to process the DAO flow
        advanceUntilIdle()
        assertThat(refreshResult).isEqualTo(ApiResult.Success(Unit))
        assertThat(repo.articles.value!!.first().isBookmarked).isFalse()

        val result = repo.bookmarkArticle("a1", true)
        // allow IO dispatcher work (bookmark network call & potential rollback) to run
        advanceUntilIdle()
        assertThat(result).isEqualTo(ApiResult.Failure(ApiError.Network("failed")))
        // should have rolled back
        assertThat(repo.articles.value!!.first().isBookmarked).isFalse()
    }

    @Test
    fun `refreshArticles success updates articles`() = runTest {
        val articles = listOf(sampleArticle("a1"))

        wireDaoFlow()

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(articles)
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(emptyList())
        coEvery { fakeArticleService.bookmarkArticle(any(), any()) } returns ApiResult.Success(Unit)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher)
        val result = repo.refreshArticles()
        // allow background collector on the repository's scope to process the DAO flow
        advanceUntilIdle()
        assertThat(result).isEqualTo(ApiResult.Success(Unit))
        assertThat(repo.articles.value).isEqualTo(articles)
    }

    @Test
    fun `getCategories delegates to service`() = runTest {
        val categories = listOf(sampleCategory("c1"))

        wireDaoFlow()

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(emptyList())
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(categories)
        coEvery { fakeArticleService.bookmarkArticle(any(), any()) } returns ApiResult.Success(Unit)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher)
        val result = repo.getCategories()
        assertThat(result).isEqualTo(ApiResult.Success(categories))
    }
}
