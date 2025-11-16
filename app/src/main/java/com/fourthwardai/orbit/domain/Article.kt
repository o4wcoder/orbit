package com.fourthwardai.orbit.domain

data class Article(val title: String,
    val url: String,
    val author: String?,
    val readTime: Int?,
    val heroImageUrl: String?,
    val source: String)
