@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fourthwardai.orbit.ui.newsfeed

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.domain.FeedFilter
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.LoadingSpinner
import com.fourthwardai.orbit.ui.categoryfilter.CategoryFilterDialog
import com.fourthwardai.orbit.ui.theme.LocalWindowClassSize
import com.fourthwardai.orbit.ui.theme.OrbitTheme

private const val MEDIUM_PACKAGE = "com.medium.reader"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewsFeed(
    uiModel: NewsFeedUiModel,
    categories: List<Category>,
    showFilters: Boolean,
    filters: FeedFilter,
    onRefresh: () -> Unit,
    onDismissFilters: () -> Unit,
    onApply: (selectedGroups: Set<String>, selectedCategoryIds: Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState() // phone list
    val staggeredGridState = rememberLazyStaggeredGridState() // tablet

    // Scroll-to-top effect when filters change (from dialog)
    LaunchedEffect(filters.selectedGroups, filters.selectedCategoryIds) {
        // pick which state to scroll depending on layout, or just scroll both safely
        listState.scrollToItem(0)
        staggeredGridState.scrollToItem(0)
    }
    Column(modifier = modifier) {
        NewsFeedActiveFiltersBar(
            categories = categories,
            filters = filters,
            onApply = onApply,
        )

        NewsFeedContent(
            uiModel = uiModel,
            listState = listState,
            staggeredGridState = staggeredGridState,
            onRefresh = onRefresh,
            modifier = Modifier,
        )
    }

    if (showFilters) {
        CategoryFilterDialog(
            categories = categories,
            initialSelectedGroups = filters.selectedGroups,
            initialSelectedCategoryIds = filters.selectedCategoryIds,
            onApply = onApply,
            onDismiss = onDismissFilters,
        )
    }
}

@Composable
private fun NewsFeedContent(
    uiModel: NewsFeedUiModel,
    listState: LazyListState,
    staggeredGridState: LazyStaggeredGridState,
    onRefresh: () -> Unit,
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
            modifier = Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
            indicator = {
                Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = uiModel.isRefreshing,
                    state = pullState,
                )
            },
        ) {
            when (val state = uiModel) {
                is NewsFeedUiModel.Loading -> {
                    LoadingSpinner()
                }

                is NewsFeedUiModel.Content -> {
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
                                state.articles,
                                key = { article -> article.id },
                            ) { article ->

                                ArticleCard(
                                    article,
                                    modifier = Modifier.clickable(
                                        role = Role.Button,
                                        onClick = { openMediumOrBrowser(context, article.url) },
                                    ),
                                )
                                VerticalSpacer(16.dp)
                            }
                        }
                    } else {
                        // Tablet: staggered grid with 2 columns and spacing between cells
                        LazyVerticalStaggeredGrid(
                            state = staggeredGridState,
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            // increase content padding so outer edges are separated from screen edges
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                        ) {
                            items(
                                state.articles,
                                key = { article -> article.id },
                            ) { article ->

                                ArticleCard(
                                    article,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .clickable(
                                            role = Role.Button,
                                            onClick = { openMediumOrBrowser(context, article.url) },
                                        ),
                                )
                            }
                        }
                    }
                }
            }
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

@Preview(showBackground = true)
@Composable
fun NewsFeedPreview() {
    OrbitTheme {
        NewsFeedContent(
            uiModel = NewsFeedUiModel.Content(
                articles = listOf(getArticlePreviewData("1"), getArticlePreviewData("2")),
            ),
            listState = rememberLazyListState(),
            staggeredGridState = rememberLazyStaggeredGridState(),
            onRefresh = {},
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, name = "Tablet Preview", device = "spec:width=800dp,height=1280dp")
@Composable
fun NewsFeedTabletPreview() {
    OrbitTheme {
        CompositionLocalProvider(
            LocalWindowClassSize provides WindowSizeClass.calculateFromSize(DpSize(1280.dp, 800.dp)),
        ) {
            NewsFeedContent(
                uiModel = NewsFeedUiModel.Content(
                    articles = listOf(getArticlePreviewData("1"), getArticlePreviewData("2"), getArticlePreviewData("3")),
                ),
                listState = rememberLazyListState(),
                staggeredGridState = rememberLazyStaggeredGridState(),
                onRefresh = {},
            )
        }
    }
}
