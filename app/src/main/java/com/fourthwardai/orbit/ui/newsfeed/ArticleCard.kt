package com.fourthwardai.orbit.ui.newsfeed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.extensions.HorizontalSpacer
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.theme.LocalWindowClassSize
import com.fourthwardai.orbit.ui.theme.OrbitTheme

@Composable
fun ArticleCard(article: Article, modifier: Modifier = Modifier) {
    val windowSizeClass = LocalWindowClassSize.current
    val widthSizeClass = windowSizeClass.widthSizeClass

    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                modifier = Modifier.fillMaxWidth(),
                model = article.heroImageUrl,
                contentScale = ContentScale.FillWidth,
                placeholder = painterResource(R.drawable.article_example),
                contentDescription = null,

            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                VerticalSpacer(16.dp)
                Text(
                    text = article.title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.headlineSmall,
                )

                VerticalSpacer(8.dp)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                    ) {
                        Text(
                            text = article.source,

                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiary,
                        )
                    }

                    article.author?.let {
                        HorizontalSpacer(16.dp)
                        Text(text = article.author, style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (widthSizeClass != WindowWidthSizeClass.Compact && article.teaser != null) {
                    VerticalSpacer(8.dp)
                    Text(
                        text = article.teaser,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                if (article.categories.isNotEmpty()) {
                    VerticalSpacer(8.dp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    ) {
                        items(article.categories) { category ->
                            // Determine chip background based on current theme (use surface luminance)
                            val isLightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f
                            val chipBg: Color = if (isLightTheme) category.colorLight else category.colorDark
                            val contentColor: Color = if (chipBg.luminance() > 0.5f) Color.Black else Color.White

                            AssistChip(
                                onClick = { /* no-op */ },
                                label = {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                        color = contentColor,
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = chipBg,
                                ),
                            )
                        }
                    }
                }

                VerticalSpacer(8.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true)
@Composable
private fun ArticleCardPreview() {
    OrbitTheme {
        CompositionLocalProvider(
            LocalWindowClassSize provides WindowSizeClass.calculateFromSize(DpSize(1280.dp, 800.dp)),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                ArticleCard(
                    article = getArticlePreviewData("1"),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, name = "Tablet Preview", device = "spec:width=800dp,height=1280dp")
@Composable
private fun ArticleCardTabletPreview() {
    OrbitTheme {
        CompositionLocalProvider(
            LocalWindowClassSize provides WindowSizeClass.calculateFromSize(DpSize(1280.dp, 800.dp)),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                ArticleCard(
                    article = getArticlePreviewData("1"),
                )
            }
        }
    }
}

internal fun getArticlePreviewData(id: String) =

    Article(
        id = id,
        title = "Using Kotlin in Android Development",
        url = "https://example.com",
        author = "John Doe",
        readTimeMinutes = 5,
        heroImageUrl = "https://example.com/image.jpg",
        source = "Example Blog",
        teaser = "This is a really cool article about Kotlin in Android Development",
        createdTime = "2023-07-10T12:00:00Z",
        ingestedAt = "2023-07-10T12:00:00Z",
        categories = listOf(
            Category(
                id = "1",
                name = "Android",
                colorLight = Color(0xFF00FF00),
                colorDark = Color(0xFF00FF00),
            ),
        ),

    )
