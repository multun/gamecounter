package net.multun.gamecounter.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


private val INITIAL_DELAY = 700.milliseconds
private val BUMP_DELAY = 500.milliseconds


class UpdateButtonState {
    val interactionSource = MutableInteractionSource()

    // on event, returns whether the button was long pressed
    @Composable
    fun WatchEvents(onEvent: (Boolean) -> Unit) {
        var longPressed by remember(interactionSource) { mutableStateOf(false) }
        var isPressed by remember(interactionSource) { mutableStateOf(false) }

        LaunchedEffect(interactionSource, onEvent) {
            val pressInteractions = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                // if the button was released and this is not a long press, emit a short press
                // this is lifted out of interactionSource.collectIsPressedAsState()
                val wasPressed = pressInteractions.isNotEmpty()
                when (interaction) {
                    is PressInteraction.Press -> pressInteractions.add(interaction)
                    is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                    is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
                }

                // when all press interactions are released, do a small counter update
                // if there was no long press, or reset the long press flag
                if (wasPressed && pressInteractions.isEmpty()) {
                    if (longPressed) {
                        longPressed = false
                    } else {
                        // short press
                        onEvent(false)
                    }
                }
                isPressed = pressInteractions.isNotEmpty()
            }
        }

        LaunchedEffect(interactionSource, onEvent, isPressed) {
            if (!isPressed)
                return@LaunchedEffect
            delay(INITIAL_DELAY)
            longPressed = true
            while (true) {
                // long press
                onEvent(true)
                delay(BUMP_DELAY)
            }
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
        onClick = {},
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
                    onClick = {},
                    role = Role.Button,
                    interactionSource = buttonState.interactionSource,
                    indication = null
                ),
    )
}