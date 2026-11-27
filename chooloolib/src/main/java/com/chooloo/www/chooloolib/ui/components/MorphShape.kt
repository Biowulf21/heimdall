package com.chooloo.www.chooloolib.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

private const val COOKIE_LOBES = 9

// Same values as the M3 Expressive "fast effects" token (internal in material3 1.4.0).
private val PressSpring =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3800f)

/** Agendula-style scalloped "cookie"; same vertex count as [CircleShapePolygon] so the morph stays smooth. */
val CookiePolygon: RoundedPolygon = RoundedPolygon.star(
    numVerticesPerRadius = COOKIE_LOBES,
    innerRadius = 0.8f,
    rounding = CornerRounding(0.5f),
).normalized()

val CircleShapePolygon: RoundedPolygon =
    RoundedPolygon.circle(numVertices = COOKIE_LOBES).normalized()

/**
 * Draws [morph] at [progress], scaled from its normalized 0..1 bounds to the layout size and turned
 * by [rotation] degrees about its centre.
 */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotation: Float = 0f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress)
        path.transform(
            android.graphics.Matrix().apply {
                setScale(size.width, size.height)
                postRotate(rotation, size.width / 2f, size.height / 2f)
            },
        )
        return Outline.Generic(path.asComposePath())
    }
}

/**
 * Cookie at rest, circle while pressed. Press feedback, so it uses the fast effects spring (no
 * overshoot) and retargets mid-flight when the finger lifts early.
 */
@Composable
fun rememberPressMorphShape(interactionSource: InteractionSource, rotation: Float = 0f): Shape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = PressSpring,
        label = "PressMorph",
    )
    val morph = remember { Morph(CookiePolygon, CircleShapePolygon) }
    return remember(progress, rotation) { MorphShape(morph, progress, rotation) }
}
