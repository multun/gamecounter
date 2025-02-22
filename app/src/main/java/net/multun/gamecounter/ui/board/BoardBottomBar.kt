package net.multun.gamecounter.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.wheel_picker.FHorizontalWheelPicker
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState


val BOTTOM_BAR_PADDING = 12.dp

@Composable
fun BottomBar(
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    content: @Composable (RowScope.() -> Unit)
) {
    Row(
        Modifier.fillMaxWidth().padding(BOTTOM_BAR_PADDING, 0.dp, BOTTOM_BAR_PADDING, BOTTOM_BAR_PADDING),
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

@Composable
fun CounterBottomBar(onRoll: () -> Unit, onOpenSettings: () -> Unit) {
    BottomBar {
        IconButton(onClick = onRoll) {
            Icon(Icons.Filled.Casino, contentDescription = "Roll a dice")
        }

        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
        }
    }
}

private val DICE_OPTIONS = listOf(0, 4, 6, 8, 10, 12, 20, 30, 100)

@Composable
fun RollBottomBar(viewModel: BoardViewModel, initialSelectedDice: Int) {
    BottomBar {
        val initialIndex = remember { DICE_OPTIONS.indexOfFirst { it == initialSelectedDice } }

        val state = rememberFWheelPickerState(initialIndex)
        // Observe currentIndex.
        LaunchedEffect(state) {
            snapshotFlow { state.currentIndex }
                .collect {
                    if (it != -1)
                        viewModel.selectDice(DICE_OPTIONS[it])
                }
        }

        IconButton(onClick = { viewModel.roll() }) {
            Icon(Icons.Filled.Casino, contentDescription = "Roll a dice")
        }

        FHorizontalWheelPicker(
            modifier = Modifier.height(48.dp),
            state = state,
            count = DICE_OPTIONS.size,
        ) { index ->
            val diceSize = DICE_OPTIONS[index]
            if (diceSize == 0)
                Icon(Icons.Filled.Person, contentDescription = "player order")
            else
                Text(diceSize.toString())
        }

        IconButton(onClick = { viewModel.clearRoll() }) {
            Icon(Icons.Filled.Clear, contentDescription = "Settings")
        }
    }
}
