@file:OptIn(ExperimentalLayoutApi::class)

package net.multun.gamecounter.components

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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val PLAYER_MIN_HEIGHT = 150.dp
private val PLAYER_MIN_WIDTH = 150.dp
private val PLAYER_PREFERRED_HEIGHT = 170.dp
private val PLAYER_PREFERRED_WIDTH = 210.dp


enum class RowType(val orientations: ImmutableList<Rotation>) {
    PAIR(persistentListOf(Rotation.ROT_90, Rotation.ROT_270)),
    SINGLE(persistentListOf(Rotation.ROT_0)),
    INVERTED_SINGLE(persistentListOf(Rotation.ROT_180));

    val slotCount get() = orientations.size

    fun minHeight(): Dp {
        return when (this) {
            PAIR -> PLAYER_MIN_WIDTH
            SINGLE -> PLAYER_MIN_HEIGHT
            INVERTED_SINGLE -> PLAYER_MIN_HEIGHT
        }
    }

    fun preferredHeight(): Dp {
        return when (this) {
            PAIR -> PLAYER_PREFERRED_WIDTH
            SINGLE -> PLAYER_PREFERRED_HEIGHT
            INVERTED_SINGLE -> PLAYER_PREFERRED_HEIGHT
        }
    }

    fun minWidth(padding: Dp): Dp {
        return when (this) {
            PAIR -> padding + PLAYER_MIN_HEIGHT * 2
            SINGLE -> PLAYER_MIN_WIDTH
            INVERTED_SINGLE -> PLAYER_MIN_WIDTH
        }
    }

    fun preferredWidth(padding: Dp): Dp {
        return when (this) {
            PAIR -> padding + PLAYER_PREFERRED_HEIGHT * 2
            SINGLE -> PLAYER_PREFERRED_WIDTH
            INVERTED_SINGLE -> PLAYER_PREFERRED_WIDTH
        }
    }
}

typealias Layout = ImmutableList<RowType>

fun Layout.minWidth(padding: Dp): Dp {
    // the min width is the widest row's width
    var maxRowWidth = this[0].minWidth(padding)
    for (rowIndex in 1 until this.size) {
        val rowWidth = this[rowIndex].minWidth(padding)
        if (rowWidth > maxRowWidth)
            maxRowWidth = rowWidth
    }
    return padding + maxRowWidth + padding
}

// given an available height, allocate room for each row. Rows can be at most as big as their
// preferred size, and at least as big as their minimum size. When there isn't enough room for the
// preferred size, distribute the loss equally between rows.
fun distribute(
    minimum: List<Dp>,
    preferred: List<Dp>,
    availableSpace: Dp,
    padding: Dp, // padding is accounted for on each side of each item
): List<Dp>? {
    assert(minimum.size == preferred.size)
    val size = minimum.size
    val itemsAvailableSpace = availableSpace - padding * (size * 2)
    val itemsMinimumSpace = minimum.reduce { acc, dp -> acc + dp }
    val itemsPreferredSpace = preferred.reduce { acc, dp -> acc + dp }

    if (itemsAvailableSpace < itemsMinimumSpace)
        return null

    assert(itemsMinimumSpace < itemsPreferredSpace)
    assert(itemsMinimumSpace < itemsAvailableSpace)

    val spareRoom = itemsAvailableSpace - itemsPreferredSpace
    if (spareRoom >= 0.dp) {
        // distribute the extra spare room equally between all rows
        val spareRoomPerItem = spareRoom / size
        return preferred.map { it + spareRoomPerItem + padding * 2 }
    } else {
        // the available space is less than preferred but more than minimum.
        // ---+         minimum
        // --------+    available
        // -----------+ preferred
        //    +====+==+ ratio
        // consider x, such that sum(lerp(item.min, item.preferred, x) for every item) == available

        val ma = itemsAvailableSpace - itemsMinimumSpace
        val mp = itemsPreferredSpace - itemsMinimumSpace
        val x = ma / mp
        val res = mutableListOf<Dp>()
        for (rowIndex in 0 until size) {
            val wiggleRoom = preferred[rowIndex] - minimum[rowIndex]
            val corrected = minimum[rowIndex] + wiggleRoom * x
            res.add(corrected + padding * 2)
        }
        return res
    }
}

private val LAYOUTS = arrayOf(
    persistentListOf(),
    // 0
    persistentListOf(RowType.SINGLE),

    // 0 1
    persistentListOf(RowType.INVERTED_SINGLE, RowType.SINGLE),

    // 0
    // 1 2
    persistentListOf(RowType.PAIR, RowType.SINGLE),

    // 0 2
    // 1 3
    persistentListOf(RowType.PAIR, RowType.PAIR),

    // 0 2
    // 1 3 4
    persistentListOf(RowType.PAIR, RowType.PAIR, RowType.SINGLE),

    // 0 2 4
    // 1 3 5
    persistentListOf(RowType.PAIR, RowType.PAIR, RowType.PAIR),

    // 0 2 4
    // 1 3 5 6
    persistentListOf(RowType.PAIR, RowType.PAIR, RowType.PAIR, RowType.SINGLE),

    // 0 2 4 6
    // 1 3 5 7
    persistentListOf(RowType.PAIR, RowType.PAIR, RowType.PAIR, RowType.PAIR),
)

