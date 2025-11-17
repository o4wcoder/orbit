package com.fourthwardai.orbit.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fourthwardai.orbit.R
import com.fourthwardai.orbit.domain.Article
import com.fourthwardai.orbit.extensions.HorizontalSpacer
import com.fourthwardai.orbit.extensions.VerticalSpacer
import com.fourthwardai.orbit.ui.theme.OrbitTheme

@Composable
fun ArticleCard(article: Article, modifier: Modifier = Modifier) {
    val isPreview = LocalInspectionMode.current
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
//            AsyncImage(
//                modifier = Modifier.fillMaxWidth(),
//                model = article.heroImageUrl,
//                contentScale = ContentScale.FillWidth,
//                placeholder =  painterResource(R.drawable.article_example) ,//else null,
//                contentDescription = null
//
//            )

            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(R.drawable.article_example),
                // model = article.heroImageUrl,
                contentScale = ContentScale.FillWidth,
                //  placeholder =  painterResource(R.drawable.article_example) ,//else null,
                contentDescription = null,

            )
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                VerticalSpacer(16.dp)
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                )

                VerticalSpacer(8.dp)
                Row(modifier = Modifier.fillMaxWidth()) {
                    article.author?.let {
                        Text(text = article.author)
                        HorizontalSpacer(16.dp)
                    }
                    Text(text = article.source)
                }
                VerticalSpacer(8.dp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArticleCardPreview() {
    OrbitTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ArticleCard(
                article = getArticlePreviewData(),
            )
        }
    }
}

internal fun getArticlePreviewData() =

    Article(
        title = "Using Kotlin in Android Development",
        url = "https://example.com",
        author = "John Doe",
        readTime = 5,
        heroImageUrl = "https://example.com/image.jpg",
        source = "Example Blog",

    )
