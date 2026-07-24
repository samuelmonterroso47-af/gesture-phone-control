package com.gesturephonecontrol.app.gesture

import android.content.Context
import android.os.PowerManager

/**
 * Keeps the screen from sleeping while gesture detection is running. A phone that dims out
 * mid-session defeats the point of hands-free control.
 *
 * Uses a classic screen wake lock rather than a WindowManager overlay: the overlay approach needs
 * the user to grant "Display over other apps" as a separate manual step, which is easy to miss —
 * and silently leaves the screen sleeping with no obvious cause when skipped. A wake lock only
 * needs the WAKE_LOCK permission, which is normal and granted automatically at install, so there
 * is no extra step to forget. The API is deprecated but fully functional; it's the standard
 * pattern e-reader, video, and flashlight apps use for exactly this.
 */
class ScreenAwakeController(context: Context) {

    private val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire() {
        if (wakeLock?.isHeld == true) return
        @Suppress("DEPRECATION")
        val lock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "$WAKE_LOCK_TAG:screenOn"
        )
        lock.setReferenceCounted(false)
        lock.acquire()
        wakeLock = lock
    }

    fun release() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private companion object {
        const val WAKE_LOCK_TAG = "GesturePhoneControl"
    }
}
