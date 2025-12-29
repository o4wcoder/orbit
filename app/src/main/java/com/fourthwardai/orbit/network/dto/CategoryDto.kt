package com.fourthwardai.orbit.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    @SerialName("slug")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("category_group")
    val group: String,
    @SerialName("color_light")
    val colorLight: String,
    @SerialName("color_dark")
    val colorDark: String,
)
