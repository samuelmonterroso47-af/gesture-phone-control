package com.gesturephonecontrol.app.gesture

import kotlin.math.abs

/**
 * A single hand's 21 landmarks reduced to plain normalized coordinates, so pose logic stays
 * testable without MediaPipe types. Index order follows MediaPipe's hand landmark model.
 */
data class Landmark(val x: Float, val y: Float)

enum class PointDirection { UP, DOWN, LEFT, RIGHT }

/**
 * A hand pose the app recognizes as a command trigger. Poses are held, not swiped: the user points
 * or makes a shape and keeps it steady, which is far less tiring than a full-arm motion and works
 * while their hands are busy.
 */
sealed interface HandPoseState {
    /** Index finger alone, extended, pointing in [direction]. */
    data class Pointing(val direction: PointDirection) : HandPoseState

    /** Index + middle extended in a "V". */
    data object TwoFingers : HandPoseState

    /** All four fingers extended. */
    data object OpenPalm : HandPoseState

    /** All fingers curled. */
    data object Fist : HandPoseState

    /** A hand is visible but isn't holding any recognized pose. */
    data object None : HandPoseState
}

/**
 * Classifies the hand's pose from its landmarks.
 *
 * The key signal for a pointing pose is the finger's *orientation* — the vector from its base
 * knuckle to its tip — not where the hand sits in the frame. When someone points up and then
 * rotates to point sideways, the hand barely moves through space, so tracking hand position would
 * miss the change entirely.
 *
 * MediaPipe landmark indices used here:
 *  - 0 wrist
 *  - 5/6/8    index MCP / PIP / tip
 *  - 9/10/12  middle MCP / PIP / tip
 *  - 13/14/16 ring MCP / PIP / tip
 *  - 17/18/20 pinky MCP / PIP / tip
 */
object HandPose {

    private const val WRIST = 0
    private const val INDEX_MCP = 5

    private data class Finger(val pip: Int, val tip: Int)

    private val INDEX = Finger(pip = 6, tip = 8)
    private val MIDDLE = Finger(pip = 10, tip = 12)
    private val RING = Finger(pip = 14, tip = 16)
    private val PINKY = Finger(pip = 18, tip = 20)

    /**
     * @param mirrored whether x coordinates are already flipped to match a selfie view, so that
     * "the user's right" reads as [PointDirection.RIGHT].
     */
    fun classify(landmarks: List<Landmark>, mirrored: Boolean = true): HandPoseState {
        if (landmarks.size < 21) return HandPoseState.None

        val indexUp = isExtended(landmarks, INDEX)
        val middleUp = isExtended(landmarks, MIDDLE)
        val ringUp = isExtended(landmarks, RING)
        val pinkyUp = isExtended(landmarks, PINKY)

        return when {
            indexUp && !middleUp && !ringUp && !pinkyUp ->
                HandPoseState.Pointing(indexDirection(landmarks, mirrored))
            indexUp && middleUp && !ringUp && !pinkyUp -> HandPoseState.TwoFingers
            indexUp && middleUp && ringUp && pinkyUp -> HandPoseState.OpenPalm
            !indexUp && !middleUp && !ringUp && !pinkyUp -> HandPoseState.Fist
            else -> HandPoseState.None
        }
    }

    /**
     * Which way the index finger points, from its base knuckle to its tip. Note screen y grows
     * downward, so a finger pointing up has a negative dy.
     */
    fun indexDirection(landmarks: List<Landmark>, mirrored: Boolean = true): PointDirection {
        val base = landmarks[INDEX_MCP]
        val tip = landmarks[INDEX.tip]
        val dx = (tip.x - base.x).let { if (mirrored) -it else it }
        val dy = tip.y - base.y

        return if (abs(dy) >= abs(dx)) {
            if (dy < 0) PointDirection.UP else PointDirection.DOWN
        } else {
            if (dx > 0) PointDirection.RIGHT else PointDirection.LEFT
        }
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
