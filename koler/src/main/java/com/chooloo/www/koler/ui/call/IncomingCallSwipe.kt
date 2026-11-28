package com.chooloo.www.koler.ui.call

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.ui.theme.Motion
import com.chooloo.www.chooloolib.ui.theme.Spacing
import com.chooloo.www.koler.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val TrackHeight = 88.dp
private val KnobSize = 72.dp
private const val COMMIT_FRACTION = 0.55f
private const val FLING_FRACTION = 0.25f
private const val FLING_VELOCITY_DP = 900f // a quick flick commits from a quarter of the way
private const val HANGUP_ROTATION = 135f
private val ThresholdHaptic = if (Build.VERSION.SDK_INT >= 34) {
    HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
} else {
    HapticFeedbackConstants.CONTEXT_CLICK
}

/** Release short of the edge: settle back with a touch of give, carrying the finger's velocity. */
private val ReturnSpring = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)

/** Release past the edge: glide the rest of the way, no bounce, then act. */
private val CommitSpring =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

/**
 * Google Phone's answer slider in Heimdall's tones. The knob follows the finger 1:1, the track and
 * knob tint toward the side you are heading for, and the phone tips into the hang-up angle on the
 * way to Decline. Answer and Decline stay tappable labels for TalkBack and for anyone who prefers it.
 */
