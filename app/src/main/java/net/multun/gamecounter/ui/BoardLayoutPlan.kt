package net.multun.gamecounter.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList


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

// slot numbers => iterated in order during rendering
// slot order => mapping from visual order to slot id
// layout order => mapping from slot id to visual order

fun slotToLayoutOrder(vararg slotOrder: Int): ImmutableList<Int> {
    val layoutOrder = IntArray(slotOrder.size) { 0 }
    for (i in slotOrder.indices)
        layoutOrder[slotOrder[i]] = i
    return layoutOrder.toList().toPersistentList()
}

// a lookup table from slot index to item index
private val LAYOUT_ORDER = arrayOf(
    slotToLayoutOrder(),
    slotToLayoutOrder(0),
    slotToLayoutOrder(0, 1),
    slotToLayoutOrder(0, 2, 1),
    slotToLayoutOrder(0, 2, 3, 1),
    slotToLayoutOrder(0, 2, 4, 3, 1),
    slotToLayoutOrder(0, 2, 4, 5, 3, 1),
    slotToLayoutOrder(0, 2, 4, 6, 5, 3, 1),
    slotToLayoutOrder(0, 2, 4, 6, 7, 5, 3, 1),
)


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

data class VerticalLayoutPlan(
    val layoutOrder: ImmutableList<Int>,
    val rows: ImmutableList<RowType>,
    val rowWeights: List<Dp>,
) : LayoutPlan

data class HorizontalLayoutPlan(
    val layoutOrder: ImmutableList<Int>,
    val columns: ImmutableList<RowType>,
    val columnWeights: List<Dp>,
) : LayoutPlan

data object FallbackPlan : LayoutPlan

fun planLayout(itemCount: Int, maxWidth: Dp, maxHeight: Dp, padding: Dp): LayoutPlan {
    if (itemCount <= 0 || itemCount >= LAYOUTS.size)
        return FallbackPlan

    val layout = LAYOUTS[itemCount]
    val layoutOrder = LAYOUT_ORDER[itemCount]
    val layoutMinWidth = layout.minWidth(padding)

    if (maxWidth <= maxHeight) {
        // distribute handles the fallback if things don't work out on the vertical axis
        // but not on the horizontal axis.
        if (layoutMinWidth > maxWidth)
            return FallbackPlan

        val rowWeights = distribute(
            layout.map { it.minHeight() },
            layout.map { it.preferredHeight() },
            maxHeight - padding * 2,
            padding,
        )
        if (rowWeights == null)
            return FallbackPlan
        return VerticalLayoutPlan(layoutOrder, layout, rowWeights)
    } else {
        // HorizontalLayout handles the fallback if things don't work out on the horizontal axis
        // but not on the vertical axis.
        if (layoutMinWidth > maxHeight)
            return FallbackPlan

        val colWeights = distribute(
            // height is used instead of width as layouts are
            // primarily vertical
            layout.map { it.minHeight() },
            layout.map { it.preferredHeight() },
            maxWidth - padding * 2,
            padding,
        )
        if (colWeights == null)
            return FallbackPlan
        return HorizontalLayoutPlan(layoutOrder, layout, colWeights)
    }
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
