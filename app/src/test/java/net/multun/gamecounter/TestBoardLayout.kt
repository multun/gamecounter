package net.multun.gamecounter

import net.multun.gamecounter.ui.board.makeLayoutOrder
import net.multun.gamecounter.ui.board.slotToLayoutOrder
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TestBoardLayout {
    @Test
    fun circularLayoutOrder() {
        assert(makeLayoutOrder(1) == slotToLayoutOrder(listOf(0)))
        assert(makeLayoutOrder(2) == slotToLayoutOrder(listOf(0, 1)))
        assert(makeLayoutOrder(3) == slotToLayoutOrder(listOf(0, 2, 1)))
        assert(makeLayoutOrder(4) == slotToLayoutOrder(listOf(0, 2, 3, 1)))
        assert(makeLayoutOrder(5) == slotToLayoutOrder(listOf(0, 2, 4, 3, 1)))
    }
}