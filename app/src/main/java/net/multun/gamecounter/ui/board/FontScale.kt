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
fun FontScale.applyDp(sizeClass: FontSizeClass): Dp {
    var res = sizeClass.base * value
    if (res > sizeClass.max)
        res = sizeClass.max
    return res
}

@Composable
fun FontScale.apply(sizeClass: FontSizeClass): TextUnit {
    return with(LocalDensity.current) {
        this@apply.applyDp(sizeClass).toSp()
    }
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
    WithFontSize(scale.apply(sizeClass), lineHeight, baseStyle, content)
}