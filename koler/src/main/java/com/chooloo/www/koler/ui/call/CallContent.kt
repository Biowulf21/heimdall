package com.chooloo.www.koler.ui.call

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.chooloo.www.chooloolib.R
import com.chooloo.www.chooloolib.ui.components.Avatar
import com.chooloo.www.chooloolib.ui.components.FloatingBubble
import com.chooloo.www.chooloolib.ui.theme.Motion
import com.chooloo.www.chooloolib.ui.theme.Spacing
import com.chooloo.www.koler.domain.enum.AudioRoute
import com.chooloo.www.koler.domain.enum.CallsState

@Composable
fun CallContent(
    callerName: String,
    avatarName: String?,
    status: String?,
    callerPhotoUri: String?,
    callsState: CallsState,
    actions: List<CallAction>,
    audioRoute: AudioRoute?,
    onActionClick: (CallActionType) -> Unit,
    onAccept: () -> Unit,
    onHangup: () -> Unit,
    onManage: () -> Unit,
) {
    val layout = @Composable {
        CallLayout(
            callerName,
            avatarName,
            status,
            callerPhotoUri,
            callsState,
            actions,
            audioRoute,
            onActionClick,
            onAccept,
            onHangup,
            onManage,
        )
    }
    Box(Modifier.fillMaxSize()) {
        if (callerPhotoUri == null) {
            layout()
        } else {
            PhotoBackground(callerPhotoUri)
            // Text and icons sit on a dark scrim, so they switch to light tones.
            val scheme = MaterialTheme.colorScheme.copy(
                onBackground = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Color.White.copy(alpha = 0.8f),
            )
            MaterialTheme(colorScheme = scheme, content = layout)
        }
    }
}

@Composable
private fun PhotoBackground(photoUri: String) {
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(
                LocalContext.current,
            ).data(photoUri).crossfade(300).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.8f)),
                ),
            ),
        )
    }
}

@Composable
private fun CallLayout(
    callerName: String,
    avatarName: String?,
    status: String?,
    callerPhotoUri: String?,
    callsState: CallsState,
    actions: List<CallAction>,
    audioRoute: AudioRoute?,
    onActionClick: (CallActionType) -> Unit,
    onAccept: () -> Unit,
    onHangup: () -> Unit,
    onManage: () -> Unit,
) {
    val moreActions = actions.filter { it.isMore() }
    var moreRequested by rememberSaveable { mutableStateOf(false) }
    val moreOpen = moreRequested && callsState.showsActions && moreActions.isNotEmpty()
    BackHandler(enabled = moreOpen) { moreRequested = false }

    val incoming = callsState == CallsState.INCOMING
    val header = @Composable { CallerHeader(callerName, status) }
    val stage = @Composable { modifier: Modifier ->
        CallStage(
            avatarName = avatarName,
            photoUri = callerPhotoUri,
            incoming = incoming,
            moreOpen = moreOpen,
            moreActions = moreActions,
            onMoreAction = { type ->
                if (type == CallActionType.ADD_CALL) moreRequested = false // a sheet takes over
                onActionClick(type)
            },
            onMoreClose = { moreRequested = false },
            modifier = modifier,
        )
    }
    val controls = @Composable {
        // Answering swaps the slider for the in-call controls: the old set drops away quickly and
        // the new one rises into place, so the hand-off reads as one move rather than a cut.
        AnimatedContent(
            targetState = callsState.isEnded to incoming,
            transitionSpec = {
                (
                    fadeIn(tween(SWAP_IN_MILLIS, delayMillis = 60, easing = Motion.EaseOut)) +
                        slideInVertically(
                            tween(SWAP_IN_MILLIS, delayMillis = 60, easing = Motion.EaseOut),
                        ) {
                            it /
                                5
                        }
                    )
                    .togetherWith(
                        fadeOut(tween(SWAP_OUT_MILLIS, easing = Motion.EaseOut)) +
                            slideOutVertically(tween(SWAP_OUT_MILLIS, easing = Motion.EaseOut)) {
                                it /
                                    8
                            },
                    )
                    .using(SizeTransform(clip = false))
            },
            contentAlignment = Alignment.BottomCenter,
            label = "CallControlsSwap",
        ) { (ended, ringing) ->
            when {
                ended -> Spacer(Modifier.fillMaxWidth())

                ringing -> IncomingCallSwipe(onAccept = onAccept, onDecline = onHangup)

                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.Large),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (callsState.showsActions) {
                        CallActions(
                            actions = actions,
                            onActionClick = onActionClick,
                            audioRoute = audioRoute,
                            moreOpen = moreOpen,
                            onMoreToggle = { moreRequested = it },
                        )
                    }
                    HangupRow(callsState, onHangup, onManage)
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        if (maxWidth > maxHeight) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(
                        1f,
                    ).fillMaxHeight().padding(vertical = Spacing.Large),
                ) {
                    header()
                    stage(Modifier.weight(1f).fillMaxWidth())
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(
                        1f,
                    ).fillMaxHeight().verticalScroll(rememberScrollState()),
                ) { controls() }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(
                    top = Spacing.ExtraLarge * 1.5f,
                    bottom = Spacing.Large,
                ),
            ) {
                header()
                stage(Modifier.weight(1f).fillMaxWidth())
                controls()
            }
        }
    }
}

