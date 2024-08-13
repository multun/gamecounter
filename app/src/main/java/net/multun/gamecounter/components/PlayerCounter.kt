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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.layoutId
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
fun PlayerCounter(color: Color, health: Int, delta: Int?, incr: () -> Unit, decr: () -> Unit, modifier: Modifier = Modifier) {
    val constraintSet = ConstraintSet {
        val decr = createRefFor("decr")
        val incr = createRefFor("incr")
        val counter = createRefFor("counter")
        val combo = createRefFor("combo")

        createHorizontalChain(decr, counter, incr, chainStyle = ChainStyle.Spread)

        for (centeredItem in listOf(decr, incr, counter)) {
            constrain(centeredItem) {
                centerVerticallyTo(parent)
            }
        }

        constrain(combo) {
            bottom.linkTo(counter.top)
            end.linkTo(counter.end, margin = -(10.dp))
        }
    }

    Card(modifier = modifier, colors = CardDefaults.cardColors().copy(containerColor = color)) {
        ConstraintLayout(constraintSet, modifier = Modifier.fillMaxSize()) {
            // minus
            CounterButton(onClick = decr, modifier = Modifier.layoutId("decr")) {
                Icon(Icons.Default.Remove, contentDescription = "Increase counter")
            }

            // counter
            Text(text = "$health", fontSize = 10.em, modifier = Modifier.layoutId("counter"))

            // combo counter
            AnimatedContent(
                label = "combo animation",
                modifier = Modifier.layoutId("combo"),
                targetState = delta,
                transitionSpec = { comboCounterAnimation() },
            ) { targetCount ->
                if (targetCount != null) {
                    val comboText = String.format(Locale.ENGLISH, "%+d", targetCount)
                    Text(text = comboText, fontSize = 4.em)
                }
            }

            // plus
            CounterButton(onClick = incr, modifier = Modifier.layoutId("incr")) {
                Icon(Icons.Default.Add, contentDescription = "Increase counter")
            }
        }
    }
}

@Preview
@Composable
fun PlayerCounterTest() {
    PlayerCounter(color = Color(0xFFD1C4E9), health = 10, delta = 3, incr = {}, decr = {})
}