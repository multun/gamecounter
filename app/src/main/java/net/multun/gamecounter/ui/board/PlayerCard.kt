package net.multun.gamecounter.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.multun.gamecounter.DEFAULT_PALETTE
import net.multun.gamecounter.store.CounterId
import net.multun.gamecounter.store.PlayerId
import net.multun.gamecounter.toDisplayColor


@Composable
fun Player(
    player: CardUIState,
    onDelete: () -> Unit,
    onSetColor: (Color) -> Unit,
    onUpdateCounter: (CounterId, Int) -> Unit,
    onNextCounter: () -> Unit,
    onPreviousCounter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    GameCard(
        baseColor = player.color,
        modifier = modifier
    ) {
        BoxWithConstraints {
            val counterScale = counterScale(this.maxWidth, this.maxHeight)
            if (isEditing) {
                PlayerCardSettings(
                    currentPlayerColor = player.color,
                    onExit = { isEditing = false },
                    onDelete = onDelete,
                    onSetColor = onSetColor,
                )
                return@BoxWithConstraints
            }

            when (player) {
                is RollCardUIState -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val text: String
                        if (player.isOrdinal) {
                            val formatter = android.icu.text.MessageFormat("{0,ordinal}")
                            text = formatter.format(arrayOf(player.roll))
                        } else {
                            text = "${player.roll}"
                        }
                        CardMainText(text, counterScale)
                    }
                }
                is CounterCardUIState -> {
                    if (player.counter == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CardSubText("no counter", counterScale)
                        }
                        return@BoxWithConstraints
                    }

                    val counter = player.counter
                    PlayerCounter(
                        modifier = Modifier.fillMaxSize(),
                        counter = counter,
                        counterScale = counterScale,
                        onUpdateCounter = onUpdateCounter,
                        onNextCounter = onNextCounter,
                        onPreviousCounter = onPreviousCounter,
                        onEdit = { isEditing = true }
                    )
                }
            }
        }
    }
}


@Composable
fun GameCard(baseColor: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val displayColor = baseColor.toDisplayColor()
    Card(
        modifier = modifier,
        colors = CardDefaults
            .cardColors()
            .copy(containerColor = displayColor)
    ) {
        content()
    }
}

@Composable
fun GameButton(
    baseColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GameCard(
        baseColor = baseColor,
        modifier = modifier.clickable(onClick = onClick),
        content = {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    )
}


@Preview(widthDp = 150, heightDp = 150, fontScale = 1f)
@Composable
fun PlayerCardPreview() {
    Player(
        player = CounterCardUIState(
            id = PlayerId(0),
            color = DEFAULT_PALETTE[0],
            counter = PlayerCounterUIState(
                id = CounterId(0),
                combo = 1,
                counterName = "test",
                hasMultipleCounters = true,
                counterValue = 100,
            ),
        ),
        onUpdateCounter = { _, _ -> },
        onNextCounter = {},
        onPreviousCounter = {},
        onDelete = {},
        onSetColor = {},
    )
}