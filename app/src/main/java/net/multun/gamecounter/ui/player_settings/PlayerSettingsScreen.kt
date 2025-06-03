@file:OptIn(ExperimentalMaterial3Api::class)

package net.multun.gamecounter.ui.player_settings


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.collections.immutable.ImmutableList
import net.multun.gamecounter.PaletteColor
import net.multun.gamecounter.R
import net.multun.gamecounter.store.PlayerId
import net.multun.gamecounter.ui.GameCounterTopBar
import net.multun.gamecounter.ui.board.GameCard
import net.multun.gamecounter.ui.board.GameIconButton
import net.multun.gamecounter.ui.theme.Typography

data class PlayerSettingsUIState(
    val id: PlayerId,
    val name: String,
    val color: Color,
)

interface PlayerSettingsActions {
    fun addPlayer()
    fun deletePlayer(playerId: PlayerId)
    fun setPlayerName(playerId: PlayerId, name: String)
    fun movePlayerUp(playerId: PlayerId)
    fun movePlayerDown(playerId: PlayerId)
}

sealed class PlayerSettingsDialog
data class EditDialog(val player: PlayerSettingsUIState) : PlayerSettingsDialog()
data class ConfirmDeleteDialog(val player: PlayerSettingsUIState) : PlayerSettingsDialog()

@Composable
fun PlayerSettingsScreen(
    players: ImmutableList<PlayerSettingsUIState>,
    viewModel: PlayerSettingsActions,
    navController: NavController,
) {
    var dialog by remember { mutableStateOf<PlayerSettingsDialog?>(null) }
    Scaffold(
        topBar = {
            GameCounterTopBar(stringResource(R.string.players_settings), navController)
        },
        floatingActionButton = {
            GameIconButton(
                PaletteColor.Indigo.color,
                onClick = remember { { viewModel.addPlayer() } }
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_new_player))
            }
        }
    ) { contentPadding ->
        PlayerSettingsList(
            players = players,
            onMoveUp = remember { { viewModel.movePlayerUp(it)} },
            onMoveDown = remember { { viewModel.movePlayerDown(it) } },
            onDialog = remember { { dialog = it } },
            modifier = Modifier.padding(contentPadding),
        )
    }

    val curDialog = dialog
    if (curDialog != null) {
        PlayerSettingsDialog(
            curDialog,
            onDelete = remember { { viewModel.deletePlayer(it) } },
            onSetName = remember { { id, name -> viewModel.setPlayerName(id, name) } },
            onClearDialog = remember { { dialog = null } },
        )
    }
}

@Composable
fun PlayerSettingsList(
    players: ImmutableList<PlayerSettingsUIState>,
    onMoveUp: (PlayerId) -> Unit,
    onMoveDown: (PlayerId) -> Unit,
    onDialog: (PlayerSettingsDialog) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.padding(10.dp),
    ) {
        for (playerIndex in 0 until players.size) {
            val player = players[playerIndex]
            val isFirst = playerIndex == 0
            val isLast = playerIndex == players.size - 1
            item(player.id.value) {
                PlayerSettingsLine(
                    player.name,
                    player.color,
                    isFirst,
                    isLast,
                    onEdit = { onDialog(EditDialog(player)) },
                    onMoveUp = remember { { onMoveUp(player.id) } },
                    onMoveDown = remember { { onMoveDown(player.id) } },
                    onDelete = { onDialog(ConfirmDeleteDialog(player)) },
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                )
            }
        }
    }
}

@Composable
fun PlayerSettingsDialog(
    dialog: PlayerSettingsDialog,
    onDelete: (PlayerId) -> Unit,
    onSetName: (PlayerId, String) -> Unit,
    onClearDialog: () -> Unit,
) {
    when (dialog) {
        is EditDialog -> PlayerChangeDialog(
            title = stringResource(R.string.player_settings),
            action = stringResource(R.string.save),
            initialName = dialog.player.name,
            onDismissRequest = onClearDialog,
            onSetName = remember { { name ->
                onSetName(dialog.player.id, name)
                onClearDialog()
            } }
        )
        is ConfirmDeleteDialog -> AlertDialog(
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            text = {
                Text(
                    stringResource(
                        R.string.confirm_delete_player,
                        dialog.player.name
                    )
                )
            },
            onDismissRequest = onClearDialog,
            confirmButton = {
                TextButton(onClick = remember { {
                    onDelete(dialog.player.id)
                    onClearDialog()
                } }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onClearDialog) {
                    Text(stringResource(R.string.cancel))
                }
            })
    }
}


@Composable
fun PlayerChangeDialog(
    title: String,
    action: String,
    onDismissRequest: () -> Unit,
    onSetName: (String) -> Unit,
    initialName: String = "",
) {
    var playerName by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(IntrinsicSize.Min)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(title, style = Typography.bodyLarge)

                val nameError = playerName.isBlank()

                OutlinedTextField(
                    value = playerName,
                    isError = nameError,
                    onValueChange = { playerName = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        enabled = !nameError,
                        onClick = { onSetName(playerName.trim()) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(action)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerSettingsLine(
    name: String,
    color: Color,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GameCard(color, modifier = modifier
        .height(50.dp)
        .fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                style = Typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Row(horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.player_settings))
                }
                IconButton(enabled = !isFirst, onClick = onMoveUp) {
                    Icon(Icons.Filled.MoveUp, contentDescription = stringResource(R.string.move_up))
                }
                IconButton(enabled = !isLast, onClick = onMoveDown) {
                    Icon(Icons.Filled.MoveDown, contentDescription = stringResource(R.string.move_down))
                }
                IconButton(enabled = !(isFirst && isLast), onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_player))
                }
            }
        }
    }
}

