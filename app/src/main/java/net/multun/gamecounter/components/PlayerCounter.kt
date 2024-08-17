package net.multun.gamecounter.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.layoutId
import androidx.datastore.core.DataStoreFactory
import net.multun.gamecounter.BoardViewModel
import net.multun.gamecounter.DEFAULT_PALETTE
import net.multun.gamecounter.PlayerUIState
import net.multun.gamecounter.datastore.CounterId
import net.multun.gamecounter.datastore.PlayerId
import net.multun.gamecounter.datastore.AppStateRepository
import net.multun.gamecounter.datastore.AppStateSerializer
import java.io.File
import java.util.Locale


@Composable
fun CounterButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(onClick = onClick,
        modifier = modifier.size(50.dp),  //avoid the oval shape
        shape = CircleShape,
        border = null,
        contentPadding = PaddingValues(0.dp),  //avoid the little icon
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
        content = content,
    )
}

fun AnimatedContentTransitionScope<Int?>.comboCounterAnimation(): ContentTransform {
    val initial = initialState
    val target = targetState
    return if ( (initial != null && target != null && target > initial) || initial == null) {
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

@Composable
fun CounterSelector(
    counterName: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    showControls: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (showControls) {
            CounterButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "")
            }
        }
        Text(text = counterName, fontSize = 4.em)
        if (showControls) {
            CounterButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "")
            }
        }
    }
}

@Composable
fun PlayerCounter(player: PlayerUIState, hasMultipleCounters: Boolean, viewModel: BoardViewModel, modifier: Modifier = Modifier) {
    val color = player.color
    val playerId = player.id
    val counterId = player.selectedCounter
    val counterValue = player.counterValue
    val counterName = player.counterName
    val combo = player.combo

    Card(modifier = modifier, colors = CardDefaults.cardColors().copy(containerColor = color)) {
        ConstraintLayout(playerCounterLayout(), modifier = Modifier.fillMaxSize()) {
            // minus
            CounterButton(onClick = { viewModel.updateCounter(playerId, counterId!!, -1) }, modifier = Modifier.layoutId("decr")) {
                Icon(Icons.Default.Remove, contentDescription = "Increase counter")
            }

            // plus
            CounterButton(onClick = { viewModel.updateCounter(playerId, counterId!!, 1) }, modifier = Modifier.layoutId("incr")) {
                Icon(Icons.Default.Add, contentDescription = "Increase counter")
            }

            // counter
            Text(text = "$counterValue", fontSize = 10.em, modifier = Modifier.layoutId("counterValue"))

            // combo counter
            AnimatedContent(
                label = "combo animation",
                modifier = Modifier.layoutId("combo"),
                targetState = combo,
                transitionSpec = { comboCounterAnimation() },
            ) { targetCount ->
                if (targetCount != null) {
                    val comboText = String.format(Locale.ENGLISH, "%+d", targetCount)
                    Text(text = comboText, fontSize = 4.em)
                }
            }

            if (counterName != null) {
                CounterSelector(
                    counterName = counterName,
                    onPrev = { viewModel.previousCounter(playerId) },
                    onNext = { viewModel.nextCounter(playerId) },
                    showControls = hasMultipleCounters,
                    modifier = Modifier.layoutId("counterSelector")
                )
            }
        }
    }
}

private fun playerCounterLayout(): ConstraintSet {
    return ConstraintSet {
        val decr = createRefFor("decr")
        val incr = createRefFor("incr")
        val counterValue = createRefFor("counterValue")
        val combo = createRefFor("combo")
        val counterSelector = createRefFor("counterSelector")

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
    }
}

@Preview
@Composable
fun PlayerCounterTest() {
    val viewModel by remember {
        mutableStateOf(BoardViewModel(AppStateRepository(
            DataStoreFactory.create(serializer = AppStateSerializer) {
                File.createTempFile("board_preview", ".pb", null)
            }
        )))
    }
    PlayerCounter(PlayerUIState(
        id = PlayerId(0),
        color = DEFAULT_PALETTE[0],
        selectedCounter = CounterId(0),
        combo = 1,
        counterName = "test",
        counterValue = 1,
    ), true, viewModel)
}