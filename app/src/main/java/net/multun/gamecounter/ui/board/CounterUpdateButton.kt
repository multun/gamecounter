package net.multun.gamecounter.ui.board

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


private val INITIAL_DELAY = 700.milliseconds
private val BUMP_DELAY = 400.milliseconds


sealed interface CounterUpdateEvent
data object SmallCounterUpdate : CounterUpdateEvent
data object BigCounterUpdate : CounterUpdateEvent

fun CounterUpdateEvent.stepSize(): Int {
    return when (this) {
        BigCounterUpdate -> 10
        SmallCounterUpdate -> 1
    }
}

@Composable
fun CounterUpdateButton(
    modifier: Modifier = Modifier,
    onUpdateCounter: (CounterUpdateEvent) -> Unit,
    content: @Composable () -> Unit,
) {
    val minusInteractionSource = remember { MutableInteractionSource() }
    val isPressed by minusInteractionSource.collectIsPressedAsState()
    var longPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (!isPressed)
            return@LaunchedEffect

        delay(INITIAL_DELAY)
        longPressed = true
        while (true) {
            onUpdateCounter(BigCounterUpdate)
            delay(BUMP_DELAY)
        }
    }

    IconButton(
        interactionSource = minusInteractionSource,
        modifier = modifier,
        content = content,
        onClick = {
            if (longPressed) {
                longPressed = false
            } else {
                onUpdateCounter(SmallCounterUpdate)
            }
        },
    )
}