package net.multun.gamecounter.ui.board

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.min


@JvmInline
value class FontScale(val value: Float)

fun counterScale(maxWidth: Dp, maxHeight: Dp): FontScale {
    val verticalScale = maxHeight / PLAYER_MIN_HEIGHT
    val horizontalScale = maxWidth / PLAYER_MIN_WIDTH
    return FontScale(min(verticalScale, horizontalScale))
}

@Composable
private fun FontScale.apply(baseFontSize: Dp, maxFontSize: Dp): TextUnit {
    var res = baseFontSize * value
    if (res > maxFontSize)
        res = maxFontSize
    return with(LocalDensity.current) { res.toSp() }
}

@Composable
fun ScaledText(
    text: String,
    scale: FontScale,
    baseSize: Dp,
    maxSize: Dp,
    modifier: Modifier = Modifier,
    lineHeight: Float = 1.15f,
) {
    val textSize = scale.apply(baseSize, maxSize)
    Text(text = text, fontSize = textSize, lineHeight = textSize * lineHeight, modifier = modifier)
}