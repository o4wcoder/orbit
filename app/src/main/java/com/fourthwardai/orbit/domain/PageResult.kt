package com.fourthwardai.orbit.domain

data class PageResult<T>(
    val items: List<T>,
    val nextCursor: ArticlesPageCursor?,
)
