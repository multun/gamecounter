@file:OptIn(ExperimentalLayoutApi::class)

package net.multun.gamecounter.ui.board

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.sizeIn
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
            callback(itemIndex, Modifier.sizeIn(
                minWidth = PLAYER_MIN_WIDTH,
                maxWidth = PLAYER_MIN_WIDTH,
                minHeight = PLAYER_MIN_HEIGHT,
                maxHeight = PLAYER_MIN_HEIGHT,
            ))
    }
}


@Composable
fun ListLayout(
    plan: UprightLayoutPlan,
    callback: @Composable (Int, Modifier) -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
) {
    var colModifier = modifier.padding(padding)
    colModifier = if (plan.scrollingNeeded) {
        colModifier.verticalScroll(rememberScrollState())
    } else {
        colModifier.fillMaxSize()
    }

    Column(modifier = colModifier) {
        val rowModifier = if (plan.scrollingNeeded)
            Modifier.height(plan.rowHeight)
        else
            Modifier.weight(1f)

        for (rowIndex in 0 until plan.rowCount)
            Row(modifier = rowModifier) {
                val rowOffset = rowIndex * plan.itemsPerRow
                val rowSize = (plan.itemCount - rowOffset).coerceAtMost(plan.itemsPerRow)
                for (itemIndex in rowOffset until rowOffset + rowSize)
                    callback(itemIndex, Modifier
                        .padding(padding)
                        .fillMaxHeight()
                        .weight(1f))
            }
    }
}

@Composable
fun VerticalLayout(
    plan: CircularLayoutPlan,
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
    plan: CircularLayoutPlan,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    callback: @Composable (Int, Modifier) -> Unit,
) {
    Row(modifier = modifier.padding(padding)) {
        var rowOffsetCursor = 0
        for ((rowIndex, rowType) in plan.rows.withIndex()) {
            val rowOffset = rowOffsetCursor
            rowOffsetCursor += rowType.slotCount

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(plan.rowWeights[rowIndex].value),
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
    alwaysUprightMode: Boolean = false,
    padding: Dp = 8.dp,
    callback: @Composable (Int, Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        Log.d(TAG, "canvas dimensions: minWidth: ${this.minWidth} maxWidth: ${this.maxWidth} minHeight: ${this.minHeight} maxHeight: ${this.maxHeight}")
        when (val layoutPlan = planLayout(alwaysUprightMode, itemCount, this.maxWidth, this.maxHeight, padding)) {
            FallbackPlan -> FallbackLayout(
                itemCount = itemCount,
                callback = callback,
                modifier = Modifier.fillMaxSize(),
                padding = padding
            )
            is CircularLayoutPlan -> {
                when (layoutPlan.direction) {
                    LayoutDirection.HORIZONTAL -> {
                        var layoutModifier: Modifier = Modifier
                        var boxModifier: Modifier = Modifier
                        if (layoutPlan.scrollingNeeded) {
                            val minWidth = layoutPlan.rowWeights.reduce { acc, dp -> acc + dp }
                            layoutModifier = Modifier.requiredWidth(minWidth)
                            boxModifier = Modifier.horizontalScroll(rememberScrollState())
                        }
                        Box(modifier = boxModifier) {
                            HorizontalLayout(
                                plan = layoutPlan,
                                padding = padding,
                                callback = callback,
                                modifier = layoutModifier,
                            )
                        }
                    }
                    LayoutDirection.VERTICAL -> {
                        var layoutModifier: Modifier = Modifier
                        var boxModifier: Modifier = Modifier
                        if (layoutPlan.scrollingNeeded) {
                            val minHeight = layoutPlan.rowWeights.reduce { acc, dp -> acc + dp }
                            layoutModifier = Modifier.requiredHeight(minHeight)
                            boxModifier = Modifier.verticalScroll(rememberScrollState())
                        }
                        Box(modifier = boxModifier) {
                            VerticalLayout(
                                plan = layoutPlan,
                                padding = padding,
                                callback = callback,
                                modifier = layoutModifier,
                            )
                        }
                    }
                }
            }
            is UprightLayoutPlan -> {
                Log.i("ListLayout", layoutPlan.toString())
                ListLayout(
                    plan = layoutPlan,
                    padding = padding,
                    callback = callback,
                )
            }
        }
    }
}

