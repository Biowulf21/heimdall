package com.chooloo.www.koler.ui.call

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chooloo.www.koler.R
import kotlin.math.roundToInt

@Composable
internal fun IncomingCallSwipe(onAccept: () -> Unit, onDecline: () -> Unit) {
    val accept by rememberUpdatedState(onAccept)
    val decline by rememberUpdatedState(onDecline)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.incoming_swipe_hint),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 352.dp).fillMaxWidth(),
        ) {
            BoxWithConstraints(Modifier.heightIn(min = 64.dp)) {
                val limit = with(LocalDensity.current) {
                    (maxWidth / 2 - 28.dp).coerceAtLeast(0.dp).toPx()
                }
                val threshold = limit * 0.55f
                var drag by remember { mutableFloatStateOf(0f) }
                var dragging by remember { mutableStateOf(false) }
                val offset by animateFloatAsState(
                    drag,
                    animationSpec = if (dragging) snap() else spring(),
                    label = "Incoming call swipe",
                )
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .pointerInput(limit) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragging = true },
                                onDragCancel = {
                                    dragging = false
                                    drag = 0f
                                },
                                onDragEnd = {
                                    val distance = drag
                                    dragging = false
                                    drag = 0f
                                    when {
                                        threshold <= 0f -> Unit
                                        distance >= threshold -> accept()
                                        distance <= -threshold -> decline()
                                    }
                                },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    drag = (drag + amount).coerceIn(-limit, limit)
                                },
                            )
                        },
                ) {
                    TextButton(
                        onClick = onDecline,
                        modifier = Modifier.align(
                            AbsoluteAlignment.CenterLeft,
                        ).padding(start = 8.dp),
                    ) {
                        Text(
                            stringResource(R.string.incoming_decline),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(
                        onClick = onAccept,
                        modifier = Modifier.align(
                            AbsoluteAlignment.CenterRight,
                        ).padding(end = 8.dp),
                    ) {
                        Text(stringResource(R.string.incoming_answer))
                    }
                    Surface(
                        color = if (drag < -threshold) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.Center)
                            .absoluteOffset { IntOffset(offset.roundToInt(), 0) }
                            .size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Call, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
