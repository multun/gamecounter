package net.multun.gamecounter.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.em
import com.sd.lib.compose.wheel_picker.FWheelPickerState
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import net.multun.gamecounter.R
import net.multun.gamecounter.store.CounterId
import kotlin.math.max

@Composable
fun PlayerCounterUpdateMenu(
    signState: FWheelPickerState,
    player: CounterCardUIState,
    counterScale: FontScale,
    onUpdateCounter: (CounterId, Int) -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val textMeasurer = rememberTextMeasurer()

        PlayerTopRow(player.name, counterScale, Modifier.align(Alignment.TopStart)) {
            PlayerTopRowButton(muted = false, onClick = onClose) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.close_menu)
                )
            }
        }

        val integersText = (0 until 10).map { formatInteger(it) }
        val charSet = listOf("+", "-") + integersText
        val digits = (0 until 4).map { rememberFWheelPickerState(cyclicalStartingPoint(10)) }
        WithScaledFontSize(counterScale, UPDATE_CARD_TEXT, lineHeight = 1f) {
            // measure the size of text
            val minSizePx = charSet
                .map { textMeasurer.measure(it, LocalTextStyle.current).size }
                .reduce { a, b -> IntSize(
                    width = max(a.width, b.width),
                    height = max(a.height, b.height)
                )
                }
            val minSizeDp = with(LocalDensity.current) { DpSize(
                width = minSizePx.width.toDp(),
                height = minSizePx.height.toDp())
            }

            Row {
                FCyclicalVerticalWheelPicker(
                    state = signState,
                    itemHeight = minSizeDp.height,
                    unfocusedCount = 1,
                    focus = {},
                    modifier = Modifier.width(minSizeDp.width)
                ) { index ->
                    Text(if (index % 2 == 0) "+" else "-", overflow = TextOverflow.Visible, textAlign = TextAlign.Center)
                }

                for (i in digits.indices) {
                    FCyclicalVerticalWheelPicker(
                        state = digits[i],
                        itemHeight = minSizeDp.height,
                        unfocusedCount = 1,
                        focus = {},
                        modifier = Modifier.width(minSizeDp.width)
                    ) { index ->
                        Text(integersText[index % 10], overflow = TextOverflow.Visible, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        IconButton(onClick = {
            var total = 0
            for (digit in digits) {
                total = total * 10 + digit.currentIndex % 10
            }
            if ((signState.currentIndex % 2) != 0)
                total = -total
            onUpdateCounter(player.selectedCounter, total)
            onClose()
        }, Modifier.align(Alignment.BottomEnd)) {
            Icon(Icons.Filled.Check, null)
        }
    }
}