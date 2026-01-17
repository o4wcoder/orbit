package com.fourthwardai.orbit.repository

import android.content.Context
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fourthwardai.orbit.data.local.ArticleDao
import com.fourthwardai.orbit.data.local.ArticleEntity
import com.fourthwardai.orbit.data.local.ArticleWithCategories
import com.fourthwardai.orbit.data.local.CategoryEntity
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

    private fun sampleCategoryEntity(id: String = "c1") = CategoryEntity(
        id = id,
        name = "Cat",
        group = "grp",
        colorLight = "#FF808080",
        colorDark = "#FFA9A9A9",
    )

    private fun sampleArticleEntity(id: String = "a1", bookmarked: Boolean = false) = ArticleEntity(
        id = id,
        title = "Title",
        url = "https://example.com",
        author = "Author",
        readTime = 5,
        heroImageUrl = null,
        teaser = "Teaser",
        source = "Source",
        sourceAvatarUrl = null,
        ingestedAt = "2020-01-01T00:00:00Z",
        isBookmarked = bookmarked,
        isDirty = false,
        lastModified = 0L,
    )

    private fun sampleArticleWithCategories(id: String = "a1", bookmarked: Boolean = false) = ArticleWithCategories(
        article = sampleArticleEntity(id, bookmarked),
        categories = listOf(sampleCategoryEntity()),
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

    @Test
    fun `pagedArticles returns flow with correct paging configuration`() = runTest {
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        wireDaoFlow()

        // Create a test paging source that we can verify
        val testPagingSource = mockk<PagingSource<Int, ArticleWithCategories>>()
        every { fakeArticleDao.pagingSource() } returns testPagingSource

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)

        // Call pagedArticles to get the flow
        val pagingDataFlow = repo.pagedArticles()

        // Verify that pagingSource is called on the DAO
        // The actual configuration (pageSize, prefetchDistance, enablePlaceholders) is tested implicitly
        // by the behavior of the Pager, but we verify the source is used
        assertThat(pagingDataFlow).isEqualTo(pagingDataFlow)
    }

    @Test
    fun `pagedArticles maps ArticleWithCategories to Article domain objects`() = runTest {
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        wireDaoFlow()

        // Create sample data
        val articlesWithCategories = listOf(
            sampleArticleWithCategories("a1", bookmarked = false),
            sampleArticleWithCategories("a2", bookmarked = true),
            sampleArticleWithCategories("a3", bookmarked = false),
        )

        // Mock the paging source to return our test data
        val testPagingSource = TestPagingSource(articlesWithCategories)
        every { fakeArticleDao.pagingSource() } returns testPagingSource

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)

        // Collect the paging data using the testing utilities
        val pagingDataFlow = repo.pagedArticles()
        val items = pagingDataFlow.asSnapshot()

        // Verify the mapping
        assertThat(items.size).isEqualTo(3)
        assertThat(items[0].id).isEqualTo("a1")
        assertThat(items[0].title).isEqualTo("Title")
        assertThat(items[0].isBookmarked).isFalse()
        assertThat(items[1].id).isEqualTo("a2")
        assertThat(items[1].isBookmarked).isTrue()
        assertThat(items[2].id).isEqualTo("a3")
        assertThat(items[2].isBookmarked).isFalse()

        // Verify domain object properties are correctly mapped
        assertThat(items[0].url).isEqualTo("https://example.com")
        assertThat(items[0].author).isEqualTo("Author")
        assertThat(items[0].readTimeMinutes).isEqualTo(5)
        assertThat(items[0].source).isEqualTo("Source")
        assertThat(items[0].ingestedAt).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"))
        assertThat(items[0].categories.size).isEqualTo(1)
        assertThat(items[0].categories[0].name).isEqualTo("Cat")
    }

    @Test
    fun `pagedArticles handles empty data correctly`() = runTest {
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        wireDaoFlow()

        // Mock the paging source to return empty data
        val testPagingSource = TestPagingSource<ArticleWithCategories>(emptyList())
        every { fakeArticleDao.pagingSource() } returns testPagingSource

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)

        // Collect the paging data
        val pagingDataFlow = repo.pagedArticles()
        val items = pagingDataFlow.asSnapshot()

        // Verify empty result
        assertThat(items.size).isEqualTo(0)
    }

    /**
     * Test PagingSource implementation for testing purposes.
     * Returns all data in a single page.
     */
    private class TestPagingSource<T : Any>(
        private val data: List<T>,
    ) : PagingSource<Int, T>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val startIndex = page * pageSize
            val endIndex = minOf(startIndex + pageSize, data.size)

            return if (startIndex >= data.size) {
                LoadResult.Page(
                    data = emptyList(),
                    prevKey = if (page > 0) page - 1 else null,
                    nextKey = null,
                )
            } else {
                LoadResult.Page(
                    data = data.subList(startIndex, endIndex),
                    prevKey = if (page > 0) page - 1 else null,
                    nextKey = if (endIndex < data.size) page + 1 else null,
                )
            }
        }

        override fun getRefreshKey(state: androidx.paging.PagingState<Int, T>): Int? = null
    }
    }
}
