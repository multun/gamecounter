package net.multun.gamecounter.ui.board

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.roundToInt


val PLAYER_MIN_HEIGHT = 150.dp
val PLAYER_MIN_WIDTH = 150.dp
val PLAYER_PREFERRED_HEIGHT = 170.dp
val PLAYER_PREFERRED_WIDTH = 210.dp


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
}


fun makeLayoutRows(players: Int): ImmutableList<RowType> {
    assert(players > 0)

    if (players == 1)
        return persistentListOf(RowType.SINGLE)
    if (players == 2)
        return persistentListOf(RowType.INVERTED_SINGLE, RowType.SINGLE)

    return buildList(players) {
        for (i in 0 until players / 2)
            add(RowType.PAIR)
        if (players % 2 != 0)
            add(RowType.SINGLE)
    }.toImmutableList()
}


// slot numbers => iterated in order during rendering
// slot order => mapping from visual order to slot id
// layout order => mapping from slot id to visual order
fun slotToLayoutOrder(slotOrder: List<Int>): ImmutableList<Int> {
    val layoutOrder = IntArray(slotOrder.size)
    for (i in slotOrder.indices)
        layoutOrder[slotOrder[i]] = i
    return layoutOrder.toList().toPersistentList()
}


fun makeLayoutOrder(players: Int): ImmutableList<Int> {
    val slotOrder = buildList(players) {
        for (even in 0 until players step 2)
            add(even)
        for (odd in (1 until players step 2).reversed())
            add(odd)
    }
    return slotToLayoutOrder(slotOrder)
}


fun ImmutableList<RowType>.minWidth(padding: Dp): Dp {
    // the min width is the widest row's width
    var maxRowWidth = this[0].minWidth(padding)
    for (rowIndex in 1 until this.size) {
        val rowWidth = this[rowIndex].minWidth(padding)
        if (rowWidth > maxRowWidth)
            maxRowWidth = rowWidth
    }
    return padding + maxRowWidth + padding
}


sealed interface LayoutPlan

data class UprightLayoutPlan(
    val itemCount: Int,
    val itemsPerRow: Int,
    val rowCount: Int,
    val rowHeight: Dp,
    // if no scrolling is needed, screen height is divided equally amongst rows
    // otherwise, the row height is set and scrolling is enabled
    val scrollingNeeded: Boolean,
) : LayoutPlan


enum class LayoutDirection {
    HORIZONTAL,
    VERTICAL,
}

data class CircularLayoutPlan(
    val direction: LayoutDirection,
    val layoutOrder: ImmutableList<Int>,
    val rows: ImmutableList<RowType>,
    val rowWeights: List<Dp>,
    val scrollingNeeded: Boolean,
) : LayoutPlan

data object FallbackPlan : LayoutPlan

fun planLayout(alwaysUprightMode: Boolean, itemCount: Int, maxWidth: Dp, maxHeight: Dp, padding: Dp): LayoutPlan {
    // if the users want all tiles to point down, lay things out as a list instead of as a circle
    if (alwaysUprightMode)
        return planUprightLayout(itemCount, maxWidth, maxHeight, padding)
    return planCircularLayout(itemCount, maxWidth, maxHeight, padding)
}

fun planUprightLayout(itemCount: Int, maxWidth: Dp, maxHeight: Dp, padding: Dp): UprightLayoutPlan {
    val availableWidth = maxWidth - padding * 2
    val availableHeight = maxHeight - padding * 2
    var rowCount = 1
    var rowSize = 1

    while (true) {
        val capacity = rowCount * rowSize
        if (capacity >= itemCount)
            break

        // not enough capacity, either add a line or increase line size
        val withMoreLines = deduceLayoutState(availableHeight, availableWidth, rowCount + 1, rowSize)
        val withMoreCols = deduceLayoutState(availableHeight, availableWidth, rowCount, rowSize + 1)

        // if we cannot add more columns while holding constraints,
        // just add lines and stop
        if (withMoreCols.slotWidth < PLAYER_MIN_WIDTH) {
            val missingCapacity = itemCount - capacity
            val requiredRows = (missingCapacity + rowSize - 1) / rowSize
            rowCount += requiredRows
            break
        }

        // prefer whatever option does not overflow, if it makes a difference
        if (!withMoreCols.overflows && withMoreLines.overflows) {
            rowSize += 1
            continue
        }
        if (!withMoreLines.overflows && withMoreCols.overflows) {
            rowCount += 1
            continue
        }

        // otherwise, select the option with the better aspect ratio
        val colsErr = (TARGET_ASPECT_RATIO - withMoreCols.aspectRatio(padding)).absoluteValue
        val linesErr = (TARGET_ASPECT_RATIO - withMoreLines.aspectRatio(padding)).absoluteValue
        if (colsErr < linesErr) {
            rowSize += 1
        } else {
            rowCount += 1
        }
    }

    val layoutState = deduceLayoutState(availableHeight, availableWidth, rowCount, rowSize)
    return UprightLayoutPlan(
        itemCount,
        rowSize,
        rowCount,
        layoutState.lineHeight,
        layoutState.overflows,
    )
}

