package net.multun.gamecounter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.multun.gamecounter.BoardUIState
import net.multun.gamecounter.BoardViewModel



// TODO: investigate using FlowRow and animatePlacement:
//   https://developer.android.com/develop/ui/compose/layouts/flow
//   https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/package-summary#(androidx.compose.ui.Modifier).onPlaced(kotlin.Function1)
@Composable
fun BoardLayout(
    slots: Int,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    callback: @Composable (Int, Modifier) -> Unit,
) {
    val playersPerRow: Int
    val rowCount: Int
    if (slots == 2) {
        playersPerRow = 1
        rowCount = 2
    } else {
        playersPerRow = 2
        rowCount = (slots + 1) / 2
    }

    Column(modifier = modifier.padding(padding / 2)) {
        for (rowIndex in 0 until rowCount) {
            val rowOffset = rowIndex * playersPerRow
            val remainingSlots = slots - rowOffset
            val rowSlots = if (remainingSlots > playersPerRow) playersPerRow else remainingSlots
            Row(modifier = Modifier
                .padding(padding / 2)
                .fillMaxSize()
                .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(padding),
                verticalAlignment = Alignment.CenterVertically,
                ) {
                for (colIndex in 0 until rowSlots) {
                    val slotIndex = rowOffset + colIndex
                    val orientation = when {
                        slots == 2 && slotIndex == 0 -> Rotation.ROT_180
                        rowSlots == 1 || slots == 2 -> Rotation.ROT_0
                        colIndex == 0 -> Rotation.ROT_90
                        else -> Rotation.ROT_270
                    }

                    val slotModifier = Modifier
                        .weight(1f)
                        .rotateLayout(orientation)
                    callback(slotIndex, slotModifier)
                }
            }
        }
    }
}

