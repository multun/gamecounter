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
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.layoutId
import net.multun.gamecounter.R
import net.multun.gamecounter.store.CounterId


@Composable
fun CounterSelector(
    counterScale: FontScale,
    counterName: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    showControls: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (showControls) {
            IconButton(onClick = onPrev) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    stringResource(R.string.previous_counter)
                )
            }
        }
        WithScaledFontSize(counterScale, SUB_CARD_TEXT) {
            Text(text = counterName)
        }
        if (showControls) {
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    stringResource(R.string.next_counter)
                )
            }
        }
    }
}


@Composable
fun PlayerCounter(
    counter: PlayerCounterUIState,
    onUpdateCounter: (CounterId, Int) -> Unit,
    onNextCounter: () -> Unit,
    onPreviousCounter: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    counterScale: FontScale,
) {
    ConstraintLayout(playerCounterLayout(), modifier = modifier) {
        // minus
        CounterUpdateButton(
            onUpdateCounter = { onUpdateCounter(counter.id, -it.stepSize()) },
            modifier = Modifier.layoutId("decr"),
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.decrease_counter)
            )
        }

        // plus
        CounterUpdateButton(
            onUpdateCounter = { onUpdateCounter(counter.id, it.stepSize()) },
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
                text = formatInteger(counter.counterValue),
                modifier = Modifier.layoutId("counterValue")
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
            if (targetCount != null) {
                val comboText = formatCombo(targetCount)
                WithScaledFontSize(counterScale, EXP_CARD_TEXT) {
                    Text(text = comboText)
                }
            }
        }

        val settingsColor = IconButtonDefaults.iconButtonColors().copy(
            contentColor = LocalContentColor.current.copy(alpha = 0.75f)
        )
        IconButton(onClick = onEdit, colors = settingsColor, modifier = Modifier.layoutId("edit")) {
            Icon(
                Icons.Outlined.PersonOutline,
                contentDescription = stringResource(R.string.player_settings)
            )
        }

        CounterSelector(
            counterScale = counterScale,
            counterName = counter.counterName,
            onPrev = onPreviousCounter,
            onNext = onNextCounter,
            showControls = counter.hasMultipleCounters,
            modifier = Modifier.layoutId("counterSelector")
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
        val edit = createRefFor("edit")

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

        constrain(edit) {
            top.linkTo(parent.top, margin = 0.dp)
            end.linkTo(parent.end, margin = 0.dp)
        }
    }
}

fun AnimatedContentTransitionScope<Int?>.comboCounterAnimation(): ContentTransform {
    val initial = initialState
    val target = targetState
    return if ((initial != null && target != null && target > initial) || initial == null) {
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