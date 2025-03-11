package net.multun.gamecounter

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.connect
import kotlin.math.sqrt

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

fun List<Color>.allocate(usedColors: List<Color>): Color {
    if (usedColors.isEmpty())
        return DEFAULT_PALETTE[0]

    // find which palette colors are left unused
    val unusedPaletteColors = toMutableSet()
    for (usedColor in usedColors) {
        unusedPaletteColors.remove(usedColor)
    }

    // if all colors from the palette were used, pick from the whole palette
    val availableColors = if (unusedPaletteColors.isEmpty()) {
        DEFAULT_PALETTE
    } else {
        unusedPaletteColors.toList()
    }

    // pick the available color which has the most distance to the last used color
    val neighbor = usedColors.last()
    var bestIndex = -1
    var bestDistance = Float.NEGATIVE_INFINITY
    for (i in availableColors.indices) {
        val candidate = availableColors[i]
        val dist = distance(neighbor, candidate)
        if (dist <= bestDistance)
            continue

        bestIndex = i
        bestDistance = dist
    }

    assert(bestIndex != -1)
    return availableColors[bestIndex]
}

private val srgbToLab = ColorSpaces.Srgb.connect(ColorSpaces.CieLab)
private val labToSrgb = ColorSpaces.CieLab.connect(ColorSpaces.Srgb)

const val DARK_LUMA = 0.55f
const val DARK_CHROMA = 1.25f

const val LIGHT_LUMA = 0.95f
const val LIGHT_CHROMA = 1f

private fun Color.toLab(): FloatArray {
    return srgbToLab.transform(this.red, this.green, this.blue)
}

@Composable
fun Color.toDisplayColor(): Color {
    val labColor = this.toLab()

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


fun distance(a: Color, b: Color): Float {
    val aLab = a.toLab()
    val bLab = b.toLab()
    val ld = aLab[0] - bLab[0]
    val ad = aLab[1] - bLab[1]
    val bd = aLab[2] - bLab[2]
    return sqrt(ld * ld + ad * ad + bd * bd)
}