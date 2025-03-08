package net.multun.gamecounter.ui.board

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.min


@JvmInline
value class FontScale(val value: Float)

data class FontSizeClass(val base: Dp, val max: Dp)

fun counterScale(maxWidth: Dp, maxHeight: Dp): FontScale {
    val verticalScale = maxHeight / PLAYER_MIN_HEIGHT
    val horizontalScale = maxWidth / PLAYER_MIN_WIDTH
    return FontScale(min(verticalScale, horizontalScale))
}

@Composable
fun FontScale.apply(baseFontSize: Dp, maxFontSize: Dp): TextUnit {
    var res = baseFontSize * value
    if (res > maxFontSize)
        res = maxFontSize
    return with(LocalDensity.current) { res.toSp() }
}

@Composable
fun WithFontSize(
    fontSize: TextUnit,
    lineHeight: Float = 1.15f,
    baseStyle: TextStyle = LocalTextStyle.current,
    content: @Composable () -> Unit
) {
    val newStyle = baseStyle.copy(fontSize = fontSize, lineHeight = fontSize * lineHeight)
    CompositionLocalProvider(LocalTextStyle provides newStyle, content = content)
}


@Composable
fun WithScaledFontSize(
    scale: FontScale,
    sizeClass: FontSizeClass,
    lineHeight: Float = 1.15f,
    baseStyle: TextStyle = LocalTextStyle.current,
    content: @Composable () -> Unit
) {
    WithFontSize(scale.apply(sizeClass.base, sizeClass.max), lineHeight, baseStyle, content)
}