package com.fourthwardai.orbit.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.ui.newsfeed.NewsFeed
import com.fourthwardai.orbit.ui.newsfeed.NewsFeedViewModel
import com.fourthwardai.orbit.ui.theme.OrbitTheme
import com.fourthwardai.orbit.ui.trends.AddTrendFAB
import com.fourthwardai.orbit.ui.trends.Trends

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object News : Screen("news", R.string.news_tab, Icons.Filled.Article)
    object Trends : Screen("trends", R.string.trends_tab, Icons.Filled.TrendingUp)
    object Settings : Screen("settings", R.string.settings_tab, Icons.Filled.Settings)
}

private val bottomNavItems = listOf(
    Screen.News,
    Screen.Trends,
    Screen.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitAppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isAddTrendFabExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        var scaffoldPadding by remember { mutableStateOf(PaddingValues()) }
        Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },

                    actions = {
                        if (currentRoute == Screen.News.route) {
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
            scaffoldPadding = innerPadding
            NavHost(
                navController = navController,
                startDestination = Screen.News.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.News.route) {
                    val viewModel: NewsFeedViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    NewsFeed(
                        uiModel = uiState,
                        onRefresh = viewModel::refreshArticles,
                        showFilters = showFilters,
                        filters = viewModel.filter.collectAsStateWithLifecycle().value,
                        onDismissFilters = { showFilters = false },
                        onApply = { groups, categoryIds, bookmarkedOnly ->
                            viewModel.onFiltersApplied(groups, categoryIds, bookmarkedOnly)
                            showFilters = false
                        },
                        onBookmarkClick = viewModel::onBookmarkClick,
                        categories = viewModel.categories.collectAsStateWithLifecycle().value,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable(Screen.Trends.route) {
                    Trends()
                }
                composable(Screen.Settings.route) {
                    // Empty placeholder for Settings
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = stringResource(R.string.settings_tab_placeholder),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(16.dp).align(Alignment.Center),
                        )
                    }
                }
            }
        }

        if (currentRoute == Screen.Trends.route) {
            AddTrendFAB(
                modifier = Modifier.navigationBarsPadding(),
                innerPadding = scaffoldPadding,
                expanded = isAddTrendFabExpanded,
                onExpandedChanged = { isAddTrendFabExpanded = it },
                onConfirm = { },
            )
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
