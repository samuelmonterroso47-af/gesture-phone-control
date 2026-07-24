package com.gesturephonecontrol.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureClassifierTest {

    @Test
    fun `fast downward swipe emits DOWN`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onPalmCentroid(HandPoint(0.5f, 0.2f, 0)))
        assertEquals(GestureDirection.DOWN, classifier.onPalmCentroid(HandPoint(0.5f, 0.6f, 200)))
    }

    @Test
    fun `fast leftward swipe emits LEFT`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onPalmCentroid(HandPoint(0.8f, 0.5f, 0)))
        assertEquals(GestureDirection.LEFT, classifier.onPalmCentroid(HandPoint(0.2f, 0.5f, 200)))
    }

    @Test
    fun `slow drift does not emit a gesture`() {
        val classifier = GestureClassifier()
        assertNull(classifier.onPalmCentroid(HandPoint(0.5f, 0.5f, 0)))
        assertNull(classifier.onPalmCentroid(HandPoint(0.52f, 0.5f, 2000)))
    }

    @Test
    fun `cooldown blocks a second gesture right after the first`() {
        val classifier = GestureClassifier()
        classifier.onPalmCentroid(HandPoint(0.5f, 0.2f, 0))
        assertEquals(GestureDirection.DOWN, classifier.onPalmCentroid(HandPoint(0.5f, 0.6f, 200)))

        classifier.onPalmCentroid(HandPoint(0.5f, 0.6f, 250))
        assertNull(classifier.onPalmCentroid(HandPoint(0.5f, 0.2f, 400)))
    }

    @Test
    fun `losing the hand clears history`() {
        val classifier = GestureClassifier()
        classifier.onPalmCentroid(HandPoint(0.5f, 0.2f, 0))
        classifier.onHandLost()
        assertNull(classifier.onPalmCentroid(HandPoint(0.5f, 0.6f, 200)))
    }
}
