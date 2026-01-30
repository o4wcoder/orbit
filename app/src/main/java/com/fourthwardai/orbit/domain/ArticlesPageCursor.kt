package com.fourthwardai.orbit.domain

import java.time.Instant

data class ArticlesPageCursor(
    val beforeIngestedAt: Instant,
    val beforeId: String,
)

private fun ArticlesPageCursor.toQueryParams(): Map<String, String> = mapOf(
    "beforeIngestedAt" to beforeIngestedAt.toString(),
    "beforeId" to beforeId,
)
