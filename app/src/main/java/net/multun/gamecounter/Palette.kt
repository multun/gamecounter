package net.multun.gamecounter

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.connect

// 2014 Material Design color palettes
// https://m2.material.io/design/color/the-color-system.html#tools-for-picking-colors
val DEFAULT_PALETTE = listOf(
    Color(0xFFFFCDD2), // red 100
    Color(0xFFF8BBD0), // pink 100
    Color(0xFFE1BEE7), // purple 100
    Color(0xFFD1C4E9), // deep purple 100

    Color(0xFFC5CAE9), // indigo 100
    Color(0xFFBBDEFB), // blue 100
    Color(0xFFB2EBF2), // cyan 100
    Color(0xFFB2DFDB), // teal 100

    Color(0xFFC8E6C9), // green 100
    Color(0xFFFFF9C4), // yellow 100
    Color(0xFFFFE0B2), // orange 100
    Color(0xFFF5F5F5), // gray 100
)

private val srgbToLab = ColorSpaces.Srgb.connect(ColorSpaces.CieLab)
private val labToSrgb = ColorSpaces.CieLab.connect(ColorSpaces.Srgb)

const val DARKENING = 0.55f
const val DESATURATION = 1.25f

@Composable
fun Color.toDisplayColor(): Color {
    if (!isSystemInDarkTheme()) {
        return this
    }

    val labColor = srgbToLab.transform(this.red, this.green, this.blue)
    labColor[0] *= DARKENING
    labColor[1] *= DESATURATION
    labColor[2] *= DESATURATION

    val res = labToSrgb.transform(labColor)
    return Color(res[0], res[1], res[2])
}