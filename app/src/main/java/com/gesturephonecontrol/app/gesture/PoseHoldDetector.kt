package com.gesturephonecontrol.app.gesture

/**
 * Fires a command when the user holds one recognized pose steady, rather than when they swipe.
 *
 * Holding is what makes this usable one-handed while eating: no full-arm motion, and the hand can
 * stay resting in front of the camera. The cost is that a pose you're merely passing through would
 * fire too, so a pose must be held for [dwellMs] before it counts.
 *
 * While a pose stays held it repeats every [repeatMs], which is what makes scrolling practical —
 * you keep pointing and the list keeps moving, instead of re-making the gesture for each notch.
 * Changing or dropping the pose resets everything, so releasing always stops the repeat.
 */
class PoseHoldDetector(
    private val dwellMs: Long = 450,
    private val repeatMs: Long = 700
) {
    private var heldPose: HandPoseState? = null
    private var heldSinceMs: Long = 0
    private var lastFireMs: Long = 0

    fun reset() {
        heldPose = null
    }

    /** A pose that has been held long enough to act on. [isRepeat] is false on the first fire. */
    data class Fire(val pose: HandPoseState, val isRepeat: Boolean)

    /**
     * @return the pose to act on for this frame, or null if nothing should fire yet.
     */
    fun onPose(pose: HandPoseState, timestampMs: Long): Fire? {
        if (pose == HandPoseState.None) {
            reset()
            return null
        }

        if (pose != heldPose) {
            heldPose = pose
            heldSinceMs = timestampMs
            lastFireMs = 0
            return null
        }

        val heldFor = timestampMs - heldSinceMs
        if (heldFor < dwellMs) return null

        val isFirstFire = lastFireMs == 0L
        if (isFirstFire) {
            lastFireMs = timestampMs
            return Fire(pose, isRepeat = false)
        }
        if (timestampMs - lastFireMs >= repeatMs) {
            lastFireMs = timestampMs
            return Fire(pose, isRepeat = true)
        }
        return null
    }
}
