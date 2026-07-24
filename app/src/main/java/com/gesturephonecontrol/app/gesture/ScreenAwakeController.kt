package com.gesturephonecontrol.app.gesture

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Keeps the screen from sleeping while gesture detection is running.
 *
 * A phone that dims out mid-use defeats the point of hands-free control, but a service has no
 * window of its own to set `FLAG_KEEP_SCREEN_ON` on. So this attaches a 1x1 fully transparent,
 * non-interactive overlay window purely to carry that flag — the modern replacement for the
 * deprecated screen wake locks, which are unreliable on recent Android versions.
 *
 * Requires the user to grant "Display over other apps". If they haven't, [acquire] does nothing
 * and detection still works — the screen just sleeps on its usual timeout.
 */
class ScreenAwakeController(private val context: Context) {

    private var overlay: View? = null

    fun canKeepScreenOn(): Boolean = Settings.canDrawOverlays(context)

    fun acquire() {
        if (overlay != null || !canKeepScreenOn()) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            1,
            1,
            type,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val view = View(context)
        runCatching { windowManager().addView(view, params) }
            .onSuccess { overlay = view }
    }

    fun release() {
        val view = overlay ?: return
        overlay = null
        runCatching { windowManager().removeView(view) }
    }

    private fun windowManager() =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
}
