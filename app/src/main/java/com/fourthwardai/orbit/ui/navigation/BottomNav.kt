@file:Suppress("DEPRECATION")

package com.fourthwardai.orbit.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.ui.categoryfilter.CategoryFilterScreen
import com.fourthwardai.orbit.ui.newsfeed.ArticleFeed
import com.fourthwardai.orbit.ui.newsfeed.NewsFeedViewModel
import com.fourthwardai.orbit.ui.saved.SavedArticlesViewModel
import com.fourthwardai.orbit.ui.settings.SettingsScreen
import com.fourthwardai.orbit.ui.theme.OrbitTheme

sealed class Screen(val route: String, val labelRes: Int, val title: Int, val icon: ImageVector) {
    object News : Screen("home", R.string.home_tab, R.string.app_name, Icons.Filled.Home)
    object Saved : Screen("saved", R.string.saved_tab, R.string.saved_tab, Icons.Filled.Bookmark)
    object Settings : Screen("settings", R.string.settings_tab, R.string.settings_tab, Icons.Filled.Settings)
}

private val bottomNavItems = listOf(
    Screen.News,
    Screen.Saved,
    Screen.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitAppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showFilters by remember { mutableStateOf(false) }

    val newsEntry = remember(navBackStackEntry) {
        runCatching { navController.getBackStackEntry(Screen.News.route) }.getOrNull()
    }

    val savedEntry = remember(navBackStackEntry) {
        runCatching { navController.getBackStackEntry(Screen.Saved.route) }.getOrNull()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val titleRes = when (currentRoute) {
                            Screen.News.route -> Screen.News.title
                            Screen.Saved.route -> Screen.Saved.title
                            Screen.Settings.route -> Screen.Settings.title
                            else -> R.string.app_name
                        }
                        Text(
                            text = stringResource(titleRes),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },

                    actions = {
                        if (currentRoute == Screen.News.route || currentRoute == Screen.Saved.route) {
                            IconButton(onClick = { showFilters = true }) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = stringResource(R.string.filters_title),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            },
            bottomBar = {
                BottomBar(navController = navController)
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.News.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.News.route) {
                    val viewModel: NewsFeedViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val pagedArticles = viewModel.pagedArticles.collectAsLazyPagingItems()
                    ArticleFeed(
                        uiModel = uiState,
                        pagedArticles = pagedArticles,
                        categories = viewModel.categories.collectAsStateWithLifecycle().value,
                        filters = viewModel.filter.collectAsStateWithLifecycle().value,
                        onRefresh = viewModel::refreshArticles,
                        onApply = { groups, categoryIds ->
                            viewModel.onFiltersApplied(groups, categoryIds)
                        },
                        onBookmarkClick = viewModel::onBookmarkClick,
                    )
                }
                composable(Screen.Saved.route) {
                    val viewModel: SavedArticlesViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val pagedArticles = viewModel.pagedArticles.collectAsLazyPagingItems()
                    ArticleFeed(
                        uiModel = uiState,
                        pagedArticles = pagedArticles,
                        categories = viewModel.categories.collectAsStateWithLifecycle().value,
                        filters = viewModel.filter.collectAsStateWithLifecycle().value,
                        isRefreshEnabled = false,
                        onRefresh = {},
                        onApply = { groups, categoryIds ->
                            viewModel.onFiltersApplied(groups, categoryIds)
                        },
                        onBookmarkClick = viewModel::onBookmarkClick,
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }

        // Intercept system back button when filters overlay is shown so it dismisses the overlay
        if (showFilters) {
            BackHandler {
                showFilters = false
            }
        }

        // Animated fullscreen filter screen overlay
        AnimatedVisibility(
            visible = showFilters && (currentRoute == Screen.News.route || currentRoute == Screen.Saved.route),
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(320),
            ) + fadeIn(animationSpec = tween(320)),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(280),
            ) + fadeOut(animationSpec = tween(280)),
        ) {
            when (currentRoute) {
                Screen.News.route -> {
                    newsEntry?.let {
                        val newsVm: NewsFeedViewModel = hiltViewModel(it)

                        CategoryFilterScreen(
                            categories = newsVm.categories.collectAsStateWithLifecycle().value,
                            initialSelectedGroups = newsVm.filter.collectAsStateWithLifecycle().value.selectedGroups,
                            initialSelectedCategoryIds = newsVm.filter.collectAsStateWithLifecycle().value.selectedCategoryIds,
                            onApply = { groups, categoryIds ->
                                newsVm.onFiltersApplied(groups, categoryIds)
                                showFilters = false
                            },
                            onDismiss = { showFilters = false },
                        )
                    }
                }

                Screen.Saved.route -> {
                    savedEntry?.let {
                        val savedVm: SavedArticlesViewModel = hiltViewModel(it)

                        CategoryFilterScreen(
                            categories = savedVm.categories.collectAsStateWithLifecycle().value,
                            initialSelectedGroups = savedVm.filter.collectAsStateWithLifecycle().value.selectedGroups,
                            initialSelectedCategoryIds = savedVm.filter.collectAsStateWithLifecycle().value.selectedCategoryIds,
                            onApply = { groups, categoryIds ->
                                savedVm.onFiltersApplied(groups, categoryIds)
                                showFilters = false
                            },
                            onDismiss = { showFilters = false },
                        )
                    }
                }

                else -> {
                    // No overlay for other routes
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(screen.labelRes),
                    )
                },
                label = { Text(text = stringResource(screen.labelRes)) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBottomNav() {
    OrbitTheme {
        BottomBar(navController = rememberNavController())
    }
}
