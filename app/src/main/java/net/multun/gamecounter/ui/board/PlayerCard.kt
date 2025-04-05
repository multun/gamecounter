package net.multun.gamecounter.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.wheel_picker.DefaultWheelPickerDisplay
import com.sd.lib.compose.wheel_picker.FVerticalWheelPicker
import com.sd.lib.compose.wheel_picker.FWheelPickerContentScope
import com.sd.lib.compose.wheel_picker.FWheelPickerDisplayScope
import com.sd.lib.compose.wheel_picker.FWheelPickerFocusVertical
import com.sd.lib.compose.wheel_picker.FWheelPickerState
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import net.multun.gamecounter.PaletteColor
import net.multun.gamecounter.R
import net.multun.gamecounter.store.CounterId
import net.multun.gamecounter.store.PlayerId
import net.multun.gamecounter.toDisplayColor


val MAIN_CARD_TEXT = FontSizeClass(base = 45.dp, max = 112.5.dp)
val UPDATE_CARD_TEXT = FontSizeClass(base = 35.dp, max = 87.5.dp)
val ORDINAL_CARD_TEXT = FontSizeClass(base = 30.dp, max = 75.dp)
val SUB_CARD_TEXT = FontSizeClass(base = 18.dp, max = 28.dp)
val EXP_CARD_TEXT = FontSizeClass(base = 18.dp, max = 45.dp)
val NAME_CARD_TEXT = FontSizeClass(base = 14.dp, max = 20.dp)


enum class PlayerModal {
    SETTINGS,
    COUNTER_UPDATE,
}

fun cyclicalStartingPoint(cycleLength: Int): Int {
    val midpoint = Int.MAX_VALUE / 2
    return midpoint - midpoint % cycleLength
}

@Composable
fun FCyclicalVerticalWheelPicker(
    modifier: Modifier = Modifier,
    state: FWheelPickerState = rememberFWheelPickerState(),
    key: ((index: Int) -> Any)? = null,
    itemHeight: Dp = 35.dp,
    unfocusedCount: Int = 2,
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    debug: Boolean = false,
    focus: @Composable () -> Unit = { FWheelPickerFocusVertical() },
    display: @Composable FWheelPickerDisplayScope.(index: Int) -> Unit = { DefaultWheelPickerDisplay(it) },
    content: @Composable FWheelPickerContentScope.(index: Int) -> Unit,
) {
    val fakeCount = Int.MAX_VALUE - (unfocusedCount * 2)
    FVerticalWheelPicker(
        modifier,
        fakeCount,
        state,
        key,
        itemHeight,
        unfocusedCount,
        userScrollEnabled,
        reverseLayout,
        debug,
        focus,
        display,
        content,
    )
}