// a lookup table from slot index to item index
private val LAYOUT_ORDER = arrayOf(
    persistentListOf(),
    persistentListOf(0),
    persistentListOf(0, 1),
    persistentListOf(0, 2, 1),
    persistentListOf(0, 2, 3, 1),
    persistentListOf(0, 2, 4, 3, 1),
    persistentListOf(0, 2, 4, 5, 3, 1),
    persistentListOf(0, 2, 4, 6, 5, 3, 1),
    persistentListOf(0, 2, 4, 6, 7, 5, 3, 1),
)


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
            space = padding,
            alignment = Alignment.CenterHorizontally
        ),
        verticalArrangement = Arrangement.spacedBy(
            space = padding,
            alignment = Alignment.CenterVertically
        ),
    ) {
        for (itemIndex in 0 until itemCount)
            callback(itemIndex, Modifier)
    }
}

@Composable
fun VerticalLayout(
    layout: Layout,
    availableHeight: Dp,
    layoutOrder: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    callback: @Composable (Int, Modifier) -> Unit,
) {
    val rowWeights = distribute(
        layout.map { it.minHeight() },
        layout.map { it.preferredHeight() },
        availableHeight,
        padding,
    )

    if (rowWeights == null) {
        FallbackLayout(itemCount = layoutOrder.size, callback = callback, padding = padding, modifier = Modifier.fillMaxSize())
        return
    }

    Column(modifier = modifier) {
        var rowOffsetCursor = 0
        for ((rowIndex, rowType) in layout.withIndex()) {
            val rowOffset = rowOffsetCursor
            rowOffsetCursor += rowType.slotCount

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(rowWeights[rowIndex].value),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (colIndex in 0 until rowType.slotCount) {
                    val orientation = rowType.orientations[colIndex]
                    val slotIndex = rowOffset + colIndex
                    val slotModifier = Modifier
                        .weight(1f)
                        .padding(padding / 2)
                        .rotateLayout(orientation)
                    callback(layoutOrder[slotIndex], slotModifier)
                }
            }
        }
    }
}

@Composable
fun HorizontalLayout(
    layout: Layout,
    availableWidth: Dp,
    layoutOrder: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    callback: @Composable (Int, Modifier) -> Unit,
) {
    val colWeights = distribute(
        // height is used instead of width as layouts are
        // primarily vertical
        layout.map { it.minHeight() },
        layout.map { it.preferredHeight() },
        availableWidth,
        padding,
    )

    if (colWeights == null) {
        FallbackLayout(itemCount = layoutOrder.size, callback = callback, padding = padding, modifier = Modifier.fillMaxSize())
        return
    }

    Row(modifier = modifier) {
        var rowOffsetCursor = 0
        for ((rowIndex, rowType) in layout.withIndex()) {
            val rowOffset = rowOffsetCursor
            rowOffsetCursor += rowType.slotCount

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(colWeights[rowIndex].value),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (colIndex in (0 until rowType.slotCount).reversed()) {
                    val orientation = rowType.orientations[colIndex] + Rotation.ROT_270
                    val slotIndex = rowOffset + colIndex
                    val slotModifier = Modifier
                        .weight(1f)
                        .padding(padding / 2)
                        .rotateLayout(orientation)
                    callback(layoutOrder[slotIndex], slotModifier)
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
    if (itemCount == 0)
        return

    if (itemCount >= LAYOUTS.size) {
        FallbackLayout(itemCount = itemCount, callback = callback, modifier = modifier, padding = padding)
        return
    }

    BoxWithConstraints(modifier = modifier) {
        val layout = LAYOUTS[itemCount]
        val layoutOrder = LAYOUT_ORDER[itemCount]
        val layoutMinWidth = layout.minWidth(padding)
        Log.i(TAG, "canvas dimensions: minWidth: ${this.minWidth} maxWidth: ${this.maxWidth} minHeight: ${this.minHeight} maxHeight: ${this.maxHeight}")
        if (this.maxWidth <= this.maxHeight) {
            // VerticalLayout handles the fallback if things don't work out on the vertical axis
            // but not on the horizontal axis.
            if (layoutMinWidth > this.maxWidth)
                FallbackLayout(itemCount = itemCount, callback = callback, modifier = Modifier.fillMaxSize(), padding = padding)
            else
                VerticalLayout(layout, this.maxHeight, layoutOrder, Modifier, padding, callback)
        } else {
            // HorizontalLayout handles the fallback if things don't work out on the horizontal axis
            // but not on the vertical axis.
            if (layoutMinWidth > this.maxHeight)
                FallbackLayout(itemCount = itemCount, callback = callback, modifier = Modifier.fillMaxSize(), padding = padding)
            else
                HorizontalLayout(layout, this.maxWidth, layoutOrder, Modifier, padding, callback)
        }
    }
}

