package com.fourthwardai.orbit.ui.newsfeed

import com.fourthwardai.orbit.domain.Article

data class NewsFeedDataState(
    val articles: List<Article> = emptyList(),
)
