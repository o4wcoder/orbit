package com.fourthwardai.orbit.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.network.ApiError
import com.fourthwardai.orbit.network.ApiResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleRepositoryTest {

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
        categories = emptyList(),
        isBookmarked = bookmarked,
    )

    private fun sampleCategory(id: String = "c1") = Category(
        id = id,
        name = "Cat",
        group = "grp",
        colorLight = androidx.compose.ui.graphics.Color(0xFF808080.toInt()),
        colorDark = androidx.compose.ui.graphics.Color(0xFFA9A9A9.toInt()),
    )

    @Test
    fun `bookmarkArticle updates in-memory state and returns configured result`() = runTest {
        val repo = FakeArticleRepository(initialArticles = listOf(sampleArticle("a1", bookmarked = false)))
        repo.bookmarkResult = ApiResult.Success(Unit)

        val result = repo.bookmarkArticle("a1", true)

        assertThat(result).isEqualTo(ApiResult.Success(Unit))
        val updated = repo.articles.value!!.first()
        assertThat(updated.isBookmarked).isTrue()
    }

    @Test
    fun `getCategories returns configured result`() = runTest {
        val categories = listOf(sampleCategory("c1"))
        val repo = FakeArticleRepository()
        repo.categoriesResult = ApiResult.Success(categories)

        val result = repo.getCategories()

        assertThat(result).isEqualTo(ApiResult.Success(categories))
    }

    @Test
    fun `pagedArticles exposes a collectable flow`() = runTest {
        val articles = listOf(
            sampleArticle("a1", bookmarked = false),
            sampleArticle("a2", bookmarked = true),
        )
        val repo = FakeArticleRepository()
        repo.setPagedArticles(articles)

        // Smoke check: the flow can be collected without hanging.
        var collected = false
        val job: Job = launch {
            repo.pagedArticles(FeedFilter())
                .take(1) // one emission is enough to prove it works
                .collect { collected = true }
        }
        job.join()

        assertThat(collected).isTrue()
    }

    @Test
    fun `syncDirtyArticles returns configured result`() = runTest {
        val repo = FakeArticleRepository()
        repo.syncDirtyResult = ApiResult.Failure(ApiError.Network("net"))

        val result = repo.syncDirtyArticles()

        assertThat(result).isEqualTo(ApiResult.Failure(ApiError.Network("net")))
    }
}
