package com.gesturephonecontrol.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureClassifierTest {

    private fun point(x: Float, y: Float, t: Long, shape: HandShape = HandShape.OTHER) =
        HandPoint(x, y, t, shape)

    @Test
    fun `fast downward swipe emits DOWN`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.2f, 0)))
        assertEquals(GestureDirection.DOWN, classifier.onHandPoint(point(0.5f, 0.6f, 200)))
    }

    @Test
    fun `fast leftward swipe emits LEFT`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.8f, 0.5f, 0)))
        assertEquals(GestureDirection.LEFT, classifier.onHandPoint(point(0.2f, 0.5f, 200)))
    }

    @Test
    fun `upward swipe with two-finger pose emits UP`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.7f, 0, HandShape.TWO_FINGERS)))
        assertEquals(
            GestureDirection.UP,
            classifier.onHandPoint(point(0.5f, 0.3f, 200, HandShape.TWO_FINGERS))
        )
    }

    @Test
    fun `upward swipe with an open hand does not emit`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.7f, 0, HandShape.OTHER)))
        assertNull(classifier.onHandPoint(point(0.5f, 0.3f, 200, HandShape.OTHER)))
    }

    @Test
    fun `upward swipe that breaks the pose midway does not emit`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.7f, 0, HandShape.TWO_FINGERS)))
        assertNull(classifier.onHandPoint(point(0.5f, 0.5f, 100, HandShape.OTHER)))
        assertNull(classifier.onHandPoint(point(0.5f, 0.3f, 200, HandShape.TWO_FINGERS)))
    }

    @Test
    fun `slow drift does not emit a gesture`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onHandPoint(point(0.5f, 0.5f, 0)))
        assertNull(classifier.onHandPoint(point(0.52f, 0.5f, 2000)))
    }

    @Test
    fun `cooldown blocks a second gesture right after the first`() {
        val classifier = GestureClassifier()
        classifier.onHandPoint(point(0.5f, 0.2f, 0))
        assertEquals(GestureDirection.DOWN, classifier.onHandPoint(point(0.5f, 0.6f, 200)))

        classifier.onHandPoint(point(0.5f, 0.6f, 250))
        assertNull(classifier.onHandPoint(point(0.5f, 0.2f, 400)))
    }

    @Test
    fun `losing the hand clears history`() {
        val classifier = GestureClassifier()
        classifier.onHandPoint(point(0.5f, 0.2f, 0))
        classifier.onHandLost()
        assertNull(classifier.onHandPoint(point(0.5f, 0.6f, 200)))
    }
}
