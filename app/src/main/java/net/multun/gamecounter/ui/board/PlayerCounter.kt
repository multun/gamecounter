package net.multun.gamecounter.ui.board

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.layoutId
import com.sd.lib.compose.wheel_picker.FHorizontalWheelPicker
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import net.multun.gamecounter.R
import net.multun.gamecounter.store.CounterId


@Composable
fun CounterSelector(
    counterScale: FontScale,
    selectedCounterId: CounterId,
    counters: List<CounterUIState>,
    onSelectCounter: (CounterId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberFWheelPickerState(counters.indexOfFirst { it.id == selectedCounterId })

    LaunchedEffect(state, counters.map { it.id }) {
        snapshotFlow { state.currentIndex }
            .collect {
                if (it != -1)
                   onSelectCounter(counters[it].id)
            }
    }

    val fontSize = counterScale.applyDp(SUB_CARD_TEXT)
    val fontSizeSp = with(LocalDensity.current) { fontSize.toSp() }
    WithFontSize(fontSizeSp) {
        val textStyle = LocalTextStyle.current
        val density = LocalDensity.current
        val counterNames = counters.map { it.name }
        val textMeasurer = rememberTextMeasurer()
        val itemWidth = remember(textMeasurer, textStyle, counterNames) {
            counterNames.selectorWidth(textMeasurer, textStyle, density)
        }
        FHorizontalWheelPicker(
            modifier = modifier.height(fontSize * 1.4f),
            state = state,
            count = counters.size,
            itemWidth = itemWidth,
        ) { index ->
            Text(
                counters[index].name,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

fun List<String>.selectorWidth(
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    density: Density,
    padding: Float = 0.2f,
    minWidth: Float = 2f,
    maxWidth: Float = 5f,
): Dp {
    val maxCounterNameWidthPx = this.maxOf {
        textMeasurer.measure(
            it,
            textStyle
        ).size.width
    }

    val maxCounterNameWidth: Dp
    val fontSize: Dp
    with(density) {
        maxCounterNameWidth = maxCounterNameWidthPx.toDp()
        fontSize = textStyle.fontSize.toDp()
    }

    var itemWidth = maxCounterNameWidth + fontSize * padding * 2
    val maxItemWidth = fontSize * maxWidth
    val minItemWidth = fontSize * minWidth
    if (itemWidth > maxItemWidth)
        itemWidth = maxItemWidth
    if (itemWidth < minItemWidth)
        itemWidth = minItemWidth
    return itemWidth
}

@Composable
fun PlayerTopRowButton(onClick: () -> Unit, muted: Boolean = true, content: @Composable () -> Unit) {
    var colors = IconButtonDefaults.iconButtonColors()
    if (muted) {
        colors = colors.copy(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    IconButton(onClick = onClick, colors = colors, content = content)
}

@Composable
fun PlayerTopRow(
    playerName: String,
    counterScale: FontScale,
    modifier: Modifier = Modifier,
    buttons: @Composable () -> Unit
) {
    // the top row, with the player name at the left and edit button at the right
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // adding a weight causes the row item size to be measured after unweighted items,
        // which allows the edit button to keep its size despite being after the player name
        PlayerName(counterScale, playerName, modifier = Modifier.weight(1f))

        buttons()
    }
}


@Composable
fun PlayerCounter(
    player: CounterCardUIState,
    onUpdateCounter: (CounterId, Int) -> Unit,
    onSelectCounter: (CounterId) -> Unit,
    onEditCounter: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    counterScale: FontScale,
) {
    ConstraintLayout(playerCounterLayout(), modifier = modifier) {
        val counter = player.counters.find { it.id == player.selectedCounter }!!

        // the top row, with the player name at the left and edit button at the right
        PlayerTopRow(player.name, counterScale, Modifier.layoutId("topRow")) {
            PlayerTopRowButton(onClick = onEdit) {
                Icon(
                    Icons.Outlined.PersonOutline,
                    contentDescription = stringResource(R.string.player_settings)
                )
            }
        }

        // minus
        CounterUpdateButton(
            onUpdateCounter = { onUpdateCounter(player.selectedCounter, -it.stepSize()) },
            modifier = Modifier.layoutId("decr"),
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.decrease_counter)
            )
        }

        // plus
        CounterUpdateButton(
            onUpdateCounter = { onUpdateCounter(player.selectedCounter, it.stepSize()) },
            modifier = Modifier.layoutId("incr"),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.increase_counter)
            )
        }

        // counter
        WithScaledFontSize(counterScale, MAIN_CARD_TEXT, lineHeight = 1f) {
            Text(
                text = formatInteger(counter.value),
                modifier = Modifier.layoutId("counterValue").clickable(onClick = onEditCounter)
            )
        }

        // combo counter
        AnimatedContent(
            label = "combo animation",
            modifier = Modifier.layoutId("combo"),
            contentAlignment = Alignment.BottomEnd,
            targetState = counter.combo,
            transitionSpec = { comboCounterAnimation() },
        ) { targetCount ->
            if (targetCount != 0) {
                val comboText = formatCombo(targetCount)
                WithScaledFontSize(counterScale, EXP_CARD_TEXT) {
                    Text(text = comboText)
                }
            }
        }

        if (player.counters.size > 1) {
            CounterSelector(
                counterScale = counterScale,
                selectedCounterId = player.selectedCounter,
                counters = player.counters,
                onSelectCounter = onSelectCounter,
                modifier = Modifier.layoutId("counterSelector")
            )
        }
    }
}

@Composable
fun PlayerName(scale: FontScale, name: String, modifier: Modifier = Modifier) {
    WithScaledFontSize(scale, NAME_CARD_TEXT, lineHeight = 1f) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
                .padding(start = 14.dp)
                // this is done to match the height of the edit button
                .minimumInteractiveComponentSize()
                .wrapContentHeight(align = Alignment.CenterVertically)
        )
    }
}

private fun playerCounterLayout(): ConstraintSet {
    return ConstraintSet {
        val decr = createRefFor("decr")
        val incr = createRefFor("incr")
        val counterValue = createRefFor("counterValue")
        val combo = createRefFor("combo")
        val counterSelector = createRefFor("counterSelector")
        val topRow = createRefFor("topRow")

        createHorizontalChain(decr, counterValue, incr, chainStyle = ChainStyle.Spread)

        for (centeredItem in listOf(decr, incr, counterValue)) {
            constrain(centeredItem) {
                centerVerticallyTo(parent)
            }
        }

        constrain(counterSelector) {
            top.linkTo(counterValue.bottom)
            centerHorizontallyTo(parent)
        }

        constrain(counterValue) {
            centerVerticallyTo(parent)
        }

        constrain(combo) {
            bottom.linkTo(counterValue.top)
            end.linkTo(counterValue.end, margin = -(10.dp))
        }

        constrain(topRow) {
            top.linkTo(parent.top, margin = 0.dp)
        }
    }
}

fun AnimatedContentTransitionScope<Int>.comboCounterAnimation(): ContentTransform {
    return if (targetState > initialState) {
        // If the target number is larger, it slides up and fades in
        // while the initial (smaller) number slides up and fades out.
        slideInVertically { height -> height } + fadeIn() togetherWith
                slideOutVertically { height -> -height } + fadeOut()
    } else {
        // If the target number is smaller, it slides down and fades in
        // while the initial number slides down and fades out.
        slideInVertically { height -> -height } + fadeIn() togetherWith
                slideOutVertically { height -> height } + fadeOut()
    }.using(
        // Disable clipping since the faded slide-in/out should
        // be displayed out of bounds.
        SizeTransform(clip = false)
    )
}