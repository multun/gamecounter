package net.multun.gamecounter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.em
import net.multun.gamecounter.BoardViewModel
import net.multun.gamecounter.CardUIState
import net.multun.gamecounter.CounterCardUIState
import net.multun.gamecounter.RollCardUIState
import net.multun.gamecounter.toDisplayColor


@Composable
fun Player(
    player: CardUIState,
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier,
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    BoardCard(
        color = player.color.toDisplayColor(),
        modifier = modifier
    ) {
        BoxWithConstraints {
            val counterScale = counterScale(this.maxWidth, this.maxHeight)
            if (isEditing) {
                PlayerCardSettings(
                    currentPlayerColor = player.color,
                    onExit = { isEditing = false },
                    onDelete = remember { { viewModel.removePlayer(player.id) } },
                    onSetColor = remember { { color -> viewModel.setPlayerColor(player.id, color) } },
                )
                return@BoxWithConstraints
            }

            when (player) {
                is RollCardUIState -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "${player.roll}", fontSize = counterScale.apply(10.em, 25.em))
                    }
                }
                is CounterCardUIState -> {
                    if (player.counter == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "no counter", fontSize = 5.em)
                        }
                        return@BoxWithConstraints
                    }

                    val counter = player.counter
                    PlayerCounter(
                        modifier = Modifier.fillMaxSize(),
                        counter = counter,
                        counterScale = counterScale,
                        onIncrement = remember(counter.id.value) { { viewModel.updateCounter(player.id, counter.id, 1) } },
                        onDecrement = remember(counter.id.value) { { viewModel.updateCounter(player.id, counter.id, -1) } },
                        onNextCounter = remember { { viewModel.nextCounter(player.id) } },
                        onPreviousCounter = remember { { viewModel.previousCounter(player.id) } },
                        onEdit = { isEditing = true }
                    )
                }
            }
        }
    }
}


@Composable
fun BoardCard(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier, colors = CardDefaults.cardColors().copy(containerColor = color)) {
        content()
    }
}