package net.multun.gamecounter.ui.board

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints


@Immutable
enum class Rotation(private val index: Int, val degrees: Float) {
    ROT_0(0, 0f),
    ROT_90(1, 90f),
    ROT_180(2, 180f),
    ROT_270(3, 270f);

    operator fun plus(rotation: Rotation): Rotation {
        return when ((this.index + rotation.index) % 4) {
            0 -> ROT_0
            1 -> ROT_90
            2 -> ROT_180
            3 -> ROT_270
            else -> error("")
        }
    }
}

/**
 * Rotates the composable by 90 degrees increments, taking layout into account: the composable
 * is rendered taking into account the fact usable space changes as the composable rotates.
 *
 * Usage of this API renders this composable into a separate graphics layer.

 * @see Modifier.rotate
 * @see graphicsLayer
 */
fun Modifier.rotateLayout(rotation: Rotation): Modifier {
    return when (rotation) {
        // rotate does not
        Rotation.ROT_0, Rotation.ROT_180 -> this
        Rotation.ROT_90, Rotation.ROT_270 -> then(HorizontalLayoutModifier)
    }.rotate(rotation.degrees)
}

/** Swap horizontal and vertical constraints */
private fun Constraints.transpose(): Constraints {
    return copy(
        minWidth = minHeight,
        maxWidth = maxHeight,
        minHeight = minWidth,
        maxHeight = maxWidth
    )
}

private object HorizontalLayoutModifier : LayoutModifier {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints.transpose())
        return layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2)
            )
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
        return measurable.maxIntrinsicWidth(width)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
        return measurable.maxIntrinsicWidth(width)
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
        return measurable.minIntrinsicHeight(height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
        return measurable.maxIntrinsicHeight(height)
    }
}