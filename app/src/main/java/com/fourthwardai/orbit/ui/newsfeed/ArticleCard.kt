package com.fourthwardai.orbit.ui.newsfeed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.domain.Category
import com.fourthwardai.orbit.extensions.HorizontalSpacer
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.theme.LocalWindowClassSize
import com.fourthwardai.orbit.ui.theme.OrbitTheme
import com.fourthwardai.orbit.ui.util.harmonizeColor
import com.fourthwardai.orbit.ui.util.sourceAccentColorFromName
import com.revenuecat.placeholder.PlaceholderDefaults
import com.revenuecat.placeholder.placeholder
import java.util.Locale

@Composable
fun ArticleCard(article: Article, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val locale: Locale =
        if (configuration.locales.isEmpty) Locale.getDefault() else configuration.locales[0]
    val windowSizeClass = LocalWindowClassSize.current
    val widthSizeClass = windowSizeClass.widthSizeClass

    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ArticleHeroImage(article.heroImageUrl)

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                VerticalSpacer(16.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val capitalizedSource = article.source.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                    }
                    SourceAvatar(
                        imageUrl = article.sourceAvatarUrl,
                        sourceName = capitalizedSource,
                    )
                    HorizontalSpacer(8.dp)
                    Text(
                        text = capitalizedSource,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                VerticalSpacer(8.dp)

                Text(
                    text = article.title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.headlineSmall,
                )

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
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(article.categories) { category ->
                            val isLightTheme =
                                MaterialTheme.colorScheme.surface.luminance() > 0.5f
                            val chipBg: Color =
                                if (isLightTheme) category.colorLight else category.colorDark
                            val contentColor: Color =
                                if (chipBg.luminance() > 0.5f) Color.Black else Color.White

                            AssistChip(
                                onClick = { /* no-op */ },
                                label = {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 12.sp,
                                        ),
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

@Composable
fun ArticleHeroImage(heroImageUrl: String?) {
    var isLoading by remember(heroImageUrl) { mutableStateOf(true) }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(heroImageUrl)
            .transitionFactory(CrossfadeTransition.Factory())
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .placeholder(
                enabled = isLoading,
                highlight = PlaceholderDefaults.shimmer,
            ),
        error = painterResource(R.drawable.orbit_article_placeholder),
        onLoading = { isLoading = true },
        onSuccess = { isLoading = false },
        onError = { isLoading = false },
    )
}

@Composable
fun SourceAvatar(
    imageUrl: String?,
    sourceName: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    val firstLetterOfSource = (sourceName.firstOrNull() ?: '?').toString()

    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    // Base neutral ring, slightly stronger in dark mode
    val ringColor = colors.onSurface.copy(alpha = if (isDark) 0.24f else 0.12f)

    // Source-specific accent, harmonized with surface
    val harmonizedBackground = remember(sourceName, colors.surface, isDark) {
        val accent = sourceAccentColorFromName(sourceName, isDark)
        harmonizeColor(accent, colors.surface, blendAmount = if (isDark) 0.55f else 0.65f)
    }

    var loadFailed by remember(imageUrl) { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, ringColor, CircleShape)
            .background(harmonizedBackground),
    ) {
        if (!imageUrl.isNullOrBlank() && !loadFailed) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$sourceName logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
                    .placeholder(
                        enabled = isLoading,
                        highlight = PlaceholderDefaults.shimmer,
                    ),
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = {
                    loadFailed = true
                    isLoading = false
                },
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = firstLetterOfSource,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface.copy(alpha = 0.9f),
                )
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
        sourceAvatarUrl = null,
        teaser = "This is a really cool article about Kotlin in Android Development",
        createdTime = "2023-07-10T12:00:00Z",
        ingestedAt = "2023-07-10T12:00:00Z",
        categories = listOf(
            Category(
                id = "1",
                name = "Android",
                group = "Android",
                colorLight = Color(0xFF00FF00),
                colorDark = Color(0xFF00FF00),
            ),
        ),

    )
