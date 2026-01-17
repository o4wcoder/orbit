@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fourthwardai.orbit.ui.newsfeed

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.theme.LocalWindowClassSize
import com.fourthwardai.orbit.ui.theme.OrbitTheme
import kotlinx.coroutines.flow.flowOf

private const val MEDIUM_PACKAGE = "com.medium.reader"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArticleFeed(
    uiModel: NewsFeedUiModel,
    pagedArticles: LazyPagingItems<Article>,
    categories: List<Category>,
    filters: FeedFilter,
    onRefresh: () -> Unit,
    onApply: (selectedGroups: Set<String>, selectedCategoryIds: Set<String>) -> Unit,
    onBookmarkClick: (id: String, isBookmarked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isRefreshEnabled: Boolean = true,
) {
    val listState = rememberLazyListState() // phone list
    val staggeredGridState = rememberLazyStaggeredGridState() // tablet

    // Scroll-to-top effect when filters change (from dialog)
    LaunchedEffect(filters) {
        // pick which state to scroll depending on layout, or just scroll both safely
        listState.scrollToItem(0)
        staggeredGridState.scrollToItem(0)
    }
    Column(modifier = modifier) {
        ActiveFiltersBar(
            categories = categories,
            filters = filters,
            onApply = onApply,
        )

        ArticleFeedContent(
            uiModel = uiModel,
            pagedArticles = pagedArticles,
            listState = listState,
            staggeredGridState = staggeredGridState,
            isRefreshEnabled = isRefreshEnabled,
            onRefresh = onRefresh,
            onBookmarkClick = onBookmarkClick,
            modifier = Modifier,
        )
    }
}

@Composable
private fun ArticleFeedContent(
    uiModel: NewsFeedUiModel,
    pagedArticles: LazyPagingItems<Article>,
    listState: LazyListState,
    staggeredGridState: LazyStaggeredGridState,
    isRefreshEnabled: Boolean,
    onRefresh: () -> Unit,
    onBookmarkClick: (id: String, isBookmarked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // No Scaffold here — the top app bar is owned by the app-level Scaffold in OrbitAppNavHost
    BoxWithConstraints(modifier = modifier) {
        val context = LocalContext.current
        val windowSizeClass = LocalWindowClassSize.current
        val widthSizeClass = windowSizeClass.widthSizeClass
        val pullState = rememberPullToRefreshState()

        // Taking the gradient out for now. Doesn't quite look right.
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val gradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.primary,
            ),
            startY = heightPx / 2f,
            endY = heightPx,
        )

        PullToRefreshBox(
            state = pullState,
            isRefreshing = uiModel.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
            indicator = {
                if (isRefreshEnabled) {
                    Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = uiModel.isRefreshing,
                        state = pullState,
                    )
                }
            },
        ) {
            val refreshState = pagedArticles.loadState.refresh
            val hasItems = pagedArticles.itemCount > 0

            when {
                refreshState is LoadState.Error && !hasItems -> {
                    // full screen error
                }

                !hasItems && refreshState is LoadState.Loading -> {}

                !hasItems && refreshState is LoadState.NotLoading -> {
                    EmptyMessage()
                }

                else -> {
                    if (widthSizeClass == WindowWidthSizeClass.Compact) {
                        // Phone: single column list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                        ) {
                            item {
                                VerticalSpacer(16.dp)
                            }
                            items(
                                pagedArticles.itemCount,
                                key = { index ->
                                    pagedArticles[index]?.id ?: "placeholder-$index"
                                },
                            ) { index ->
                                val article = pagedArticles[index] ?: return@items

                                ArticleCard(
                                    article,
                                    onBookmarkClick = onBookmarkClick,
                                    modifier = Modifier.clickable(
                                        role = Role.Button,
                                        onClick = { openMediumOrBrowser(context, article.url) },
                                    ),
                                )
                                VerticalSpacer(16.dp)
                            }

                            when (val append = pagedArticles.loadState.append) {
                                is LoadState.Loading -> item { FooterLoading() }
                                is LoadState.Error -> item { FooterError(onRetry = { pagedArticles.retry() }) }
                                else -> Unit
                            }
                        }
                    } else {
                        // Tablet: staggered grid with 2 columns and spacing between cells
                        LazyVerticalStaggeredGrid(
                            state = staggeredGridState,
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            // increase content padding so outer edges are separated from screen edges
                            contentPadding = PaddingValues(
                                horizontal = 24.dp,
                                vertical = 24.dp,
                            ),
                        ) {
                            items(
                                pagedArticles.itemCount,
                                key = { index ->
                                    pagedArticles[index]?.id ?: "placeholder-$index"
                                },
                            ) { index ->
                                val article = pagedArticles[index] ?: return@items
                                ArticleCard(
                                    article,
                                    onBookmarkClick = onBookmarkClick,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .clickable(
                                            role = Role.Button,
                                            onClick = {
                                                openMediumOrBrowser(
                                                    context,
                                                    article.url,
                                                )
                                            },
                                        ),
                                )
                            }

                            when (val append = pagedArticles.loadState.append) {
                                is LoadState.Loading -> item { FooterLoading() }
                                is LoadState.Error -> item { FooterError(onRetry = { pagedArticles.retry() }) }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center),
        ) {
            Image(
                painter = painterResource(R.drawable.empty_articles),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(200.dp),
            )

            Text(
                text = stringResource(R.string.empty_articles_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FooterLoading(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        VerticalSpacer(8.dp)

        Text(
            text = stringResource(R.string.loading_more_articles),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun FooterError(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(R.string.error_loading_more_articles),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        VerticalSpacer(8.dp)

        FilledTonalButton(onClick = onRetry) {
            Text(text = stringResource(R.string.error_loading_more_articles_retry))
        }
    }
}

fun openMediumOrBrowser(context: Context, url: String) {
    val uri = Uri.parse(url)

    // 1) Try Medium app explicitly
    val mediumIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        `package` = MEDIUM_PACKAGE
    }

    try {
        context.startActivity(mediumIntent)
    } catch (_: Exception) {
        // 2) Fallback to any browser
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(browserIntent)
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true)
@Composable
fun ArticleFeedPreview() {
    OrbitTheme {
        CompositionLocalProvider(
            LocalWindowClassSize provides WindowSizeClass.calculateFromSize(DpSize(1280.dp, 800.dp)),
        ) {
            val fakeArticles =
                listOf(
                    getArticlePreviewData("1"),
                    getArticlePreviewData("2"),
                )
            val fakeFlow = flowOf(PagingData.from(fakeArticles))
            val items = fakeFlow.collectAsLazyPagingItems()
            ArticleFeedContent(
                uiModel = NewsFeedUiModel.Content(),
                pagedArticles = items,
                listState = rememberLazyListState(),
                isRefreshEnabled = true,
                staggeredGridState = rememberLazyStaggeredGridState(),
                onRefresh = {},
                onBookmarkClick = { _, _ -> },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, name = "Tablet Preview", device = "spec:width=800dp,height=1280dp")
@Composable
fun ArticleFeedTabletPreview() {
    OrbitTheme {
        CompositionLocalProvider(
            LocalWindowClassSize provides WindowSizeClass.calculateFromSize(DpSize(1280.dp, 800.dp)),
        ) {
            val fakeArticles =
                listOf(
                    getArticlePreviewData("1"),
                    getArticlePreviewData("2"),
                    getArticlePreviewData("3"),
                )
            val fakeFlow = flowOf(PagingData.from(fakeArticles))
            val items = fakeFlow.collectAsLazyPagingItems()
            ArticleFeedContent(
                uiModel = NewsFeedUiModel.Content(),
                pagedArticles = items,
                listState = rememberLazyListState(),
                isRefreshEnabled = true,
                staggeredGridState = rememberLazyStaggeredGridState(),
                onRefresh = {},
                onBookmarkClick = { _, _ -> },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true)
@Composable
private fun EmptyNewsFeedPreview() {
    OrbitTheme {
        CompositionLocalProvider(
            LocalWindowClassSize provides WindowSizeClass.calculateFromSize(DpSize(1280.dp, 800.dp)),
        ) {
            EmptyMessage()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FooterLoadingPreview() {
    OrbitTheme {
        FooterLoading()
    }
}

@Preview(showBackground = true)
@Composable
private fun FooterErrorPreview() {
    OrbitTheme {
        FooterError(onRetry = {})
    }
}

@Preview()
@Composable
private fun FooterErrorPreviewDark() {
    OrbitTheme(darkTheme = true) {
        FooterError(onRetry = {})
    }
}
