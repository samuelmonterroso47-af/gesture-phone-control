package com.gesturephonecontrol.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureClassifierTest {

    private fun point(x: Float, y: Float, t: Long, shape: HandShape) = HandPoint(x, y, t, shape)

    @Test
    fun `open palm swiping down emits an open palm DOWN event`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.2f, 0, HandShape.OPEN_PALM)))
        assertEquals(
            GestureEvent(HandShape.OPEN_PALM, GestureDirection.DOWN),
            classifier.onHandPoint(point(0.5f, 0.6f, 200, HandShape.OPEN_PALM))
        )
    }

    @Test
    fun `two fingers swiping up emits a two-finger UP event`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.7f, 0, HandShape.TWO_FINGERS)))
        assertEquals(
            GestureEvent(HandShape.TWO_FINGERS, GestureDirection.UP),
            classifier.onHandPoint(point(0.5f, 0.3f, 200, HandShape.TWO_FINGERS))
        )
    }

    @Test
    fun `the same direction with a different pose is a different event`() {
        val classifier = GestureClassifier()
        classifier.onHandPoint(point(0.5f, 0.7f, 0, HandShape.FIST))
        assertEquals(
            GestureEvent(HandShape.FIST, GestureDirection.UP),
            classifier.onHandPoint(point(0.5f, 0.3f, 200, HandShape.FIST))
        )
    }

    @Test
    fun `an unrecognized pose never emits`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.7f, 0, HandShape.OTHER)))
        assertNull(classifier.onHandPoint(point(0.5f, 0.3f, 200, HandShape.OTHER)))
    }

    @Test
    fun `a swipe that changes pose midway does not emit`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.7f, 0, HandShape.TWO_FINGERS)))
        assertNull(classifier.onHandPoint(point(0.5f, 0.5f, 100, HandShape.OPEN_PALM)))
        assertNull(classifier.onHandPoint(point(0.5f, 0.3f, 200, HandShape.TWO_FINGERS)))
    }

    @Test
    fun `slow drift does not emit a gesture`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.5f, 0, HandShape.OPEN_PALM)))
        assertNull(classifier.onHandPoint(point(0.52f, 0.5f, 2000, HandShape.OPEN_PALM)))
    }

    @Test
    fun `cooldown blocks a second gesture right after the first`() {
        val classifier = GestureClassifier()
        classifier.onHandPoint(point(0.5f, 0.2f, 0, HandShape.OPEN_PALM))
        assertEquals(
            GestureEvent(HandShape.OPEN_PALM, GestureDirection.DOWN),
            classifier.onHandPoint(point(0.5f, 0.6f, 200, HandShape.OPEN_PALM))
        )

        classifier.onHandPoint(point(0.5f, 0.6f, 250, HandShape.OPEN_PALM))
        assertNull(classifier.onHandPoint(point(0.5f, 0.2f, 400, HandShape.OPEN_PALM)))
    }

    @Test
    fun `losing the hand clears history`() {
        val classifier = GestureClassifier()
        classifier.onHandPoint(point(0.5f, 0.2f, 0, HandShape.OPEN_PALM))
        classifier.onHandLost()
        assertNull(classifier.onHandPoint(point(0.5f, 0.6f, 200, HandShape.OPEN_PALM)))
    }
}