val TARGET_ASPECT_RATIO = PLAYER_PREFERRED_WIDTH / PLAYER_PREFERRED_HEIGHT

data class LayoutState(
    val lineHeight: Dp, // excluding padding
    val slotWidth: Dp, // excluding padding
    val overflows: Boolean,
) {
    fun aspectRatio(padding: Dp): Float {
        val height = lineHeight - padding * 2
        val width = slotWidth - padding * 2
        return width / height
    }
}

fun deduceLayoutState(
    availableHeight: Dp,
    availableWidth: Dp,
    rowCount: Int,
    rowSize: Int,
): LayoutState {
    var overflows = false
    var lineHeight = availableHeight / rowCount
    if (lineHeight < PLAYER_MIN_HEIGHT) {
        lineHeight = PLAYER_MIN_HEIGHT
        overflows = true
    }

    return LayoutState(
        lineHeight = lineHeight,
        slotWidth = availableWidth / rowSize,
        overflows = overflows,
    )
}

fun planCircularLayout(
    itemCount: Int,
    maxWidth: Dp,
    maxHeight: Dp,
    padding: Dp,
): CircularLayoutPlan {
    val layout = makeLayoutRows(itemCount)
    val layoutOrder = makeLayoutOrder(itemCount)

    val smallerDimension: LayoutDirection
    val smallerDimensionSize: Dp
    val biggerDimension: LayoutDirection
    val biggerDimensionSize: Dp
    if (maxWidth <= maxHeight) {
        smallerDimension = LayoutDirection.HORIZONTAL
        smallerDimensionSize = maxWidth
        biggerDimension = LayoutDirection.VERTICAL
        biggerDimensionSize = maxHeight
    } else {
        smallerDimension = LayoutDirection.VERTICAL
        smallerDimensionSize = maxHeight
        biggerDimension = LayoutDirection.HORIZONTAL
        biggerDimensionSize = maxWidth
    }

    val layoutMinWidth = layout.minWidth(padding)
    // if the layout width fits within the smallest dimension,
    val mainAxis = if (layoutMinWidth <= smallerDimensionSize)
        // align the layout with the larger dimension
        biggerDimension
    else
        // otherwise, align the layout with the smaller dimension
        smallerDimension

    val distribution = distribute(
        // height is used as layouts are assumed to be vertical
        minimum = layout.map { it.minHeight() },
        preferred = layout.map { it.preferredHeight() },
        availableSpace = biggerDimensionSize - padding * 2,
        padding = padding,
    )
    return CircularLayoutPlan(
        mainAxis,
        layoutOrder,
        layout,
        distribution.rowSizes,
        distribution.overflows,
    )
}


// if the distribution overflows, scrolling will be required to display all items
data class Distribution(val overflows: Boolean, val rowSizes: List<Dp>)

// given an available height, allocate room for each row. Rows can be at most as big as their
// preferred size, and at least as big as their minimum size. When there isn't enough room for the
// preferred size, distribute the loss equally between rows.
fun distribute(
    minimum: List<Dp>,
    preferred: List<Dp>,
    availableSpace: Dp,
    padding: Dp, // padding is accounted for on each side of each item
): Distribution {
    assert(minimum.size == preferred.size)
    val itemCount = minimum.size
    val itemsMinimumSpace = minimum.reduce { acc, dp -> acc + dp }
    var itemsAvailableSpace = availableSpace - (padding * 2) * itemCount
    val itemsPreferredSpace = preferred.reduce { acc, dp -> acc + dp }

    // if there is not enough room, pretend there is enough
    var overflows = false
    if (itemsAvailableSpace < itemsMinimumSpace) {
        itemsAvailableSpace = itemsMinimumSpace
        overflows = true
    }

    assert(itemsMinimumSpace < itemsPreferredSpace)
    assert(itemsMinimumSpace <= itemsAvailableSpace)

    val spareRoom = itemsAvailableSpace - itemsPreferredSpace
    if (spareRoom >= 0.dp) {
        // distribute the extra spare room equally between all rows
        val spareRoomPerItem = spareRoom / itemCount
        assert(!overflows) // we can't possibly have room to spare if we overflew
        return Distribution(false, preferred.map { it + spareRoomPerItem + padding * 2 })
    }

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
    for (rowIndex in 0 until itemCount) {
        val wiggleRoom = preferred[rowIndex] - minimum[rowIndex]
        val corrected = minimum[rowIndex] + wiggleRoom * x
        res.add(corrected + padding * 2)
    }
    return Distribution(overflows, res)
}

// given the minimum space for an item, figure out how many such items fit within a row
fun fitItems(
    availableSpace: Dp,
    minimumItemSize: Dp,
    padding: Dp, // padding is accounted for on each side of each item
): Int {
    val itemMinPaddedSize = minimumItemSize + padding * 2
    return floor(availableSpace / itemMinPaddedSize).roundToInt()
}