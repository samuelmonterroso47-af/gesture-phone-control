package com.gesturephonecontrol.app.gesture

import kotlin.math.abs

enum class GestureDirection { UP, DOWN, LEFT, RIGHT }

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
 * [onHandLost] whenever detection drops the hand. It emits a [GestureDirection] when it sees a
 * fast, dominant-axis displacement within [windowMs], then enforces [cooldownMs] before it will
 * emit again, so a single physical swipe never fires twice.
 *
 * UP additionally requires the deliberate two-finger pose to be held throughout the swipe. Without
 * that, raising a hand into frame — something people do constantly — would scroll the active app.
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

    fun onHandPoint(point: HandPoint): GestureDirection? {
        history.addLast(point)
        while (history.size > 1 && point.timestampMs - history.first().timestampMs > windowMs) {
            history.removeFirst()
        }
        if (history.size < 2) return null
        if (point.timestampMs - lastEmitMs < cooldownMs) return null

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

        if (direction == GestureDirection.UP && history.any { it.shape != HandShape.TWO_FINGERS }) {
            return null
        }

        lastEmitMs = point.timestampMs
        history.clear()
        return direction
    }
}