@Composable
internal fun IncomingCallSwipe(onAccept: () -> Unit, onDecline: () -> Unit) {
    val accept by rememberUpdatedState(onAccept)
    val decline by rememberUpdatedState(onDecline)
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val reduceMotion = remember(view) {
        Settings.Global.getFloat(
            view.context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) ==
            0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        modifier = Modifier.padding(horizontal = Spacing.Large),
    ) {
        Text(
            text = stringResource(R.string.incoming_swipe_hint),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        BoxWithConstraints(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().height(TrackHeight),
        ) {
            val density = LocalDensity.current
            val limit =
                with(density) { ((maxWidth - KnobSize) / 2 - 8.dp).coerceAtLeast(0.dp).toPx() }
            val flingVelocity = with(density) { FLING_VELOCITY_DP.dp.toPx() }
            val offset = remember { Animatable(0f) }
            var dragging by remember { mutableStateOf(false) }
            var committed by remember { mutableStateOf(false) }
            var pastThreshold by remember { mutableStateOf(0) }

            // -1 = fully at Decline, 1 = fully at Answer
            val progress = if (limit > 0f) (offset.value / limit).coerceIn(-1f, 1f) else 0f
            val toAnswer = progress.coerceAtLeast(0f)
            val toDecline = (-progress).coerceAtLeast(0f)

            fun commit(target: Float, action: () -> Unit) {
                committed = true
                scope.launch {
                    offset.animateTo(target, CommitSpring)
                    action()
                }
            }

            val track = lerp(
                lerp(colors.surfaceContainerHigh, colors.primaryContainer, toAnswer * 0.8f),
                colors.errorContainer,
                toDecline * 0.8f,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(track)
                    .pointerInput(limit) {
                        val tracker = VelocityTracker()
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragging = true
                                tracker.resetTracking()
                            },
                            onDragCancel = {
                                dragging = false
                                if (!committed) scope.launch { offset.animateTo(0f, ReturnSpring) }
                            },
                            onDragEnd = {
                                dragging = false
                                val velocity = tracker.calculateVelocity().x
                                val distance = offset.value
                                val flung = abs(velocity) > flingVelocity &&
                                    abs(distance) > limit * FLING_FRACTION &&
                                    (velocity > 0) == (distance > 0)
                                val reach = limit * COMMIT_FRACTION
                                when {
                                    committed || limit <= 0f -> Unit

                                    distance >= reach || (flung && distance > 0) ->
                                        commit(limit) { accept() }

                                    distance <= -reach || (flung && distance < 0) ->
                                        commit(-limit) { decline() }

                                    else -> scope.launch {
                                        offset.animateTo(0f, ReturnSpring, velocity)
                                    }
                                }
                            },
                            onHorizontalDrag = { change, amount ->
                                if (committed) return@detectHorizontalDragGestures
                                change.consume()
                                tracker.addPosition(change.uptimeMillis, change.position)
                                val next = (offset.value + amount).coerceIn(-limit, limit)
                                scope.launch { offset.snapTo(next) }
                                // One tick each time the knob crosses into "release to act"
                                val zone = when {
                                    next >= limit * COMMIT_FRACTION -> 1
                                    next <= -limit * COMMIT_FRACTION -> -1
                                    else -> 0
                                }
                                if (zone != pastThreshold) {
                                    if (zone != 0) view.performHapticFeedback(ThresholdHaptic)
                                    pastThreshold = zone
                                }
                            },
                        )
                    },
            ) {
                SideLabel(
                    text = stringResource(R.string.incoming_decline),
                    color = colors.error,
                    // The label you're leaving fades; the one you're heading for holds
                    alpha = 1f - toAnswer,
                    onClick = { if (!committed) commit(-limit) { decline() } },
                    modifier = Modifier.align(AbsoluteAlignment.CenterLeft),
                )
                SideLabel(
                    text = stringResource(R.string.incoming_answer),
                    color = colors.primary,
                    alpha = 1f - toDecline,
                    onClick = { if (!committed) commit(limit) { accept() } },
                    modifier = Modifier.align(AbsoluteAlignment.CenterRight),
                )

                if (!reduceMotion && !dragging &&
                    offset.value == 0f
                ) {
                    InvitePulse(Modifier.align(Alignment.Center))
                }

                // Blending blue straight into red goes muddy mid-drag, so the hue follows the
                // direction (a quick crossfade) and the distance only sets how strong it is.
                val declining = offset.value < 0f
                val restTone by animateColorAsState(
                    if (declining) colors.errorContainer else colors.primaryContainer,
                    tween(150, easing = Motion.EaseOut),
                    label = "KnobRest",
                )
                val restContent by animateColorAsState(
                    if (declining) colors.onErrorContainer else colors.onPrimaryContainer,
                    tween(150, easing = Motion.EaseOut),
                    label = "KnobRestContent",
                )
                val reach = abs(progress)
                val knob = lerp(restTone, if (declining) colors.error else colors.primary, reach)
                // Pick the icon tone by the knob's brightness: a mid-blend icon vanishes mid-drag
                val knobContent by animateColorAsState(
                    when {
                        knob.luminance() > 0.4f -> restContent
                        declining -> colors.onError
                        else -> colors.onPrimary
                    },
                    tween(120, easing = Motion.EaseOut),
                    label = "KnobContent",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(offset.value.roundToInt(), 0) }
                        .size(KnobSize)
                        .graphicsLayer {
                            val lift = if (dragging) 1.06f else 1f
                            scaleX = lift
                            scaleY = lift
                        }
                        .background(knob, CircleShape),
                ) {
                    Icon(
                        Icons.Rounded.Call,
                        contentDescription = null,
                        tint = knobContent,
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer { rotationZ = toDecline * HANGUP_ROTATION },
                    )
                }
            }
        }
    }
}

@Composable
private fun SideLabel(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    alpha: Float,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.Large)
            .graphicsLayer { this.alpha = alpha },
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

/** A slow ring that breathes out of the knob, so a ringing phone reads as "swipe me". */
@Composable
private fun InvitePulse(modifier: Modifier) {
    val pulse = rememberInfiniteTransition(label = "InvitePulse")
    val t by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            tween(1600, easing = Motion.EaseOut),
            RepeatMode.Restart,
        ),
        label = "InvitePulseProgress",
    )
    Box(
        modifier
            .size(KnobSize)
            .graphicsLayer {
                val s = 1f + t * 0.45f
                scaleX = s
                scaleY = s
                alpha = (1f - t) * 0.35f
            }
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}
