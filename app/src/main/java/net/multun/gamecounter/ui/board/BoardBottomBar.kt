package net.multun.gamecounter.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.wheel_picker.FHorizontalWheelPicker
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import net.multun.gamecounter.R


val BOTTOM_BAR_PADDING_SIDES = 12.dp


object BottomBarDefaults {
    val insets: WindowInsets
        @Composable get() = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
}

@Composable
fun BottomBar(
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    windowInsets: WindowInsets,
    content: @Composable (RowScope.() -> Unit),
) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(windowInsets)
            .padding(BOTTOM_BAR_PADDING_SIDES, 0.dp, BOTTOM_BAR_PADDING_SIDES, 0.dp),
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

@Composable
fun CounterBottomBar(
    onRoll: () -> Unit,
    onOpenSettings: () -> Unit,
    windowInsets: WindowInsets = BottomBarDefaults.insets,
) {
    BottomBar(windowInsets = windowInsets) {
        IconButton(onClick = onRoll) {
            Icon(Icons.Filled.Casino, contentDescription = stringResource(R.string.roll_dice))
        }

        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
        }
    }
}


@Composable
fun PlayerNamesBottomBar(
    onClear: () -> Unit,
    windowInsets: WindowInsets = BottomBarDefaults.insets,
) {
    BottomBar(windowInsets = windowInsets, horizontalArrangement = Arrangement.End) {
        IconButton(onClick = onClear) {
            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_player_names))
        }
    }
}

private val DICE_OPTIONS = listOf(0, 4, 6, 8, 10, 12, 20, 30, 100)

@Composable
fun RollBottomBar(
    initialSelectedDice: Int,
    onSelectDice: (Int) -> Unit,
    onRoll: () -> Unit,
    onClear: () -> Unit,
    windowInsets: WindowInsets = BottomBarDefaults.insets,
) {
    BottomBar(windowInsets = windowInsets) {
        val initialIndex = remember { DICE_OPTIONS.indexOfFirst { it == initialSelectedDice } }

        val state = rememberFWheelPickerState(initialIndex)
        // Observe currentIndex.
        LaunchedEffect(state) {
            snapshotFlow { state.currentIndex }
                .collect {
                    if (it != -1)
                        onSelectDice(DICE_OPTIONS[it])
                }
        }

        IconButton(onClick = onRoll) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.roll_dice))
        }

        FHorizontalWheelPicker(
            modifier = Modifier.height(48.dp),
            state = state,
            count = DICE_OPTIONS.size,
        ) { index ->
            val diceSize = DICE_OPTIONS[index]
            if (diceSize == 0)
                Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.player_order))
            else
                Text(diceSize.toString())
        }

        IconButton(onClick = onClear) {
            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_dice_roll))
        }
    }
}
