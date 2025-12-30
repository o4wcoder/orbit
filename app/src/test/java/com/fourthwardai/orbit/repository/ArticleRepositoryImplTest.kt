package com.fourthwardai.orbit.repository

import android.content.Context
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fourthwardai.orbit.data.local.ArticleDao
import com.fourthwardai.orbit.data.local.ArticleEntity
import com.fourthwardai.orbit.data.local.ArticleWithCategories
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.network.ApiError
import com.fourthwardai.orbit.network.ApiResult
import com.fourthwardai.orbit.service.newsfeed.ArticleService
import com.fourthwardai.orbit.work.scheduleArticleSync
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
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
    val fakeContext = mockk<Context>(relaxed = true)

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
        // Allow inserts during tests without side-effects
        coEvery { fakeArticleDao.insert(any()) } just Runs
        return store
    }

    @Test
    fun `bookmarkArticle success updates state and returns success`() = runTest {
        // Prevent actual WorkManager scheduling during unit tests - mock the generated Kotlin file class
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        val articles = listOf(sampleArticle("a1", bookmarked = false), sampleArticle("a2"))

        wireDaoFlow()

        // Mock getById to return a corresponding ArticleEntity for bookmark handling
        coEvery { fakeArticleDao.getById("a1") } returns ArticleEntity(
            id = "a1",
            title = articles[0].title,
            url = articles[0].url,
            author = articles[0].author,
            readTime = articles[0].readTimeMinutes,
            heroImageUrl = articles[0].heroImageUrl,
            teaser = articles[0].teaser,
            source = articles[0].source,
            sourceAvatarUrl = articles[0].sourceAvatarUrl,
            ingestedAt = articles[0].ingestedAt.toString(),
            isBookmarked = false,
            isDirty = false,
            lastModified = 0L,
        )

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(articles)
        coEvery { fakeArticleService.bookmarkArticle("a1", true) } returns ApiResult.Success(Unit)
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(listOf(sampleCategory()))

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)
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
        // Prevent actual WorkManager scheduling during unit tests
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        val articles = listOf(sampleArticle("a1", bookmarked = false))

        wireDaoFlow()

        // Mock getById to return a corresponding ArticleEntity for bookmark handling
        coEvery { fakeArticleDao.getById("a1") } returns ArticleEntity(
            id = "a1",
            title = articles[0].title,
            url = articles[0].url,
            author = articles[0].author,
            readTime = articles[0].readTimeMinutes,
            heroImageUrl = articles[0].heroImageUrl,
            teaser = articles[0].teaser,
            source = articles[0].source,
            sourceAvatarUrl = articles[0].sourceAvatarUrl,
            ingestedAt = articles[0].ingestedAt.toString(),
            isBookmarked = false,
            isDirty = false,
            lastModified = 0L,
        )

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(articles)
        coEvery { fakeArticleService.bookmarkArticle("a1", true) } returns ApiResult.Failure(ApiError.Network("failed"))
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(emptyList())

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)
        val refreshResult = repo.refreshArticles()
        // allow background collector on the repository's scope to process the DAO flow
        advanceUntilIdle()
        assertThat(refreshResult).isEqualTo(ApiResult.Success(Unit))
        assertThat(repo.articles.value!!.first().isBookmarked).isFalse()

        val result = repo.bookmarkArticle("a1", true)
        // allow IO dispatcher work (bookmark network call & potential rollback) to run
        advanceUntilIdle()
        assertThat(result).isEqualTo(ApiResult.Failure(ApiError.Network("failed")))
        // For transient errors we keep optimistic local change and schedule background retry
        assertThat(repo.articles.value!!.first().isBookmarked).isTrue()
    }

    @Test
    fun `refreshArticles success updates articles`() = runTest {
        // Prevent actual WorkManager scheduling during unit tests
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        val articles = listOf(sampleArticle("a1"))

        wireDaoFlow()

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(articles)
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(emptyList())
        coEvery { fakeArticleService.bookmarkArticle(any(), any()) } returns ApiResult.Success(Unit)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)
        val result = repo.refreshArticles()
        // allow background collector on the repository's scope to process the DAO flow
        advanceUntilIdle()
        assertThat(result).isEqualTo(ApiResult.Success(Unit))
        assertThat(repo.articles.value).isEqualTo(articles)
    }

    @Test
    fun `getCategories delegates to service`() = runTest {
        // Prevent actual WorkManager scheduling during unit tests
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        val categories = listOf(sampleCategory("c1"))

        wireDaoFlow()

        coEvery { fakeArticleService.fetchArticles() } returns ApiResult.Success(emptyList())
        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(categories)
        coEvery { fakeArticleService.bookmarkArticle(any(), any()) } returns ApiResult.Success(Unit)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)
        val result = repo.getCategories()
        assertThat(result).isEqualTo(ApiResult.Success(categories))
    }

    @Test
    fun `syncDirtyArticles returns success when no dirty articles`() = runTest {
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        wireDaoFlow()
        coEvery { fakeArticleDao.getDirtyArticles() } returns emptyList()

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)

        val result = repo.syncDirtyArticles()
        assertThat(result).isEqualTo(ApiResult.Success(Unit))
    }

    @Test
    fun `syncDirtyArticles retries on transient failure and returns failure`() = runTest {
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        wireDaoFlow()

        val entity = ArticleEntity(
            id = "a1",
            title = "T",
            url = "u",
            author = "author",
            readTime = 1,
            heroImageUrl = null,
            teaser = null,
            source = "src",
            sourceAvatarUrl = null,
            ingestedAt = "2020-01-01T00:00:00Z",
            isBookmarked = true,
            isDirty = true,
            lastModified = 0L,
        )

        coEvery { fakeArticleDao.getDirtyArticles() } returns listOf(entity)
        coEvery { fakeArticleService.bookmarkArticle(entity.id, entity.isBookmarked) } returns ApiResult.Failure(ApiError.Network("net"))

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)

        val result = repo.syncDirtyArticles()
        assertThat(result).isEqualTo(ApiResult.Failure(ApiError.Network("net")))

        // transient failure: repository should not mark the item as not-dirty
        coVerify(exactly = 0) { fakeArticleDao.insert(match { it.id == entity.id && it.isDirty == false }) }
    }

    @Test
    fun `syncDirtyArticles marks permanent failures as not dirty and returns success`() = runTest {
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        wireDaoFlow()

        val entity = ArticleEntity(
            id = "a2",
            title = "T2",
            url = "u2",
            author = "author2",
            readTime = 2,
            heroImageUrl = null,
            teaser = null,
            source = "src2",
            sourceAvatarUrl = null,
            ingestedAt = "2020-01-01T00:00:00Z",
            isBookmarked = false,
            isDirty = true,
            lastModified = 0L,
        )

        coEvery { fakeArticleDao.getDirtyArticles() } returns listOf(entity)
        coEvery { fakeArticleService.bookmarkArticle(entity.id, entity.isBookmarked) } returns ApiResult.Failure(ApiError.Http(400, "bad"))

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)

        val result = repo.syncDirtyArticles()
        assertThat(result).isEqualTo(ApiResult.Success(Unit))

        // permanent failure should mark the article as not dirty
        coVerify(exactly = 1) { fakeArticleDao.insert(match { it.id == entity.id && it.isDirty == false }) }
    }
}
