package net.multun.gamecounter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import net.multun.gamecounter.BoardViewModel
import net.multun.gamecounter.data.MemoryAppState
import net.multun.gamecounter.data.MockDataStore
import net.multun.gamecounter.data.PlayerId


/** The number of counters per row. Cannot easily be changed. */
private const val MAX_ROW_PLAYERS: Int = 2
private val PADDING = 8.dp

@Composable
fun Board(
    players: ImmutableList<PlayerId>,
    viewModel: BoardViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn {
        items(2) {
            key("a") {

            }
        }
    }
    Box(modifier = modifier) {
        Column(modifier = Modifier.padding(PADDING / 2)) {
            val rowCount = (players.size + 1) / MAX_ROW_PLAYERS
            for (rowIndex in 0 until rowCount) {
                val rowPlayerOffset = rowIndex * MAX_ROW_PLAYERS
                val remainingPlayers = players.size - rowPlayerOffset
                val rowPlayers = if (remainingPlayers > MAX_ROW_PLAYERS) MAX_ROW_PLAYERS else remainingPlayers
                Row(modifier = Modifier
                    .padding(PADDING / 2)
                    .fillMaxSize()
                    .weight(1f), horizontalArrangement = Arrangement.spacedBy(PADDING)) {
                    for (colIndex in 0 until rowPlayers) {
                        val playerIndex = rowPlayerOffset + colIndex
                        val playerId = players[playerIndex]
                        val orientation = when {
                            rowPlayers == 1 -> Rotation.ROT_0
                            colIndex == 0 -> Rotation.ROT_90
                            else -> Rotation.ROT_270
                        }

                        key(playerId) {
                            val playerState by viewModel.watchPlayer(playerId).collectAsStateWithLifecycle()
                            if (playerState != null) {
                                Player(
                                    modifier = Modifier
                                        .weight(1f)
                                        .rotateLayout(orientation),
                                    health = playerState!!.health,
                                    color = playerState!!.color,
                                    incr = { viewModel.incrCount(playerId) },
                                    decr = { viewModel.decrCount(playerId) },
                                    editClicked = { viewModel.addPlayer() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BoardPreview() {
    // Define the counter value as a state object
    val viewModel by remember { mutableStateOf(BoardViewModel(MemoryAppState(MockDataStore()))) }
    val playerIds by viewModel.playerIds.collectAsStateWithLifecycle()
    Board(playerIds, viewModel, Modifier.fillMaxSize())
}