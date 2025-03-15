package net.multun.gamecounter.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.multun.gamecounter.PaletteColor
import net.multun.gamecounter.R
import net.multun.gamecounter.store.CounterId
import net.multun.gamecounter.store.PlayerId
import net.multun.gamecounter.toDisplayColor


val MAIN_CARD_TEXT = FontSizeClass(base = 45.dp, max = 112.5.dp)
val ORDINAL_CARD_TEXT = FontSizeClass(base = 30.dp, max = 75.dp)
val SUB_CARD_TEXT = FontSizeClass(base = 18.dp, max = 28.dp)
val EXP_CARD_TEXT = FontSizeClass(base = 18.dp, max = 45.dp)
val NAME_CARD_TEXT = FontSizeClass(base = 14.dp, max = 20.dp)


@Composable
fun Player(
    player: CardUIState,
    onDelete: () -> Unit,
    onSetColor: (Color) -> Unit,
    onEditName: () -> Unit,
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
            when (player) {
                is RollCardUIState -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    if (isEditing) {
                        PlayerCardSettings(
                            currentPlayerColor = player.color,
                            onExit = { isEditing = false },
                            onDelete = onDelete,
                            onSetColor = onSetColor,
                            onEditName = onEditName,
                        )
                        return@BoxWithConstraints
                    }

                    if (player.counter == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            WithScaledFontSize(counterScale, SUB_CARD_TEXT) {
                                Text(text = stringResource(R.string.no_counter))
                            }
                        }
                        return@BoxWithConstraints
                    }

                    val counter = player.counter
                    PlayerCounter(
                        name = player.name,
                        modifier = Modifier.fillMaxSize(),
                        counter = counter,
                        counterScale = counterScale,
                        onUpdateCounter = onUpdateCounter,
                        onNextCounter = onNextCounter,
                        onPreviousCounter = onPreviousCounter,
                        onEdit = { isEditing = true }
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
        onEditName = {}
    )
}