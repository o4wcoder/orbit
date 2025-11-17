package com.fourthwardai.orbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fourthwardai.orbit.ui.dashboard.Dashboard
import com.fourthwardai.orbit.ui.dashboard.getArticlePreviewData
import com.fourthwardai.orbit.ui.theme.OrbitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrbitTheme {
                Dashboard(articles = listOf(getArticlePreviewData(), getArticlePreviewData(), getArticlePreviewData(), getArticlePreviewData()))
            }
        }
    }
}
