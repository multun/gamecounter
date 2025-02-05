@file:OptIn(ExperimentalLayoutApi::class)

package net.multun.gamecounter.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun FallbackLayout(
    itemCount: Int,
    callback: @Composable (Int, Modifier) -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
) {
    FlowRow(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(
            space = padding * 2,
            alignment = Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(
            space = padding * 2,
            alignment = Alignment.CenterVertically
        ),
    ) {
        for (itemIndex in 0 until itemCount)
            callback(itemIndex, Modifier)
    }
}

@Composable
fun VerticalLayout(
    plan: VerticalLayoutPlan,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    callback: @Composable (Int, Modifier) -> Unit,
) {
    Column(modifier = modifier.padding(padding)) {
        var rowOffsetCursor = 0
        for ((rowIndex, rowType) in plan.rows.withIndex()) {
            val rowOffset = rowOffsetCursor
            rowOffsetCursor += rowType.slotCount

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(plan.rowWeights[rowIndex].value),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (colIndex in 0 until rowType.slotCount) {
                    val orientation = rowType.orientations[colIndex]
                    val slotIndex = rowOffset + colIndex
                    val slotModifier = Modifier
                        .weight(1f)
                        .padding(padding)
                        .rotateLayout(orientation)
                    callback(plan.layoutOrder[slotIndex], slotModifier)
                }
            }
        }
    }
}

@Composable
fun HorizontalLayout(
    plan: HorizontalLayoutPlan,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    callback: @Composable (Int, Modifier) -> Unit,
) {
    Row(modifier = modifier.padding(padding)) {
        var rowOffsetCursor = 0
        for ((rowIndex, rowType) in plan.columns.withIndex()) {
            val rowOffset = rowOffsetCursor
            rowOffsetCursor += rowType.slotCount

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(plan.columnWeights[rowIndex].value),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (colIndex in (0 until rowType.slotCount).reversed()) {
                    val orientation = rowType.orientations[colIndex] + Rotation.ROT_270
                    val slotIndex = rowOffset + colIndex
                    val slotModifier = Modifier
                        .weight(1f)
                        .padding(padding)
                        .rotateLayout(orientation)
                    callback(plan.layoutOrder[slotIndex], slotModifier)
                }
            }
        }
    }
}

private const val TAG = "BoardLayout"


// TODO: investigate using FlowRow and animatePlacement:
//   https://developer.android.com/develop/ui/compose/layouts/flow
//   https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/package-summary#(androidx.compose.ui.Modifier).onPlaced(kotlin.Function1)
@Composable
fun BoardLayout(
    itemCount: Int,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    callback: @Composable (Int, Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        Log.i(TAG, "canvas dimensions: minWidth: ${this.minWidth} maxWidth: ${this.maxWidth} minHeight: ${this.minHeight} maxHeight: ${this.maxHeight}")
        when (val layoutPlan = planLayout(itemCount, this.maxWidth, this.maxHeight, padding)) {
            FallbackPlan -> FallbackLayout(
                itemCount = itemCount,
                callback = callback,
                modifier = Modifier.fillMaxSize(),
                padding = padding
            )
            is HorizontalLayoutPlan -> HorizontalLayout(
                plan = layoutPlan,
                padding = padding,
                callback = callback,
            )
            is VerticalLayoutPlan -> VerticalLayout(
                plan = layoutPlan,
                padding = padding,
                callback = callback,
            )
        }
    }
}

