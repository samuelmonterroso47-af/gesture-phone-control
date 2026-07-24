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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import com.gesturephonecontrol.app.gesture.GestureDirection
import com.gesturephonecontrol.app.gesture.HandShape

/**
 * Animated diagram of the hand pose and swipe direction a lesson expects. The whole drawing slides
 * back and forth along the gesture's axis so the required motion reads at a glance, without needing
 * video assets.
 */
@Composable
fun HandPoseDiagram(
    direction: GestureDirection,
    requiredShape: HandShape?,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF5B8DEF)
) {
    val transition = rememberInfiniteTransition(label = "gesture-motion")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swipe-progress"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val travel = minOf(w, h) * 0.18f

        val (dx, dy) = when (direction) {
            GestureDirection.UP -> 0f to -travel
            GestureDirection.DOWN -> 0f to travel
            GestureDirection.LEFT -> -travel to 0f
            GestureDirection.RIGHT -> travel to 0f
        }

        val shiftX = dx * progress
        val shiftY = dy * progress

        val palmWidth = w * 0.30f
        val palmHeight = h * 0.26f
        val centerX = w / 2f + shiftX
        val palmTop = h * 0.55f + shiftY
        val strokeWidth = w * 0.035f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        // Palm
        drawRoundedPalm(
            centerX = centerX,
            top = palmTop,
            width = palmWidth,
            height = palmHeight,
            color = color,
            stroke = stroke
        )

        val fingerSpacing = palmWidth / 3.4f
        val fingerBaseY = palmTop
        val extendedTipY = palmTop - h * 0.30f
        val curledTipY = palmTop - h * 0.07f

        val twoFingers = requiredShape == HandShape.TWO_FINGERS
        // index, middle, ring, pinky — outer two curl only for the two-finger pose
        val fingers = listOf(
            -1.5f to true,
            -0.5f to true,
            0.5f to !twoFingers,
            1.5f to !twoFingers
        )

        fingers.forEach { (slot, extended) ->
            val x = centerX + slot * fingerSpacing
            drawLine(
                color = if (extended) color else color.copy(alpha = 0.35f),
                start = Offset(x, fingerBaseY),
                end = Offset(x, if (extended) extendedTipY else curledTipY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Thumb, angled off the side of the palm
        drawLine(
            color = color,
            start = Offset(centerX - palmWidth / 2f, palmTop + palmHeight * 0.30f),
            end = Offset(centerX - palmWidth * 0.85f, palmTop - h * 0.02f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawDirectionArrow(
            direction = direction,
            color = color.copy(alpha = 0.55f),
            strokeWidth = strokeWidth * 0.8f
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundedPalm(
    centerX: Float,
    top: Float,
    width: Float,
    height: Float,
    color: Color,
    stroke: Stroke
) {
    val path = Path().apply {
        val left = centerX - width / 2f
        val right = centerX + width / 2f
        val bottom = top + height
        moveTo(left, top)
        lineTo(right, top)
        lineTo(right, bottom - height * 0.25f)
        quadraticBezierTo(right, bottom, centerX, bottom)
        quadraticBezierTo(left, bottom, left, bottom - height * 0.25f)
        close()
    }
    drawPath(path, color = color, style = stroke)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDirectionArrow(
    direction: GestureDirection,
    color: Color,
    strokeWidth: Float
) {
    val w = size.width
    val h = size.height
    val margin = minOf(w, h) * 0.08f
    val length = minOf(w, h) * 0.20f
    val headSize = length * 0.35f

    val (start, end) = when (direction) {
        GestureDirection.UP -> Offset(w - margin, h * 0.55f) to Offset(w - margin, h * 0.55f - length)
        GestureDirection.DOWN -> Offset(w - margin, h * 0.35f) to Offset(w - margin, h * 0.35f + length)
        GestureDirection.LEFT -> Offset(w * 0.62f, margin) to Offset(w * 0.62f - length, margin)
        GestureDirection.RIGHT -> Offset(w * 0.38f, margin) to Offset(w * 0.38f + length, margin)
    }

    drawLine(color, start, end, strokeWidth = strokeWidth, cap = StrokeCap.Round)

    val (h1, h2) = when (direction) {
        GestureDirection.UP -> Offset(end.x - headSize, end.y + headSize) to Offset(end.x + headSize, end.y + headSize)
        GestureDirection.DOWN -> Offset(end.x - headSize, end.y - headSize) to Offset(end.x + headSize, end.y - headSize)
        GestureDirection.LEFT -> Offset(end.x + headSize, end.y - headSize) to Offset(end.x + headSize, end.y + headSize)
        GestureDirection.RIGHT -> Offset(end.x - headSize, end.y - headSize) to Offset(end.x - headSize, end.y + headSize)
    }
    drawLine(color, end, h1, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color, end, h2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}
