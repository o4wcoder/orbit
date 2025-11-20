package com.fourthwardai.orbit.ui.newsfeed

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.LoadingSpinner
import com.fourthwardai.orbit.ui.theme.OrbitTheme

/**
 * Full-screen Dashboard composable that hosts a Scaffold with an AppBar showing the app title.
 */

private const val MEDIUM_PACKAGE = "com.medium.reader"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewsFeed(modifier: Modifier = Modifier) {
    val viewModel: NewsFeedViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        NewsFeedContent(uiState = uiState, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
fun NewsFeedContent(uiState: NewsFeedUiModel, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val context = LocalContext.current

        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val gradient = Brush.verticalGradient(
            colors = listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary),
            startY = heightPx / 2f,
            endY = heightPx,
        )

        Box(modifier = Modifier.fillMaxSize().background(brush = gradient)) {
            when (val state = uiState) {
                is NewsFeedUiModel.Loading -> {
                    LoadingSpinner()
                }

                is NewsFeedUiModel.Content -> {
                    LazyColumn(
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
    } catch (e: Exception) {
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
            uiState = NewsFeedUiModel.Content(
                articles = listOf(getArticlePreviewData(), getArticlePreviewData()),
            ),
        )
    }
}
