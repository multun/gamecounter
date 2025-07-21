package net.multun.gamecounter.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


private val INITIAL_DELAY = 700.milliseconds
private val BUMP_DELAY = 500.milliseconds


sealed interface CounterUpdateEvent
data object SmallCounterUpdate : CounterUpdateEvent
data object BigCounterUpdate : CounterUpdateEvent

fun CounterUpdateEvent.stepSize(): Int {
    return when (this) {
        BigCounterUpdate -> 10
        SmallCounterUpdate -> 1
    }
}

@Stable
class UpdateButtonState(val onUpdateCounter: (CounterUpdateEvent) -> Unit) {
    val interactionSource = MutableInteractionSource()
    private var longPressed by mutableStateOf(false)

    @Composable
    fun WatchLongPress() {
        val isPressed by interactionSource.collectIsPressedAsState()
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
    }

    fun onClick() {
        if (longPressed) {
            longPressed = false
        } else {
            onUpdateCounter(SmallCounterUpdate)
        }
    }
}

@Composable
fun CounterUpdateButton(
    buttonState: UpdateButtonState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        interactionSource = buttonState.interactionSource,
        modifier = modifier,
        content = content,
        onClick = { buttonState.onClick() },
    )
}

@Composable
fun CounterUpdatePad(
    buttonState: UpdateButtonState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(
                    onClick = { buttonState.onClick() },
                    role = Role.Button,
                    interactionSource = buttonState.interactionSource,
                    indication = null
                ),
    )
}