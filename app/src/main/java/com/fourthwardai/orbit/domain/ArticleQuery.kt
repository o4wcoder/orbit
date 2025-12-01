package com.fourthwardai.orbit.domain

data class ArticleQuery(
    val filter: FeedFilter = FeedFilter(),
    val pageSize: Int = 50,
    val cursor: String? = null,
)
