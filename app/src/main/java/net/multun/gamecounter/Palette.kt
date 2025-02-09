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

const val DARK_LUMA = 0.55f
const val DARK_CHROMA = 1.25f

const val LIGHT_LUMA = 0.95f
const val LIGHT_CHROMA = 1f

@Composable
fun Color.toDisplayColor(): Color {
    val labColor = srgbToLab.transform(this.red, this.green, this.blue)

    if (isSystemInDarkTheme()) {
        labColor[0] *= DARK_LUMA
        labColor[1] *= DARK_CHROMA
        labColor[2] *= DARK_CHROMA
    } else {
        labColor[0] *= LIGHT_LUMA
        labColor[1] *= LIGHT_CHROMA
        labColor[2] *= LIGHT_CHROMA
    }


    val res = labToSrgb.transform(labColor)
    return Color(res[0], res[1], res[2])
}