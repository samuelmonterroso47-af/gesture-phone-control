package com.gesturephonecontrol.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class HandPoseTest {

    /**
     * Builds a 21-landmark hand. The wrist sits at the bottom; each finger's tip is placed either
     * beyond its PIP joint (extended) or short of it (curled), matching how [HandPose] decides.
     * [indexTip] lets a test aim the index finger somewhere specific.
     */
    private fun hand(
        indexUp: Boolean,
        middleUp: Boolean,
        ringUp: Boolean,
        pinkyUp: Boolean,
        indexTip: Landmark? = null
    ): List<Landmark> {
        val marks = MutableList(21) { Landmark(0.5f, 0.9f) }
        marks[0] = Landmark(0.5f, 0.9f) // wrist
        marks[5] = Landmark(0.44f, 0.7f) // index MCP

        fun place(pip: Int, tip: Int, x: Float, extended: Boolean) {
            marks[pip] = Landmark(x, 0.6f)
            marks[tip] = Landmark(x, if (extended) 0.4f else 0.75f)
        }
        place(6, 8, 0.44f, indexUp)
        place(10, 12, 0.50f, middleUp)
        place(14, 16, 0.56f, ringUp)
        place(18, 20, 0.62f, pinkyUp)

        if (indexTip != null) marks[8] = indexTip
        return marks
    }

    @Test
    fun `index alone pointing up is Pointing UP`() {
        val pose = HandPose.classify(hand(indexUp = true, middleUp = false, ringUp = false, pinkyUp = false))
        assertEquals(HandPoseState.Pointing(PointDirection.UP), pose)
    }

    @Test
    fun `index alone pointing down is Pointing DOWN`() {
        // Tip below the MCP joint, still far enough from the wrist to count as extended.
        val pose = HandPose.classify(
            hand(
                indexUp = true, middleUp = false, ringUp = false, pinkyUp = false,
                indexTip = Landmark(0.44f, 0.95f)
            )
        )
        assertEquals(HandPoseState.Pointing(PointDirection.DOWN), pose)
    }

    @Test
    fun `a horizontal index reads as a sideways point, not up or down`() {
        // Mirrored: tip further left in raw coords becomes the user's RIGHT.
        val pose = HandPose.classify(
            hand(
                indexUp = true, middleUp = false, ringUp = false, pinkyUp = false,
                indexTip = Landmark(0.10f, 0.68f)
            )
        )
        assertEquals(HandPoseState.Pointing(PointDirection.RIGHT), pose)
    }

    @Test
    fun `index and middle extended is TwoFingers`() {
        val pose = HandPose.classify(hand(indexUp = true, middleUp = true, ringUp = false, pinkyUp = false))
        assertEquals(HandPoseState.TwoFingers, pose)
    }

    @Test
    fun `all fingers curled is Fist`() {
        val pose = HandPose.classify(hand(indexUp = false, middleUp = false, ringUp = false, pinkyUp = false))
        assertEquals(HandPoseState.Fist, pose)
    }

    @Test
    fun `a fully open hand is not a command pose`() {
        val pose = HandPose.classify(hand(indexUp = true, middleUp = true, ringUp = true, pinkyUp = true))
        assertEquals(HandPoseState.None, pose)
    }

    @Test
    fun `incomplete landmark list is not a command pose`() {
        assertEquals(HandPoseState.None, HandPose.classify(listOf(Landmark(0.5f, 0.5f))))
    }
}
