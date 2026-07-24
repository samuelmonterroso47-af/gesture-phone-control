package com.gesturephonecontrol.app.training

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.gesturephonecontrol.app.gesture.HandPoseState
import com.gesturephonecontrol.app.gesture.PointDirection

/**
 * Diagram of the pose a lesson expects. Poses are held rather than swiped, so instead of animating
 * a motion path this draws the hand itself and pulses gently — the cue is "make this shape and keep
 * it there", not "move this way".
 *
 * Everything is drawn as if pointing up, then rotated, so one piece of geometry covers all four
 * pointing directions.
 */
@Composable
fun HandPoseDiagram(
    pose: HandPoseState,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF5B8DEF)
) {
    val transition = rememberInfiniteTransition(label = "pose-hold")
    val pulse by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val rotationDegrees = when (pose) {
            is HandPoseState.Pointing -> when (pose.direction) {
                PointDirection.UP -> 0f
                PointDirection.RIGHT -> 90f
                PointDirection.DOWN -> 180f
                PointDirection.LEFT -> -90f
            }
            else -> 0f
        }

        val extendedFingers = when (pose) {
            is HandPoseState.Pointing -> 1
            HandPoseState.TwoFingers -> 2
            HandPoseState.OpenPalm -> 4
            else -> 0
        }

        rotate(degrees = rotationDegrees, pivot = center) {
            drawHand(
                extendedFingers = extendedFingers,
                scale = pulse,
                color = color
            )
        }
    }
}

/**
 * Draws a stylised hand pointing up: a rounded palm, [extendedFingers] fingers standing up, and the
 * rest as knuckle stubs.
 */
private fun DrawScope.drawHand(extendedFingers: Int, scale: Float, color: Color) {
    val w = size.width
    val h = size.height
    val centerX = w / 2f
    val palmWidth = w * 0.34f * scale
    val palmHeight = h * 0.26f * scale
    val palmTop = h * 0.56f
    val palmCenterY = palmTop + palmHeight / 2f

    val stroke = Stroke(width = w * 0.035f, cap = StrokeCap.Round)

    drawRoundedPalm(centerX, palmCenterY, palmWidth, palmHeight, color, stroke)

    // Four finger slots across the top of the palm; the leftmost ones are the extended ones.
    val slotSpacing = palmWidth / 4f
    val firstSlotX = centerX - palmWidth / 2f + slotSpacing / 2f
    val fingerLength = h * 0.28f * scale
    val stubLength = h * 0.05f * scale

    repeat(4) { i ->
        val x = firstSlotX + i * slotSpacing
        val extended = i < extendedFingers
        val length = if (extended) fingerLength else stubLength
        drawLine(
            color = color,
            start = Offset(x, palmTop),
            end = Offset(x, palmTop - length),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }

    // Thumb, tucked against the side of the palm.
    drawLine(
        color = color,
        start = Offset(centerX + palmWidth / 2f, palmCenterY + palmHeight * 0.15f),
        end = Offset(centerX + palmWidth / 2f + w * 0.10f * scale, palmCenterY - palmHeight * 0.25f),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawRoundedPalm(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    color: Color,
    stroke: Stroke
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(centerX - width / 2f, centerY - height / 2f),
        size = androidx.compose.ui.geometry.Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.25f),
        style = stroke
    )
}
