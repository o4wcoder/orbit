package com.fourthwardai.orbit.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val group: String,
    val colorLight: String,
    val colorDark: String,
)
