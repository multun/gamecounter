package net.multun.gamecounter

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.connect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.enums.enumEntries
import kotlin.math.sqrt

// tweaked from 2014 Material Design color palettes
// https://m2.material.io/design/color/the-color-system.html#tools-for-picking-colors
enum class PaletteColor(val color: Color) {
    Red(Color(0xFFFFCDD2)), // red 100
    Blue(Color(0xFFBBDEFB)), // blue 100
    Purple(Color(0xFFD1C4E9)), // deep purple 100
    Green(Color(0xFFC8E6C9)), // green 100
    Indigo(Color(0xFFC5CAE9)), // indigo 100
    Gold(Color(0xFFF1E4AB)),
    Teal(Color(0xFFB2DFDB)), // teal 100
    Pink(Color(0xFFF8BBD0)), // pink 100
    Cyan(Color(0xFFB2EBF2)), // cyan 100
    Orange(Color(0xFFF1CCB4)), // custom orange
    Gray(Color(0xFFE0E0E0)), // custom gray
    ;

    companion object {
        @JvmStatic
        fun allocate(usedColors: List<Color>): PaletteColor {
            val paletteColors = enumEntries<PaletteColor>()
            if (usedColors.isEmpty())
                return paletteColors[0]

            // compute how often palette colors are currently used
            val colorUsage = mutableMapOf<Color, Int>()
            for (paletteColor in paletteColors)
                colorUsage[paletteColor.color] = 0

            // only count palette colors
            for (usedColor in usedColors) {
                colorUsage.compute(usedColor) {
                        _, oldCount ->
                    if (oldCount == null)
                        return@compute null
                    oldCount + 1
                }
            }

            // the number of time the least used color occurred
            val leastUsedCount = colorUsage.values.minOrNull() ?: 0

            // iterate over the palette, pick the first color that hasn't been used too many times
            val availableColors = enumEntries<PaletteColor>().filter { (colorUsage[it.color] ?: 0) <= leastUsedCount }
            return availableColors[0]
        }
    }
}

val PALETTE = enumEntries<PaletteColor>().map { it.color }


private val transformSpace = ColorSpaces.Oklab
private val srgbToLab = ColorSpaces.Srgb.connect(transformSpace)
private val labToSrgb = transformSpace.connect(ColorSpaces.Srgb)

const val DARK_LUMA = 0.65f
const val DARK_CHROMA = 1.45f

private fun Color.toOklab(): FloatArray {
    return srgbToLab.transform(this.red, this.green, this.blue)
}

@Composable
fun Color.toDisplayColor(isDark: Boolean = isSystemInDarkTheme()): Color {
    val labColor = this.toOklab()

    if (isDark) {
        labColor[0] *= DARK_LUMA
        labColor[1] *= DARK_CHROMA
        labColor[2] *= DARK_CHROMA
    }

    val res = labToSrgb.transform(labColor)
    return Color(res[0], res[1], res[2])
}

@ExperimentalLayoutApi
@Preview(widthDp = 600)
@Composable
fun PalettePreview() {
    val lightColors = PALETTE.map { it.toDisplayColor(false) }
    val darkColors = PALETTE.map { it.toDisplayColor(true) }

    Column {
        Colors(darkColors, modifier = Modifier.background(darkColorScheme().background))
        Colors(lightColors, modifier = Modifier.background(lightColorScheme().background))
    }
}

@ExperimentalLayoutApi
@Composable
fun Colors(colors: List<Color>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically
        ),
    ) {
        for (color in colors) {
            Spacer(
                modifier = Modifier
                    .size(100.dp)
                    .background(color)
            )
        }
    }
}