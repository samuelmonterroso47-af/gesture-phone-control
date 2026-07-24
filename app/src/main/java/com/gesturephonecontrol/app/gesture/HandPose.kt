package com.gesturephonecontrol.app.gesture

/**
 * A single hand's 21 landmarks reduced to plain normalized coordinates, so pose logic stays
 * testable without MediaPipe types. Index order follows MediaPipe's hand landmark model.
 */
data class Landmark(val x: Float, val y: Float)

enum class HandShape {
    /** Index + middle extended, ring + pinky curled — the deliberate "two fingers" pose. */
    TWO_FINGERS,

    /** All four fingers extended. */
    OPEN_PALM,

    /** All four fingers curled. */
    FIST,

    /** Any other combination — tracked for motion, but not a command pose. */
    OTHER
}

/**
 * Classifies the hand's shape from its landmarks, so a gesture can require a deliberate pose
 * (two fingers up) instead of firing on any hand movement that happens to pass the camera.
 *
 * MediaPipe landmark indices used here:
 *  - 0 wrist
 *  - 5/6/8    index MCP / PIP / tip
 *  - 9/10/12  middle MCP / PIP / tip
 *  - 13/14/16 ring MCP / PIP / tip
 *  - 17/18/20 pinky MCP / PIP / tip
 *
 * A finger counts as extended when its tip is farther from the wrist than its PIP joint. This is
 * orientation-agnostic (works with the hand tilted), unlike comparing raw y values.
 */
object HandPose {

    private const val WRIST = 0

    private data class Finger(val pip: Int, val tip: Int)

    private val INDEX = Finger(pip = 6, tip = 8)
    private val MIDDLE = Finger(pip = 10, tip = 12)
    private val RING = Finger(pip = 14, tip = 16)
    private val PINKY = Finger(pip = 18, tip = 20)

    fun classify(landmarks: List<Landmark>): HandShape {
        if (landmarks.size < 21) return HandShape.OTHER

        val indexUp = isExtended(landmarks, INDEX)
        val middleUp = isExtended(landmarks, MIDDLE)
        val ringUp = isExtended(landmarks, RING)
        val pinkyUp = isExtended(landmarks, PINKY)

        return when {
            indexUp && middleUp && !ringUp && !pinkyUp -> HandShape.TWO_FINGERS
            indexUp && middleUp && ringUp && pinkyUp -> HandShape.OPEN_PALM
            !indexUp && !middleUp && !ringUp && !pinkyUp -> HandShape.FIST
            else -> HandShape.OTHER
        }
    }

    /** Midpoint between the index and middle fingertips — the anchor point a two-finger swipe tracks. */
    fun twoFingerAnchor(landmarks: List<Landmark>): Landmark {
        val indexTip = landmarks[INDEX.tip]
        val middleTip = landmarks[MIDDLE.tip]
        return Landmark(
            x = (indexTip.x + middleTip.x) / 2f,
            y = (indexTip.y + middleTip.y) / 2f
        )
    }

    /** Centroid of wrist + the four MCP knuckles — a stable anchor for whole-hand motion. */
    fun palmCentroid(landmarks: List<Landmark>): Landmark {
        val palmIndices = intArrayOf(0, 5, 9, 13, 17)
        var sumX = 0f
        var sumY = 0f
        for (i in palmIndices) {
            sumX += landmarks[i].x
            sumY += landmarks[i].y
        }
        return Landmark(sumX / palmIndices.size, sumY / palmIndices.size)
    }

    private fun isExtended(landmarks: List<Landmark>, finger: Finger): Boolean {
        val wrist = landmarks[WRIST]
        return distance(landmarks[finger.tip], wrist) > distance(landmarks[finger.pip], wrist)
    }

    private fun distance(a: Landmark, b: Landmark): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy // squared distance is enough for comparisons
    }
}
