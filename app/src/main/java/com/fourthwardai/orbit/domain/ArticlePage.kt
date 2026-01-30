package com.fourthwardai.orbit.domain

data class ArticlePage(
    val articles: List<Article>,
    val nextCursor: ArticlesPageCursor? = null,
)
