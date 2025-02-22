package net.multun.gamecounter.ui.board

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

fun FontScale.apply(baseFontSize: TextUnit, maxFontSize: TextUnit): TextUnit {
    val res =  baseFontSize * value
    if (res > maxFontSize)
        return maxFontSize
    return res
}