package com.gesturephonecontrol.app.gesture

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

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

    private const val INDEX_MCP = 5

    /** A finger is "extended" when its PIP joint angle (MCP-PIP-TIP) is close to straight. */
    private const val EXTENDED_ANGLE_DEGREES = 150.0

    private data class Finger(val mcp: Int, val pip: Int, val tip: Int)

    private val INDEX = Finger(mcp = 5, pip = 6, tip = 8)
    private val MIDDLE = Finger(mcp = 9, pip = 10, tip = 12)
    private val RING = Finger(mcp = 13, pip = 14, tip = 16)
    private val PINKY = Finger(mcp = 17, pip = 18, tip = 20)

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

    /**
     * Whether the finger is straight, by the angle at its PIP joint between the MCP and the tip.
     * A straight finger reads close to 180°; a curled one folds back toward the palm, well under
     * that. This is rotation-invariant — unlike comparing tip/pip distance to the wrist, it doesn't
     * depend on how the hand as a whole is tilted or turned relative to the camera, which is what
     * let a curled ring/pinky get misread as extended in some poses.
     */
    private fun isExtended(landmarks: List<Landmark>, finger: Finger): Boolean {
        val mcp = landmarks[finger.mcp]
        val pip = landmarks[finger.pip]
        val tip = landmarks[finger.tip]

        val v1x = mcp.x - pip.x
        val v1y = mcp.y - pip.y
        val v2x = tip.x - pip.x
        val v2y = tip.y - pip.y

        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)
        if (mag1 == 0f || mag2 == 0f) return false

        val cosAngle = ((v1x * v2x + v1y * v2y) / (mag1 * mag2)).coerceIn(-1f, 1f)
        val angleDegrees = Math.toDegrees(acos(cosAngle).toDouble())
        return angleDegrees > EXTENDED_ANGLE_DEGREES
    }
}
