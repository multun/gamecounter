@file:OptIn(ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.multun.gamecounter.PALETTE
import net.multun.gamecounter.PaletteColor
import net.multun.gamecounter.R
import net.multun.gamecounter.toDisplayColor


@Composable
fun PlayerCardPalette(
    currentPlayerColor: Color,
    onExit: () -> Unit,
    onSetColor: (Color) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            CardSettingsTopBar(onBack = onExit)
            ColorPicker(currentPlayerColor, onSetColor)
        }
    }
}

@Composable
fun CardSettingsTopBar(onBack: () -> Unit) {
    // top row
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        // back button
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.previous_screen))
        }
    }
}

@Composable
private fun ColorPicker(
    currentPlayerColor: Color,
    onSetColor: (Color) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
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
        for (color in PALETTE) {
            PaletteItem(color.toDisplayColor(), selected = color == currentPlayerColor) {
                onSetColor(color)
            }
        }
    }
}

@Composable
fun PaletteItem(color: Color, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Spacer(modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(width = 1.5.dp, color = Color.DarkGray, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
        )
        if (selected) {
            Icon(Icons.Default.Check, null)
        }
    }
}


@Preview(widthDp = 150, heightDp = 150)
@Composable
fun PreviewPlayerSettings() {
    var playerColor by remember { mutableStateOf(PaletteColor.Green.color) }
    GameCard(baseColor = playerColor) {
        PlayerCardPalette(
            currentPlayerColor = playerColor,
            onExit = {},
            onSetColor = { playerColor = it },
        )
    }
}