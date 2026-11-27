package com.chooloo.www.chooloolib.ui.components.list

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.chooloo.www.chooloolib.domain.enum.DesignStyle
import com.chooloo.www.chooloolib.ui.theme.LocalDesignStyle

/** Where a row sits in its section; Expressive uses it to draw grouped cards. Null outside lists. */
enum class ListItemPosition {
    FIRST,
    MIDDLE,
    LAST,
    SINGLE,
    ;

    companion object {
        fun of(isFirst: Boolean, isLast: Boolean) = when {
            isFirst && isLast -> SINGLE
            isFirst -> FIRST
            isLast -> LAST
            else -> MIDDLE
        }
    }
}

val LocalListItemPosition = compositionLocalOf<ListItemPosition?> { null }

private val OuterCorner = 28.dp
private val InnerCorner = 4.dp
private val PillCorner = 40.dp
private val GroupGap = 3.dp
private const val RELEASE_MILLIS = 150

@Composable
fun ListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    startContainer: @Composable (() -> Unit)? = null,
    endContainer: @Composable (() -> Unit)? = null,
    animateTitle: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val position = LocalListItemPosition.current
    val isGrouped = LocalDesignStyle.current == DesignStyle.EXPRESSIVE && position != null
    val shape = if (isGrouped) {
        rememberGroupedShape(position!!, interactionSource)
    } else {
        MaterialTheme.shapes.extraLarge
    }

    MaterialListItem(
        headlineContent = {
            if (animateTitle) {
                Crossfade(targetState = title, animationSpec = tween(180), label = "Title") {
                    Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        supportingContent = subtitle?.let {
            { Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        leadingContent = startContainer,
        trailingContent = endContainer,
        colors = ListItemDefaults.colors(
            containerColor = if (isGrouped) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                Color.Transparent
            },
        ),
        modifier = Modifier
            .then(
                if (isGrouped &&
                    position.hasGapBelow
                ) {
                    Modifier.padding(bottom = GroupGap)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .then(modifier)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            ),
    )
}

private val ListItemPosition?.hasGapBelow
    get() = this == ListItemPosition.FIRST || this == ListItemPosition.MIDDLE

/**
 * Grouped-card corners that round toward a pill while the row is held. The rounding runs over the
 * long-press timeout, so it reads as "keep holding", then eases back quickly on release.
 */
@Composable
private fun rememberGroupedShape(
    position: ListItemPosition,
    interactionSource: MutableInteractionSource,
): Shape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val holdMillis = LocalViewConfiguration.current.longPressTimeoutMillis.toInt()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
        } else {
            progress.animateTo(0f, tween(RELEASE_MILLIS))
        }
    }
    val roundTop = position == ListItemPosition.FIRST || position == ListItemPosition.SINGLE
    val roundBottom = position == ListItemPosition.LAST || position == ListItemPosition.SINGLE
    fun corner(isOuter: Boolean): Dp =
        lerp(if (isOuter) OuterCorner else InnerCorner, PillCorner, progress.value)
    return RoundedCornerShape(
        topStart = corner(roundTop),
        topEnd = corner(roundTop),
        bottomStart = corner(roundBottom),
        bottomEnd = corner(roundBottom),
    )
}
