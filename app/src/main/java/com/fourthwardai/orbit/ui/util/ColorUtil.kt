package com.fourthwardai.orbit.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs

/**
 * Generate a stable, source-specific accent from the source name.
 * This is not random: same sourceName → same color.
 */
fun sourceAccentColorFromName(name: String, dark: Boolean): Color {
    if (name.isBlank()) {
        return if (dark) Color(0xFF90CAF9) else Color(0xFF1E88E5) // neutral blue default for blank source names
    }

    val hash = name.hashCode()
    val hue = (abs(hash) % 360).toFloat() // 0..359
    val saturation = if (dark) 0.55f else 0.65f // a bit muted
    val lightness = if (dark) 0.55f else 0.65f // mid tones

    return hslToColor(hue, saturation, lightness)
}

/**
 * Harmonize an accent with a base (usually the theme surface) by blending them.
 * amount = 0 → base; amount = 1 → accent.
 */
fun harmonizeColor(accent: Color, base: Color, blendAmount: Float): Color {
    val clamped = blendAmount.coerceIn(0f, 1f)
    return lerp(base, accent, clamped)
}

/**
 * Simple HSL → Color conversion (good enough for UI accents).
 */
fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2 * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2 - 1f))
    val m = l - c / 2f

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)

    return Color(r, g, b)
}
