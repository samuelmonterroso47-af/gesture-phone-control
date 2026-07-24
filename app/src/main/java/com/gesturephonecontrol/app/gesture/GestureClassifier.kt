package com.gesturephonecontrol.app.gesture

import kotlin.math.abs

enum class GestureDirection { UP, DOWN, LEFT, RIGHT }

/**
 * A recognized swipe: which hand pose was held throughout it, and which way it went. The pose acts
 * as a mode selector, so the same direction can mean different things depending on the hand shape.
 */
data class GestureEvent(val shape: HandShape, val direction: GestureDirection)

/** Normalized (0..1) anchor position for a single camera frame, plus the hand shape in that frame. */
data class HandPoint(
    val x: Float,
    val y: Float,
    val timestampMs: Long,
    val shape: HandShape = HandShape.OTHER
)

/**
 * Pure logic gesture classifier: no Android/CameraX/MediaPipe types, fully unit-testable.
 *
 * Feed it the tracked anchor position for every frame in which a hand is detected, and call
 * [onHandLost] whenever detection drops the hand. It emits a [GestureEvent] when it sees a fast,
 * dominant-axis displacement within [windowMs], then enforces [cooldownMs] before it will emit
 * again, so a single physical swipe never fires twice.
 *
 * A swipe only counts if one deliberate pose was held for its whole duration. That rules out both
 * incidental hand movement (an unposed hand passing the camera) and the ambiguous middle of a
 * transition between poses, which would otherwise fire the wrong command.
 */
class GestureClassifier(
    private val minDisplacement: Float = 0.18f,
    private val minSpeed: Float = 0.6f, // normalized units per second
    private val cooldownMs: Long = 700,
    private val windowMs: Long = 350
) {
    private val history = ArrayDeque<HandPoint>()
    private var lastEmitMs: Long = Long.MIN_VALUE

    fun onHandLost() {
        history.clear()
    }

    fun onHandPoint(point: HandPoint): GestureEvent? {
        history.addLast(point)
        while (history.size > 1 && point.timestampMs - history.first().timestampMs > windowMs) {
            history.removeFirst()
        }
        if (history.size < 2) return null
        if (point.timestampMs - lastEmitMs < cooldownMs) return null

        val shape = point.shape
        if (shape == HandShape.OTHER) return null
        if (history.any { it.shape != shape }) return null

        val oldest = history.first()
        val dx = point.x - oldest.x
        val dy = point.y - oldest.y
        val dtSeconds = (point.timestampMs - oldest.timestampMs).coerceAtLeast(1) / 1000f

        val absDx = abs(dx)
        val absDy = abs(dy)
        val dominantDisplacement = maxOf(absDx, absDy)
        if (dominantDisplacement < minDisplacement) return null

        val speed = dominantDisplacement / dtSeconds
        if (speed < minSpeed) return null

        val direction = if (absDx > absDy) {
            if (dx > 0) GestureDirection.RIGHT else GestureDirection.LEFT
        } else {
            if (dy > 0) GestureDirection.DOWN else GestureDirection.UP
        }

        lastEmitMs = point.timestampMs
        history.clear()
        return GestureEvent(shape, direction)
    }
}
