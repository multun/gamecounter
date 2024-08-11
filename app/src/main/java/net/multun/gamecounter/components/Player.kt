package net.multun.gamecounter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay


@Composable
fun CounterButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(onClick = onClick,
        modifier = Modifier.size(50.dp),  //avoid the oval shape
        shape = CircleShape,
        border = null,
        contentPadding = PaddingValues(0.dp),  //avoid the little icon
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
        content = content,
    )
}

@Composable
fun Player(color: Color, health: Int, editClicked: () -> Unit, incr: () -> Unit, decr: () -> Unit, modifier: Modifier = Modifier) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var delta: Int? by rememberSaveable { mutableStateOf(null) }

    LaunchedEffect(key1 = delta) {
        delay(4000)
        delta = null
    }

    Card(modifier = modifier, colors = CardDefaults.cardColors().copy(containerColor = color)) {
        if (isEditing) {
            Box(modifier = Modifier.fillMaxSize(), propagateMinConstraints = true) {
                Text("yaaay")
            }
        } else {
            // makes children overlap each other
            Box(modifier = Modifier.fillMaxSize(), propagateMinConstraints = true) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.matchParentSize(),
                ) {
                    CounterButton(onClick = { decr(); delta = (delta ?: 0) - 1 }) {
                        Icon(Icons.Default.Remove, contentDescription = "Increase counter")
                    }
                    Text(text = "$health", fontSize = 8.em)
                    if (delta != null)
                        Text(text = "( $delta)", fontSize = 8.em)
                    CounterButton(onClick = { incr(); delta = (delta ?: 0) + 1 }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase counter")
                    }
                }

                // the button overlay
                Row(
                    modifier = Modifier
                        .padding(PaddingValues(10.dp))
                        .matchParentSize(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(border = null, onClick = { isEditing = true }) {
                        Icon(
                            Icons.Default.Settings,
                            "Edit counter",
                            tint = Color(0xFF888888)
                        )
                    }
                }
            }
        }
    }
}