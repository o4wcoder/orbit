package com.fourthwardai.orbit.domain

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import com.fourthwardai.orbit.network.dto.CategoryDto
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Category(
    val id: String,
    val name: String,
    val group: String,
    val colorLight: @RawValue Color,
    val colorDark: @RawValue Color,
) : Parcelable

fun CategoryDto.toDomain(): Category =
    Category(
        id = id,
        name = name,
        group = group,
        colorLight = colorLight.toComposeColor(),
        colorDark = colorDark.toComposeColor(),
    )
