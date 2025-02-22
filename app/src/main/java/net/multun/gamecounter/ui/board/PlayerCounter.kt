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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.layoutId
import net.multun.gamecounter.DEFAULT_PALETTE
import net.multun.gamecounter.store.CounterId
import java.util.Locale



@Composable
fun CardMainText(text: String, scale: FontScale, modifier: Modifier = Modifier) {
    ScaledText(
        text = text,
        scale = scale,
        baseSize = 45.dp,
        maxSize = 112.5.dp,
        modifier = modifier,
    )
}

@Composable
fun CardSubText(text: String, scale: FontScale, modifier: Modifier = Modifier) {
    ScaledText(
        text = text,
        scale = scale,
        baseSize = 18.dp,
        maxSize = 28.dp,
        modifier = modifier,
    )
}

@Composable
fun CardExpText(text: String, scale: FontScale, modifier: Modifier = Modifier) {
    ScaledText(
        text = text,
        scale = scale,
        baseSize = 18.dp,
        maxSize = 45.dp,
        modifier = modifier,
    )
}


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
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "")
            }
        }
        CardSubText(text = counterName, counterScale)
        if (showControls) {
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "")
            }
        }
    }
}

@Composable
fun PlayerCounter(
    counter: PlayerCounterUIState,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onNextCounter: () -> Unit,
    onPreviousCounter: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    counterScale: FontScale = FontScale(1f),
) {
    ConstraintLayout(playerCounterLayout(), modifier = modifier) {
        // minus
        IconButton(onClick = onDecrement, modifier = Modifier.layoutId("decr")) {
            Icon(Icons.Default.Remove, contentDescription = "Increase counter")
        }

        // plus
        IconButton(onClick = onIncrement, modifier = Modifier.layoutId("incr")) {
            Icon(Icons.Default.Add, contentDescription = "Increase counter")
        }

        // counter
        CardMainText(
            text = "${counter.counterValue}",
            scale = counterScale,
            modifier = Modifier.layoutId("counterValue"),
        )

        // combo counter
        AnimatedContent(
            label = "combo animation",
            modifier = Modifier.layoutId("combo"),
            targetState = counter.combo,
            transitionSpec = { comboCounterAnimation() },
        ) { targetCount ->
            if (targetCount != null) {
                val comboText = String.format(Locale.ENGLISH, "%+d", targetCount)
                CardExpText(comboText, counterScale)
            }
        }

        val settingsColor = IconButtonDefaults.iconButtonColors().copy(
            contentColor = LocalContentColor.current.copy(alpha = 0.75f)
        )
        IconButton(onClick = onEdit, colors = settingsColor, modifier = Modifier.layoutId("edit")) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = "Edit player settings")
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


@Preview(widthDp = 400, heightDp = 400, fontScale = 1f)
@Composable
fun PlayerCounterTest() {
    BoardCard(color = DEFAULT_PALETTE[0], modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min)) {
        PlayerCounter(
            modifier = Modifier.fillMaxSize(),
            counter = PlayerCounterUIState(
                id = CounterId(0),
                combo = 1,
                counterName = "test",
                hasMultipleCounters = true,
                counterValue = 1,
            ),
            onIncrement = {},
            onDecrement = {},
            onNextCounter = {},
            onPreviousCounter = {},
            onEdit = {}
        )
    }
}