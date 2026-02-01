package com.fourthwardai.orbit.domain

import java.time.Instant

data class ArticlesPageCursor(
    val beforeIngestedAt: Instant,
    val beforeId: String,
)
