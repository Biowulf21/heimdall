package com.chooloo.www.koler.ui.call

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.ui.theme.Motion
import com.chooloo.www.chooloolib.ui.theme.Spacing
import com.chooloo.www.koler.R
import com.chooloo.www.koler.domain.enum.AudioRoute
import com.chooloo.www.koler.ui.common.CallLabels

/** The row Google Phone keeps on screen; everything else lives behind More. */
private val PrimaryActions =
    listOf(CallActionType.KEYPAD, CallActionType.MUTE, CallActionType.SPEAKER)

/** Toggle state changes: colour and corner retarget mid-flight if the user taps again. */
private fun <T> toggleSpec() = tween<T>(durationMillis = 200, easing = Motion.EaseOut)

/** Press feedback: fast, no overshoot, follows the finger (same as the bubble press morph). */
private val PressSpring =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3800f)

private const val PRESSED_SCALE = 0.92f
private val ButtonHeight = 72.dp // big, thumb-friendly; the label below is part of the target too
private val RestCorner = ButtonHeight / 2 // stadium at rest
private val ActiveCorner = 22.dp // squarer when on, the M3 Expressive toggle cue

internal fun CallAction.isMore() = type !in PrimaryActions

@Composable
fun CallActions(
    actions: List<CallAction>,
    onActionClick: (CallActionType) -> Unit,
    audioRoute: AudioRoute?,
    moreOpen: Boolean,
    onMoreToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = PrimaryActions.mapNotNull { type -> actions.firstOrNull { it.type == type } }
    val hasMore = actions.any { it.isMore() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.Page),
        modifier = modifier.widthIn(
            max = 420.dp,
        ).fillMaxWidth().padding(horizontal = Spacing.Large),
    ) {
        primary.forEach { action ->
            CallToggleButton(
                icon = action.type.icon,
                label = stringResource(actionLabel(action.type, audioRoute)),
                isOn = action.isActive,
                enabled = action.isEnabled,
                onToggle = { onActionClick(action.type) },
                modifier = Modifier.weight(1f),
            )
        }
        if (hasMore) {
            CallToggleButton(
                icon = Icons.Rounded.MoreVert,
                label = stringResource(R.string.call_action_more),
                isOn = moreOpen,
                enabled = true,
                onToggle = onMoreToggle,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun actionLabel(type: CallActionType, audioRoute: AudioRoute?): Int =
    if (type == CallActionType.SPEAKER) {
        audioRoute?.let(CallLabels::labelRes) ?: R.string.call_audio_output
    } else {
        type.labelRes
    }

/**
 * Google Phone's in-call toggle in Heimdall's terms: tonal stadium at rest, filled primary with
 * squarer corners when on. Colour, corner and content all move together on one 200ms ease-out so
 * the state change reads as a single gesture.
 */
@Composable
private fun CallToggleButton(
    icon: ImageVector,
    label: String,
    isOn: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val container by animateColorAsState(
        if (isOn) colors.primary else colors.surfaceContainerHigh,
        toggleSpec(),
        label = "ToggleContainer",
    )
    val content by animateColorAsState(
        if (isOn) colors.onPrimary else colors.onSurface,
        toggleSpec(),
        label = "ToggleContent",
    )
    val corner by animateDpAsState(
        when {
            isOn -> ActiveCorner

            pressed -> ActiveCorner + 6.dp

            // a hint of the on shape under the finger
            else -> RestCorner
        },
        toggleSpec(),
        label = "ToggleCorner",
    )
    val scale by animateFloatAsState(
        if (pressed) PRESSED_SCALE else 1f,
        PressSpring,
        label = "TogglePress",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .toggleable(
                value = isOn,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = onToggle,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(corner))
                .background(container),
        ) {
            Icon(icon, contentDescription = null, tint = content)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Google Phone's More sheet, drawn as Heimdall's grouped card: rows with round badges, the active
 * one filled. Shown in place of the avatar; the caller opens and closes it.
 */
@Composable
internal fun CallMorePanel(
    actions: List<CallAction>,
    onActionClick: (CallActionType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(OUTER),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.widthIn(
            max = 420.dp,
        ).fillMaxWidth().padding(horizontal = Spacing.Large),
    ) {
        Column(
            Modifier.padding(start = Spacing.Small, end = Spacing.Small, bottom = Spacing.Small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = Spacing.Medium),
            ) {
                Text(
                    stringResource(R.string.call_more_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.call_more_close))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                actions.forEachIndexed { index, action ->
                    val top = if (index == 0) OUTER - Spacing.Small else INNER
                    val bottom = if (index == actions.lastIndex) OUTER - Spacing.Small else INNER
                    MoreRow(action, RoundedCornerShape(top, top, bottom, bottom)) {
                        onActionClick(action.type)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreRow(action: CallAction, shape: RoundedCornerShape, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val row by animateColorAsState(
        if (action.isActive) colors.secondaryContainer else colors.surfaceContainer,
        toggleSpec(),
        label = "MoreRow",
    )
    val badge by animateColorAsState(
        if (action.isActive) colors.secondaryContainer else colors.surfaceContainerHighest,
        toggleSpec(),
        label = "MoreBadge",
    )
    val content = if (action.isActive) colors.onSecondaryContainer else colors.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .graphicsLayer { alpha = if (action.isEnabled) 1f else 0.38f }
            .clip(shape)
            .background(row)
            .clickable(enabled = action.isEnabled, role = Role.Button, onClick = onClick)
            .padding(start = Spacing.Small, end = Spacing.Medium),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).background(badge, CircleShape),
        ) {
            Icon(action.type.icon, contentDescription = null, tint = content)
        }
        Spacer(Modifier.width(Spacing.Medium))
        Text(
            stringResource(action.type.labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = content,
            modifier = Modifier.weight(1f),
        )
        if (action.isActive) {
            Text(
                stringResource(R.string.call_action_on),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimary,
                modifier = Modifier
                    .background(colors.primary, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

private val OUTER: Dp = 28.dp
private val INNER: Dp = 4.dp
