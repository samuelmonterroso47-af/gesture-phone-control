package com.gesturephonecontrol.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.gesturephonecontrol.app.gesture.GestureDirection
import com.gesturephonecontrol.app.gesture.GestureEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Consumes hand-gesture events from [GestureEventBus] (produced by the camera pipeline in
 * [com.gesturephonecontrol.app.gesture.GestureForegroundService]) and turns them into real system
 * actions. This is the only component with permission to do so — Android restricts
 * [performGlobalAction] and [dispatchGesture] to services the user explicitly enabled under
 * Settings > Accessibility, which is why these two effects live behind a manual opt-in.
 *
 * Default mapping (see README to change it):
 *  - swipe DOWN  -> pull down the notification shade
 *  - swipe UP    -> synthetic on-screen swipe up (acts like a real scroll/swipe in any app)
 *  - swipe LEFT  -> back
 *  - swipe RIGHT -> recent apps (switch view)
 */
class GestureAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            GestureEventBus.events.collect { direction -> handle(direction) }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handle(direction: GestureDirection) {
        when (direction) {
            GestureDirection.DOWN -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            GestureDirection.UP -> dispatchSwipe(fromBottom = true)
            GestureDirection.LEFT -> performGlobalAction(GLOBAL_ACTION_BACK)
            GestureDirection.RIGHT -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }

    /** Injects a real synthetic touch swipe on the screen, so it works like an actual finger swipe in any app. */
    private fun dispatchSwipe(fromBottom: Boolean) {
        val metrics = resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = if (fromBottom) metrics.heightPixels * 0.8f else metrics.heightPixels * 0.2f
        val endY = if (fromBottom) metrics.heightPixels * 0.2f else metrics.heightPixels * 0.8f

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, SWIPE_DURATION_MS))
            .build()
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    private companion object {
        const val SWIPE_DURATION_MS = 150L
    }
}
