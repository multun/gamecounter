package net.multun.gamecounter.ui.board

import android.icu.number.NumberFormatter
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import java.util.Locale
import kotlin.streams.toList


fun formatNumber(number: Int, pattern: String, locale: Locale = Locale.getDefault()): String {
    try {
        val formatter = android.icu.text.MessageFormat(pattern, locale)
        return formatter.format(arrayOf(number))
    } catch (e: IllegalArgumentException) {
        return number.toString()
    }
}

fun formatOrdinal(number: Int, locale: Locale = Locale.getDefault()): String {
    return formatNumber(number, "{0,ordinal}", locale)
}

fun formatInteger(number: Int, locale: Locale = Locale.getDefault()): String {
    return formatNumber(number, "{0,number,integer}", locale)
}

fun formatCombo(number: Int, locale: Locale = Locale.getDefault()): String {
    // we can't use {0, number, :: +?}, as this is not available in nougat
    val formatter = DecimalFormat("0", DecimalFormatSymbols(locale))
    if (number != 0)
        formatter.positivePrefix = "+"
    return formatter.format(number)
}

@Composable
fun ordinalAnnotatedString(ordinal: String, ordFontSize: TextUnit): AnnotatedString {
    val codePoints = ordinal.codePoints().toList()
    val firstDigitCPIndex = codePoints.indexOfFirst { Character.isDigit(it) }
    val lastDigitCPIndex = codePoints.indexOfLast { Character.isDigit(it) }
    val digitsStart = ordinal.offsetByCodePoints(0, firstDigitCPIndex)
    val digitsEnd = ordinal.offsetByCodePoints(digitsStart, lastDigitCPIndex - digitsStart + 1)

    val prefix = ordinal.substring(0, digitsStart)
    val digits = ordinal.substring(digitsStart, digitsEnd)
    val suffix = ordinal.substring(digitsEnd, ordinal.length)

    val smallTextStyle = SpanStyle(
        fontSize = ordFontSize,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    return buildAnnotatedString {
        withStyle(style = smallTextStyle) {
            append(prefix)
        }
        append(digits)
        withStyle(style = smallTextStyle) {
            append(suffix)
        }
    }
}
