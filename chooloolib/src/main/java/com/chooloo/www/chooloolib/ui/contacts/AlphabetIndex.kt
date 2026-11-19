package com.chooloo.www.chooloolib.ui.contacts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chooloo.www.chooloolib.R
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun scrollTarget(fraction: Float, lastIndex: Int): Int =
    (fraction.coerceIn(0f, 1f) * (lastIndex + 1))
        .toInt().coerceIn(0, lastIndex.coerceAtLeast(0))

internal fun alphabetBend(distance: Float): Float {
    val relative = distance / 144f
    val strength = (1f - relative * relative).coerceAtLeast(0f)
    return strength * strength
}

@Composable
internal fun AlphabetIndex(
    sections: List<Map.Entry<String, Int>>,
    listState: LazyListState,
    onLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    val interacting = listState.isScrollInProgress || dragIndex != null
    var showLetter by remember { mutableStateOf(false) }
    var previewRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(interacting, previewRequest) {
        if (interacting || previewRequest > 0) showLetter = true
        if (!interacting) {
            delay(850)
            showLetter = false
        }
    }

    val first = listState.firstVisibleItemIndex
    val atEnd = !listState.canScrollForward && listState.canScrollBackward
    val active = dragIndex?.coerceIn(sections.indices) ?: if (atEnd) {
        sections.lastIndex
    } else {
        sections.indexOfLast { it.value <= first }.coerceAtLeast(0)
    }
    val letter = sections[active].key
    val description = stringResource(R.string.contact_alphabet_index)
    val accessibility = sections.map {
        CustomAccessibilityAction(stringResource(R.string.contact_jump_section, it.key)) {
            scope.launch { listState.scrollToItem(it.value) }
            true
        }
    }

    val edge = if (onLeft) AbsoluteAlignment.TopLeft else AbsoluteAlignment.TopRight
    BoxWithConstraints(modifier.fillMaxHeight().width(120.dp)) {
        val rowHeight = maxHeight / sections.size
        val highlightHeight = rowHeight.coerceAtMost(24.dp)
        val direction = if (onLeft) 1 else -1
        val bend by animateFloatAsState(
            targetValue = if (showLetter) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "Alphabet curve",
        )
        val maximumInset = 34.dp * direction * bend
        val center by animateDpAsState(
            targetValue = rowHeight * (active + 0.5f),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "Alphabet highlight position",
        )
        Box(
            Modifier
                .align(edge)
                .fillMaxHeight()
                .width(32.dp)
                .clearAndSetSemantics {
                    contentDescription = description
                    stateDescription = letter
                    customActions = accessibility
                }
                .pointerInput(sections, listState) {
                    awaitEachGesture {
                        var previous = -1
                        var pending: Job? = null
                        fun move(y: Float) {
                            if (size.height == 0) return
                            val target = scrollTarget(y / size.height, sections.lastIndex)
                            dragIndex = target
                            if (target != previous) {
                                previous = target
                                pending?.cancel()
                                pending = scope.launch {
                                    listState.scrollToItem(sections[target].value)
                                }
                            }
                        }
                        val down = awaitFirstDown()
                        try {
                            previewRequest++
                            down.consume()
                            move(down.position.y)
                            do {
                                val pointer = awaitPointerEvent().changes.firstOrNull {
                                    it.id == down.id
                                }
                                if (pointer?.pressed == true) {
                                    move(pointer.position.y)
                                    pointer.consume()
                                }
                            } while (pointer?.pressed == true)
                        } finally {
                            dragIndex = null
                        }
                    }
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .absoluteOffset(x = maximumInset, y = center - highlightHeight / 2)
                    .width(24.dp)
                    .height(highlightHeight)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            )
            Column {
                sections.forEachIndexed { index, (label, _) ->
                    val distance = abs((center - rowHeight * (index + 0.5f)).value)
                    Box(
                        Modifier.fillMaxWidth()
                            .height(rowHeight)
                            .absoluteOffset(x = maximumInset * alphabetBend(distance)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (distance < highlightHeight.value / 2) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        AnimatedContent(
            targetState = letter.takeIf { showLetter },
            transitionSpec = {
                val inward = if (onLeft) -1 else 1
                (
                    fadeIn(tween(120)) + scaleIn(
                        initialScale = 0.77f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ) + slideInHorizontally(tween(150)) { inward * it / 4 }
                    )
                    .togetherWith(fadeOut(tween(90)))
            },
            modifier = Modifier
                .align(edge)
                .absoluteOffset(
                    x = 32.dp * direction + maximumInset,
                    y = (center - 22.dp).coerceIn(0.dp, (maxHeight - 44.dp).coerceAtLeast(0.dp)),
                )
                .size(44.dp)
                .clearAndSetSemantics {},
            label = "Contact section",
        ) { label ->
            if (label != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}
