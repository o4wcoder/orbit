package com.fourthwardai.orbit.ui.newsfeed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.theme.OrbitTheme

/**
 * Full-screen Dashboard composable that hosts a Scaffold with an AppBar showing the app title.
 */
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
        BoxWithConstraints(modifier = Modifier.padding(innerPadding)) {
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val gradient = Brush.verticalGradient(
                colors = listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary),
                startY = heightPx / 2f,
                endY = heightPx,
            )

            when (val state = uiState) {
                is NewsFeedUiModel.Loading -> {
                    // TODO: Show loading state
                }
                is NewsFeedUiModel.Content -> {
                    LazyColumn(
                        modifier = Modifier
                            .background(brush = gradient)
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                    ) {
                        items(state.articles) { article ->
                            ArticleCard(article)
                            VerticalSpacer(16.dp)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewsFeedPreview() {
    OrbitTheme {
        NewsFeed()
    }
}
