package com.fourthwardai.orbit.service.newsfeed

import kotlinx.serialization.Serializable

@Serializable
data class BookmarkArticleRequest(
    val recordId: String,
    val isBookmarked: Boolean,
)
