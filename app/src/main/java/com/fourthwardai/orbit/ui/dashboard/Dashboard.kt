package com.fourthwardai.orbit.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.ui.theme.OrbitTheme
import kotlinx.coroutines.launch

/**
 * Full-screen Dashboard composable that hosts a Scaffold with an AppBar showing the app title.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Dashboard(modifier: Modifier = Modifier) {
    val tabs = listOf("Medium", "Android", "AI")
    // pass page count as Int to rememberPagerState (expected by the bundled Compose version)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.app_name),
                  color = MaterialTheme.colorScheme.onSurface)},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints {
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val gradient = Brush.verticalGradient(
                colors = listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary),
                startY = heightPx / 2f,
                endY = heightPx
            )
        val gradientColors = listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier
            .background(brush = gradient)
            .fillMaxSize()
            .padding(innerPadding)) {

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ElevatedCard(

                    modifier = Modifier.padding(16.dp).fillMaxWidth().height(200.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "content",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { /*TODO*/ },
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            Text(text = "Button")
                        }
                    }
                }
            }
        }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    OrbitTheme() {
        Dashboard()
    }
}
