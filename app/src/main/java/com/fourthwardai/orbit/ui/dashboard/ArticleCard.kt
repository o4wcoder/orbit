package com.fourthwardai.orbit.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fourthwardai.orbit.domain.Article

@Composable
fun ArticleCard(article: Article, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
        }
    }
}