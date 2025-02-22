@file:OptIn(ExperimentalLayoutApi::class, ExperimentalLayoutApi::class,
    ExperimentalLayoutApi::class
)

package net.multun.gamecounter.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.multun.gamecounter.DEFAULT_PALETTE
import net.multun.gamecounter.toDisplayColor

enum class PlayerMenu {
    MAIN,
    COLOR,
    DELETE,
}

@Composable
fun PlayerCardSettings(
    currentPlayerColor: Color,
    onExit: () -> Unit,
    onDelete: () -> Unit,
    onSetColor: (Color) -> Unit,
) {
    var menu by remember { mutableStateOf(PlayerMenu.MAIN) }
    Column(modifier = Modifier.fillMaxSize()) {
        // top row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            // back button
            IconButton(onClick = {
                if (menu == PlayerMenu.MAIN) {
                    onExit()
                } else {
                    menu = PlayerMenu.MAIN
                }
            }) {
                Icon(Icons.Filled.Clear, contentDescription = "Go back")
            }
        }

        when (menu) {
            PlayerMenu.MAIN -> PlayerMenu {
                PlayerMenuItem(icon = Icons.Default.Palette, "Color") { menu = PlayerMenu.COLOR }
                PlayerMenuItem(icon = Icons.Default.Delete, "Delete") { menu = PlayerMenu.DELETE }
            }
            PlayerMenu.COLOR -> FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterVertically
                ),
            ) {
                for (color in DEFAULT_PALETTE) {
                    PaletteItem(color.toDisplayColor(), selected = color == currentPlayerColor) {
                        onSetColor(color)
                    }
                }
            }
            PlayerMenu.DELETE -> {
                PlayerMenu {
                    PlayerMenuItem(Icons.Default.Cancel, "Cancel") { menu = PlayerMenu.MAIN }
                    PlayerMenuItem(Icons.Default.Delete, "Confirm") { onDelete() }
                }
            }
        }
    }
}

@Composable
fun PlayerMenu(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize()
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
    }
}

@Composable
fun PaletteItem(color: Color, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Spacer(modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(width = 1.dp, color = Color.DarkGray, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
        )
        if (selected) {
            Icon(Icons.Default.Check, "")
        }
    }
}


@Composable
fun PlayerMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = text)
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text, fontSize = 18.sp, lineHeight = 18.sp * 1.2f)
    }
}

@Preview(widthDp = 210, heightDp = 170)
@Composable
fun PreviewPlayerSettings() {
    var playerColor by remember { mutableStateOf(DEFAULT_PALETTE[0]) }
    BoardCard(color = playerColor) {
        PlayerCardSettings(
            currentPlayerColor = playerColor,
            onExit = {},
            onDelete = {},
            onSetColor = { playerColor = it },
        )
    }
}