@Composable
fun Player(
    player: CardUIState,
    onDelete: () -> Unit,
    onSetColor: (Color) -> Unit,
    onEditName: () -> Unit,
    onUpdateCounter: (CounterId, Int) -> Unit,
    onSelectCounter: (CounterId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var modal: PlayerModal? by rememberSaveable { mutableStateOf(null) }
    val sign = rememberFWheelPickerState(cyclicalStartingPoint(2))

    GameCard(
        baseColor = player.color,
        modifier = modifier
    ) {
        BoxWithConstraints {
            val counterScale = counterScale(this.maxWidth, this.maxHeight)
            when (player) {
                is RollCardUIState -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PlayerName(counterScale, player.name, modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth())

                        val baseStyle = LocalTextStyle.current
                        val digitsFontSize = counterScale.apply(MAIN_CARD_TEXT)
                        val ordFontSize = counterScale.apply(ORDINAL_CARD_TEXT)
                        WithFontSize(digitsFontSize, baseStyle = baseStyle) {
                            if (player.isOrdinal) {
                                val ordinal = formatOrdinal(player.roll)
                                Text(ordinalAnnotatedString(ordinal, ordFontSize))
                            } else {
                                Text(formatInteger(player.roll))
                            }
                        }
                    }
                }
                is CounterCardUIState -> {
                    if (modal == PlayerModal.SETTINGS) {
                        PlayerCardSettings(
                            currentPlayerColor = player.color,
                            onExit = { modal = null },
                            onDelete = onDelete,
                            onSetColor = onSetColor,
                            onEditName = onEditName,
                        )
                        return@BoxWithConstraints
                    }

                    if (modal == PlayerModal.COUNTER_UPDATE) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PlayerTopRow(player.name, counterScale, Modifier.align(Alignment.TopStart)) {
                                PlayerTopRowButton(muted = false, onClick = { modal = null }) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        contentDescription = null // TODO
                                    )
                                }
                            }

                            val digits = (0 until 4).map { rememberFWheelPickerState(cyclicalStartingPoint(10)) }
                            WithScaledFontSize(counterScale, UPDATE_CARD_TEXT, lineHeight = 1f) {
                                val fontSizeDp = with(LocalDensity.current) { LocalTextStyle.current.fontSize.toDp() }
                                val fontWidthDp = fontSizeDp * 0.66f // TODO: actually measure width ?

                                Row {
                                    FCyclicalVerticalWheelPicker(
                                        state = sign,
                                        itemHeight = fontSizeDp,
                                        unfocusedCount = 1,
                                        focus = {},
                                        modifier = Modifier.width(fontWidthDp)
                                    ) { index ->
                                        Text(if (index % 2 == 0) "+" else "-")
                                    }

                                    for (i in digits.indices) {
                                        FCyclicalVerticalWheelPicker(
                                            state = digits[i],
                                            itemHeight = fontSizeDp,
                                            unfocusedCount = 1,
                                            focus = {},
                                            modifier = Modifier.width(fontWidthDp)
                                        ) { index ->
                                            Text(formatInteger(index % 10))
                                        }
                                    }
                                }
                            }

                            IconButton(onClick = {
                                var total = 0
                                for (digit in digits) {
                                    total = total * 10 + digit.currentIndex % 10
                                }
                                if ((sign.currentIndex % 2) != 0)
                                    total = -total
                                onUpdateCounter(player.selectedCounter, total)
                                modal = null
                            }, Modifier.align(Alignment.BottomEnd)) {
                                Icon(Icons.Filled.Check, null)
                            }

                        }
                        return@BoxWithConstraints
                    }

                    PlayerCounter(
                        player = player,
                        modifier = Modifier.fillMaxSize(),
                        counterScale = counterScale,
                        onEditCounter = { modal = PlayerModal.COUNTER_UPDATE },
                        onUpdateCounter = onUpdateCounter,
                        onSelectCounter = onSelectCounter,
                        onEdit = { modal = PlayerModal.SETTINGS }
                    )
                }

                is PlayerNameUIState -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        WithScaledFontSize(counterScale, SUB_CARD_TEXT) {
                            Row(
                                modifier = Modifier
                                    .clickable(onClick = onEditName)
                                    .fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                val playerName = player.name
                                if (playerName.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.player_name),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = playerName,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun gameCardColors(backgroundColor: Color): CardColors {
    return CardDefaults
        .cardColors()
        .copy(containerColor = backgroundColor)
}

@Composable
fun GameCard(baseColor: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        colors = gameCardColors(baseColor.toDisplayColor())
    ) {
        content()
    }
}

// clickable variant
@Composable
fun GameCard(baseColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = gameCardColors(baseColor.toDisplayColor())
    ) {
        content()
    }
}

@Composable
fun GameButton(
    baseColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    GameCard(
        baseColor = baseColor,
        modifier = modifier,
        onClick = onClick,
        content = {
            val newTextStyle = LocalTextStyle.current.merge(MaterialTheme.typography.labelLarge)
            CompositionLocalProvider(LocalTextStyle provides newTextStyle) {
                Column(modifier = Modifier.padding(padding)) {
                    content()
                }
            }
        }
    )
}


@Composable
fun GameIconButton(
    baseColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GameButton(baseColor, onClick, modifier, 15.dp, content)
}


@Preview(widthDp = 150, heightDp = 150, fontScale = 1f)
@Composable
fun PlayerCardPreview() {
    Player(
        player = CounterCardUIState(
            id = PlayerId(0),
            color = PaletteColor.Blue.color,
            name = "Alice",
            counters = listOf(
                CounterUIState(CounterId(0), "test", 100, 1)
            ),
            selectedCounter = CounterId(0),
        ),
        onUpdateCounter = { _, _ -> },
        onSelectCounter = {},
        onDelete = {},
        onSetColor = {},
        onEditName = {}
    )
}