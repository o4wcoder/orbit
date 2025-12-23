package com.fourthwardai.orbit.ui.trends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fourthwardai.orbit.ui.theme.OrbitTheme

@Composable
fun Trends() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Trends screen", modifier = Modifier.align(Alignment.Center))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTrends() {
    OrbitTheme {
        Trends()
    }
}