/**
 * The middle of the screen: the caller's avatar, big while it rings (Google Phone's incoming look)
 * and settling smaller once the call is live; the More panel takes its place when open.
 */
@Composable
private fun CallStage(
    avatarName: String?,
    photoUri: String?,
    incoming: Boolean,
    moreOpen: Boolean,
    moreActions: List<CallAction>,
    onMoreAction: (CallActionType) -> Unit,
    onMoreClose: () -> Unit,
    modifier: Modifier,
) {
    val avatarScale by animateFloatAsState(
        targetValue = when {
            moreOpen -> 0.6f
            incoming -> 1f
            else -> 0.75f
        },
        animationSpec = tween(STAGE_MILLIS, easing = Motion.EaseOut),
        label = "AvatarScale",
    )
    val avatarAlpha by animateFloatAsState(
        targetValue = if (moreOpen) 0f else 1f,
        animationSpec = tween(PANEL_OUT_MILLIS, easing = Motion.EaseOut),
        label = "AvatarAlpha",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(vertical = Spacing.Medium),
    ) {
        Avatar(
            name = avatarName,
            photoUri = photoUri,
            size = StageAvatar,
            modifier = Modifier.graphicsLayer {
                scaleX = avatarScale
                scaleY = avatarScale
                alpha = avatarAlpha
            },
        )
        AnimatedVisibility(
            visible = moreOpen,
            // Grows out of the More button's corner of the screen, leaves the same way, faster
            enter = fadeIn(tween(PANEL_IN_MILLIS, easing = Motion.EaseOut)) +
                scaleIn(
                    tween(PANEL_IN_MILLIS, easing = Motion.EaseOut),
                    0.94f,
                    TransformOrigin(0.85f, 1f),
                ),
            exit = fadeOut(tween(PANEL_OUT_MILLIS, easing = Motion.EaseOut)) +
                scaleOut(
                    tween(PANEL_OUT_MILLIS, easing = Motion.EaseOut),
                    0.96f,
                    TransformOrigin(0.85f, 1f),
                ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CallMorePanel(
                actions = moreActions,
                onActionClick = onMoreAction,
                onClose = onMoreClose,
            )
        }
    }
}

@Composable
private fun CallerHeader(name: String, status: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = Modifier.padding(horizontal = Spacing.Large),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HangupRow(callsState: CallsState, onHangup: () -> Unit, onManage: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Large),
    ) {
        if (callsState == CallsState.ACTIVE_MULTI) {
            FloatingBubble(
                icon = Icons.Rounded.PeopleAlt,
                contentDescription = stringResource(R.string.cd_manage_call),
                onClick = onManage,
                size = HangupHeight,
                hasShadow = false,
            )
        }
        EndCallButton(onHangup)
    }
}

/**
 * Google Phone's wide red pill, made deliberately large: the one button that must never be missed.
 * Pressing squares the corners slightly and sinks it, on the same fast spring as other presses.
 */
@Composable
private fun EndCallButton(onHangup: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, PressSpring, label = "HangupPress")
    val corner by animateDpAsState(
        if (pressed) 28.dp else HangupHeight / 2,
        tween(120, easing = Motion.EaseOut),
        label = "HangupCorner",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .fillMaxWidth(0.62f)
            .height(HangupHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.error)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onError),
                role = Role.Button,
                onClick = onHangup,
            ),
    ) {
        Icon(
            Icons.Rounded.CallEnd,
            contentDescription = stringResource(R.string.cd_hangup_call),
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(34.dp),
        )
    }
}

private val StageAvatar = 168.dp
private val HangupHeight = 80.dp
private const val STAGE_MILLIS = 360 // the avatar settling after Answer: a bridge, not a UI tap
private const val SWAP_IN_MILLIS = 280
private const val SWAP_OUT_MILLIS = 140
private const val PANEL_IN_MILLIS = 220
private const val PANEL_OUT_MILLIS = 150
private val PressSpring =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3800f)

fun callStatus(elapsedSeconds: Long?, stateLabel: String?): String? =
    elapsedSeconds?.let(DateUtils::formatElapsedTime) ?: stateLabel
