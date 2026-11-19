package com.chooloo.www.chooloolib.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
internal fun AlphabetIndex(
    sections: List<Map.Entry<String, Int>>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val active by remember(sections, listState) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            if (!listState.canScrollForward && listState.canScrollBackward) {
                sections.last().key
            } else {
                sections.lastOrNull { it.value <= index }?.key
            }
        }
    }
    val scope = rememberCoroutineScope()
    val description = stringResource(R.string.contact_alphabet_index)
    val accessibility = sections.map {
        CustomAccessibilityAction(stringResource(R.string.contact_jump_section, it.key)) {
            scope.launch { listState.scrollToItem(it.value) }
            true
        }
    }
    BoxWithConstraints(modifier.width(32.dp), contentAlignment = Alignment.TopCenter) {
        val rowHeight = (maxHeight / sections.size).coerceAtMost(20.dp)
        Column(
            modifier = Modifier
                .height(rowHeight * sections.size)
                .clearAndSetSemantics {
                    contentDescription = description
                    active?.let { stateDescription = it }
                    customActions = accessibility
                }
                .pointerInput(sections, listState) {
                    awaitEachGesture {
                        var lastSection = -1
                        fun jumpAt(y: Float) {
                            if (size.height == 0) return
                            val index = (y * sections.size / size.height).toInt()
                                .coerceIn(sections.indices)
                            if (index != lastSection) {
                                lastSection = index
                                scope.launch { listState.scrollToItem(sections[index].value) }
                            }
                        }
                        val down = awaitFirstDown()
                        down.consume()
                        jumpAt(down.position.y)
                        do {
                            val pointer = awaitPointerEvent().changes.firstOrNull {
                                it.id == down.id
                            }
                            if (pointer?.pressed == true) {
                                jumpAt(pointer.position.y)
                                pointer.consume()
                            }
                        } while (pointer?.pressed == true)
                    }
                },
        ) {
            sections.forEach { (label, _) ->
                val selected = label == active
                Box(
                    modifier = Modifier.width(28.dp).height(rowHeight).background(
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
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
}
