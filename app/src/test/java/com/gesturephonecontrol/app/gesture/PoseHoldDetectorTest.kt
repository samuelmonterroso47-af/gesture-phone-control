package com.gesturephonecontrol.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoseHoldDetectorTest {

    private val pointUp = HandPoseState.Pointing(PointDirection.UP)
    private val fist = HandPoseState.Fist

    @Test
    fun `a pose held past the dwell time fires once`() {
        val detector = PoseHoldDetector(dwellMs = 450, repeatMs = 700)
        assertNull(detector.onPose(pointUp, 0))
        assertNull(detector.onPose(pointUp, 300))
        assertEquals(
            PoseHoldDetector.Fire(pointUp, isRepeat = false),
            detector.onPose(pointUp, 500)
        )
    }

    @Test
    fun `a pose released before the dwell time never fires`() {
        val detector = PoseHoldDetector(dwellMs = 450, repeatMs = 700)
        assertNull(detector.onPose(pointUp, 0))
        assertNull(detector.onPose(pointUp, 200))
        assertNull(detector.onPose(HandPoseState.None, 300))
        assertNull(detector.onPose(pointUp, 400))
    }

    @Test
    fun `holding keeps firing at the repeat interval`() {
        val detector = PoseHoldDetector(dwellMs = 450, repeatMs = 700)
        detector.onPose(pointUp, 0)
        assertEquals(PoseHoldDetector.Fire(pointUp, false), detector.onPose(pointUp, 500))
        assertNull(detector.onPose(pointUp, 900))
        assertEquals(PoseHoldDetector.Fire(pointUp, true), detector.onPose(pointUp, 1200))
    }

    @Test
    fun `switching pose restarts the dwell`() {
        val detector = PoseHoldDetector(dwellMs = 450, repeatMs = 700)
        detector.onPose(pointUp, 0)
        detector.onPose(pointUp, 500)

        assertNull(detector.onPose(fist, 600))
        assertNull(detector.onPose(fist, 900))
        assertEquals(PoseHoldDetector.Fire(fist, false), detector.onPose(fist, 1100))
    }

    @Test
    fun `dropping the hand stops the repeat`() {
        val detector = PoseHoldDetector(dwellMs = 450, repeatMs = 700)
        detector.onPose(pointUp, 0)
        detector.onPose(pointUp, 500)
        detector.onPose(HandPoseState.None, 600)

        assertNull(detector.onPose(pointUp, 700))
    }
}
