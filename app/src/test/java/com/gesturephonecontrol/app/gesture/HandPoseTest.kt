package com.gesturephonecontrol.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class HandPoseTest {

    /**
     * Builds an upright hand with the wrist at the bottom. Each finger is given a PIP and a tip;
     * an extended finger's tip sits farther from the wrist than its PIP, a curled one's doesn't.
     */
    private fun hand(
        indexUp: Boolean,
        middleUp: Boolean,
        ringUp: Boolean,
        pinkyUp: Boolean
    ): List<Landmark> {
        val points = MutableList(21) { Landmark(0.5f, 0.9f) }
        points[0] = Landmark(0.5f, 0.9f) // wrist

        fun setFinger(x: Float, pip: Int, tip: Int, extended: Boolean) {
            points[pip] = Landmark(x, 0.6f)
            points[tip] = Landmark(x, if (extended) 0.3f else 0.7f)
        }

        setFinger(0.40f, pip = 6, tip = 8, extended = indexUp)
        setFinger(0.47f, pip = 10, tip = 12, extended = middleUp)
        setFinger(0.54f, pip = 14, tip = 16, extended = ringUp)
        setFinger(0.61f, pip = 18, tip = 20, extended = pinkyUp)

        return points
    }

    @Test
    fun `index and middle extended with others curled is TWO_FINGERS`() {
        val shape = HandPose.classify(hand(indexUp = true, middleUp = true, ringUp = false, pinkyUp = false))
        assertEquals(HandShape.TWO_FINGERS, shape)
    }

    @Test
    fun `all four fingers extended is OPEN_PALM`() {
        val shape = HandPose.classify(hand(indexUp = true, middleUp = true, ringUp = true, pinkyUp = true))
        assertEquals(HandShape.OPEN_PALM, shape)
    }

    @Test
    fun `all four fingers curled is FIST`() {
        val shape = HandPose.classify(hand(indexUp = false, middleUp = false, ringUp = false, pinkyUp = false))
        assertEquals(HandShape.FIST, shape)
    }

    @Test
    fun `a single index finger is not a command pose`() {
        val shape = HandPose.classify(hand(indexUp = true, middleUp = false, ringUp = false, pinkyUp = false))
        assertEquals(HandShape.OTHER, shape)
    }

    @Test
    fun `incomplete landmark list is not a command pose`() {
        assertEquals(HandShape.OTHER, HandPose.classify(listOf(Landmark(0.5f, 0.5f))))
    }

    @Test
    fun `two finger anchor sits between the index and middle tips`() {
        val points = hand(indexUp = true, middleUp = true, ringUp = false, pinkyUp = false)
        val anchor = HandPose.twoFingerAnchor(points)
        assertEquals(0.435f, anchor.x, 0.001f)
        assertEquals(0.3f, anchor.y, 0.001f)
    }
}
