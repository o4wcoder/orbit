package com.fourthwardai.orbit.repository

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.fourthwardai.orbit.data.local.ArticleDao
import com.fourthwardai.orbit.data.local.ArticleEntity
import com.fourthwardai.orbit.data.local.ArticleRemoteKeyDao
import com.fourthwardai.orbit.data.local.ArticleWithCategories
import com.fourthwardai.orbit.data.local.CategoryEntity
import com.fourthwardai.orbit.data.local.OrbitDatabase
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
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
    val fakeOrbitDatabase = mockk<OrbitDatabase>()
    val fakeContext = mockk<Context>(relaxed = true)
    val fakeArticleRemoteKeyDao = mockk<ArticleRemoteKeyDao>()

    private val defaultFilter = FeedFilter()

    private fun sampleCategory(id: String = "c1") = Category(
        id = id,
        name = "Cat",
        group = "grp",
        colorLight = androidx.compose.ui.graphics.Color(0xFF808080.toInt()),
        colorDark = androidx.compose.ui.graphics.Color(0xFFA9A9A9.toInt()),
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
        ingestedAt = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli(),
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
        every { fakeOrbitDatabase.articleDao() } returns fakeArticleDao
        every { fakeOrbitDatabase.articleRemoteKeyDao() } returns fakeArticleRemoteKeyDao
        coEvery { fakeArticleRemoteKeyDao.get(any()) } returns null
        coEvery { fakeArticleRemoteKeyDao.clear(any()) } just Runs
        coEvery { fakeArticleRemoteKeyDao.upsert(any()) } just Runs
        every { fakeArticleDao.getAllWithCategories() } returns store
        coEvery { fakeArticleDao.replaceAll(any()) } answers {
            val arg = args[0] as List<ArticleWithCategories>
            store.value = arg
        }
        // Allow inserts during tests without side-effects
        coEvery { fakeArticleDao.insert(any()) } just Runs
        coEvery { fakeArticleDao.insertAll(any()) } just Runs
        coEvery { fakeArticleDao.insertCategories(any()) } just Runs
        coEvery { fakeArticleDao.insertCrossRefs(any()) } just Runs
        coEvery { fakeArticleDao.clearArticles() } just Runs
        coEvery { fakeArticleDao.clearCategories() } just Runs
        coEvery { fakeArticleDao.clearCrossRefs() } just Runs
        return store
    }

    @Test
    fun `bookmarkArticle success updates state and returns success`() = runTest {
        // Prevent actual WorkManager scheduling during unit tests - mock the generated Kotlin file class
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        val articles = listOf(sampleArticle("a1", bookmarked = false), sampleArticle("a2"))

        val store = wireDaoFlow()
        store.value = articles.map { article ->
            ArticleWithCategories(
                article = ArticleEntity(
                    id = article.id,
                    title = article.title,
                    url = article.url,
                    author = article.author,
                    readTime = article.readTimeMinutes,
                    heroImageUrl = article.heroImageUrl,
                    teaser = article.teaser,
                    source = article.source,
                    sourceAvatarUrl = article.sourceAvatarUrl,
                    ingestedAt = article.ingestedAt.toEpochMilli(),
                    isBookmarked = article.isBookmarked,
                    isDirty = false,
                    lastModified = 0L,
                ),
                categories = listOf(sampleCategoryEntity()),
            )
        }

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
            ingestedAt = articles[0].ingestedAt.toEpochMilli(),
            isBookmarked = false,
            isDirty = false,
            lastModified = 0L,
        )
        // syncDirtyArticles() is called on success; avoid unmocked DAO calls
        coEvery { fakeArticleDao.getDirtyArticles() } returns emptyList()

        coEvery { fakeArticleService.bookmarkArticle("a1", true) } returns ApiResult.Success(Unit)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, fakeOrbitDatabase, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)
        advanceUntilIdle()

        // precondition: articles loaded from DAO
        assertThat(repo.articles.value).isNotNull()
        assertThat(repo.articles.value!!.size).isEqualTo(2)

        val result = repo.bookmarkArticle("a1", true)
        advanceUntilIdle()
        assertThat(result).isEqualTo(ApiResult.Success(Unit))
        val updated = repo.articles.value!!.first { it.id == "a1" }
        assertThat(updated.isBookmarked).isTrue()
    }

    @Test
    fun `bookmarkArticle failure rolls back state and returns failure`() = runTest {
        // Prevent actual WorkManager scheduling during unit tests
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        val article = sampleArticle("a1", bookmarked = false)

        val store = wireDaoFlow()
        store.value = listOf(
            ArticleWithCategories(
                article = ArticleEntity(
                    id = article.id,
                    title = article.title,
                    url = article.url,
                    author = article.author,
                    readTime = article.readTimeMinutes,
                    heroImageUrl = article.heroImageUrl,
                    teaser = article.teaser,
                    source = article.source,
                    sourceAvatarUrl = article.sourceAvatarUrl,
                    ingestedAt = article.ingestedAt.toEpochMilli(),
                    isBookmarked = article.isBookmarked,
                    isDirty = false,
                    lastModified = 0L,
                ),
                categories = listOf(sampleCategoryEntity()),
            ),
        )

        coEvery { fakeArticleDao.getById("a1") } returns ArticleEntity(
            id = "a1",
            title = article.title,
            url = article.url,
            author = article.author,
            readTime = article.readTimeMinutes,
            heroImageUrl = article.heroImageUrl,
            teaser = article.teaser,
            source = article.source,
            sourceAvatarUrl = article.sourceAvatarUrl,
            ingestedAt = article.ingestedAt.toEpochMilli(),
            isBookmarked = false,
            isDirty = false,
            lastModified = 0L,
        )

        coEvery { fakeArticleService.bookmarkArticle("a1", true) } returns ApiResult.Failure(ApiError.Network("failed"))

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, fakeOrbitDatabase, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)
        advanceUntilIdle()

        // precondition: article loaded
        assertThat(repo.articles.value).isNotNull()
        assertThat(repo.articles.value!!.first().isBookmarked).isFalse()

        val result = repo.bookmarkArticle("a1", true)
        advanceUntilIdle()
        assertThat(result).isEqualTo(ApiResult.Failure(ApiError.Network("failed")))
        val updated = repo.articles.value!!.first { it.id == "a1" }
        assertThat(updated.isBookmarked).isTrue()
    }

    @Test
    fun `getCategories delegates to service`() = runTest {
        // Prevent actual WorkManager scheduling during unit tests
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        val categories = listOf(sampleCategory("c1"))

        wireDaoFlow()

        coEvery { fakeArticleService.fetchArticleCategories() } returns ApiResult.Success(categories)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, fakeOrbitDatabase, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)
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
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, fakeOrbitDatabase, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)

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
            ingestedAt = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli(),
            isBookmarked = true,
            isDirty = true,
            lastModified = 0L,
        )

        coEvery { fakeArticleDao.getDirtyArticles() } returns listOf(entity)
        coEvery { fakeArticleService.bookmarkArticle(entity.id, entity.isBookmarked) } returns ApiResult.Failure(ApiError.Network("net"))

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, fakeOrbitDatabase, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)

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
            ingestedAt = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli(),
            isBookmarked = false,
            isDirty = true,
            lastModified = 0L,
        )

        coEvery { fakeArticleDao.getDirtyArticles() } returns listOf(entity)
        coEvery { fakeArticleService.bookmarkArticle(entity.id, entity.isBookmarked) } returns ApiResult.Failure(ApiError.Http(400, "bad"))

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, fakeOrbitDatabase, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext)

        val result = repo.syncDirtyArticles()
        assertThat(result).isEqualTo(ApiResult.Success(Unit))

        // permanent failure should mark the article as not dirty
        coVerify(exactly = 1) { fakeArticleDao.insert(match { it.id == entity.id && it.isDirty == false }) }
    }

    @Test
    fun `pagedArticles returns flow with correct paging configuration`() = runTest {
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        // Initialize DB/DAO mocks used by mediator
        wireDaoFlow()

        val articles = (1..30).map { index ->
            sampleArticle("a$index", bookmarked = index % 2 == 0)
        }
        every { fakeOrbitDatabase.articleDao() } returns fakeArticleDao
        every { fakeOrbitDatabase.articleRemoteKeyDao() } returns fakeArticleRemoteKeyDao
        every { fakeArticleDao.pagingSource() } returns TestPagingSource(
            articles.map { sampleArticleWithCategories(it.id, it.isBookmarked) },
        )

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        // disable RemoteMediator for unit tests
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, fakeOrbitDatabase, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext, useRemoteMediator = false)

        val pagingDataFlow = repo.pagedArticles(defaultFilter)
        val items = pagingDataFlow.asSnapshot()

        assertThat(items.size).isEqualTo(30)
        assertThat(items[0].id).isEqualTo("a1")
        assertThat(items[29].id).isEqualTo("a30")
        assertThat(items[1].isBookmarked).isTrue()
        assertThat(items[0].isBookmarked).isFalse()
    }

    @Test
    fun `pagedArticles maps ArticleWithCategories to Article domain objects`() = runTest {
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        wireDaoFlow()
        every { fakeOrbitDatabase.articleDao() } returns fakeArticleDao
        every { fakeOrbitDatabase.articleRemoteKeyDao() } returns fakeArticleRemoteKeyDao

        val articlesWithCategories = listOf(
            sampleArticleWithCategories("a1", bookmarked = false),
            sampleArticleWithCategories("a2", bookmarked = true),
            sampleArticleWithCategories("a3", bookmarked = false),
        )

        every { fakeArticleDao.pagingSource() } returns TestPagingSource(articlesWithCategories)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, fakeOrbitDatabase, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext, useRemoteMediator = false)

        val pagingDataFlow = repo.pagedArticles(defaultFilter)
        val items = pagingDataFlow.asSnapshot()

        assertThat(items.size).isEqualTo(3)
        assertThat(items[0].id).isEqualTo("a1")
        assertThat(items[1].id).isEqualTo("a2")
        assertThat(items[2].id).isEqualTo("a3")
        assertThat(items[0].title).isEqualTo("Title")
        assertThat(items[0].isBookmarked).isFalse()
        assertThat(items[1].isBookmarked).isTrue()
        assertThat(items[2].isBookmarked).isFalse()
    }

    @Test
    fun `pagedArticles handles empty data correctly`() = runTest {
        mockkStatic("com.fourthwardai.orbit.work.SchedulerKt")
        every { scheduleArticleSync(any()) } just Runs

        wireDaoFlow()
        every { fakeOrbitDatabase.articleDao() } returns fakeArticleDao
        every { fakeOrbitDatabase.articleRemoteKeyDao() } returns fakeArticleRemoteKeyDao
        every { fakeArticleDao.pagingSource() } returns TestPagingSource<ArticleWithCategories>(emptyList())

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repo = ArticleRepositoryImpl(fakeArticleService, fakeArticleDao, fakeOrbitDatabase, scope = testScope, ioDispatcher = testDispatcher, context = fakeContext, useRemoteMediator = false)

        val pagingDataFlow = repo.pagedArticles(defaultFilter)
        val items = pagingDataFlow.asSnapshot()

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
