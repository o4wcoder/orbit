package com.fourthwardai.orbit.domain

data class ArticlePage(
    val items: List<Article>,
    val nextCursor: String? = null,
)
