package com.gesturephonecontrol.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import com.gesturephonecontrol.app.gesture.GestureCommand
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
 * Which gesture produces which command lives in
 * [com.gesturephonecontrol.app.gesture.GESTURE_COMMANDS]; this class only knows how to carry each
 * command out.
 */
class GestureAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            GestureEventBus.events.collect { command -> handle(command) }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handle(command: GestureCommand) {
        when (command) {
            GestureCommand.SCROLL_UP -> dispatchVerticalSwipe(scrollUp = true)
            GestureCommand.SCROLL_DOWN -> dispatchVerticalSwipe(scrollUp = false)
            GestureCommand.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            GestureCommand.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            GestureCommand.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            GestureCommand.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
        }
        vibrateConfirmation()
    }

    /** Short buzz so the user gets confirmation a gesture fired without having to look at the screen. */
    private fun vibrateConfirmation() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(CONFIRMATION_VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /**
     * Injects a real synthetic touch swipe, so scrolling works like an actual finger in any app.
     * Scrolling up means dragging the content upward, i.e. the finger travels bottom → top.
     */
    private fun dispatchVerticalSwipe(scrollUp: Boolean) {
        val metrics = resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY = if (scrollUp) metrics.heightPixels * 0.8f else metrics.heightPixels * 0.2f
        val endY = if (scrollUp) metrics.heightPixels * 0.2f else metrics.heightPixels * 0.8f

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
        const val CONFIRMATION_VIBRATION_MS = 40L
    }
}
