package com.gesturephonecontrol.app.gesture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.gesturephonecontrol.app.R

/**
 * Foreground service that keeps [HandGesturePipeline] running with no preview — it only extracts
 * the palm centroid per frame and forwards swipe events to [GestureEventBus].
 *
 * Runs as a `camera`-typed foreground service because Android forbids camera access from a
 * background process on API 28+ otherwise (see README for why this can't be a plain Service).
 */
class GestureForegroundService : LifecycleService() {

    private lateinit var pipeline: HandGesturePipeline
    private lateinit var screenAwake: ScreenAwakeController

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        screenAwake = ScreenAwakeController(this)
        screenAwake.acquire()
        pipeline = HandGesturePipeline(
            context = applicationContext,
            onGesture = { direction -> GestureEventBus.emit(direction) }
        )
        pipeline.start(lifecycleOwner = this)
    }

    override fun onDestroy() {
        pipeline.stop()
        screenAwake.release()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "gesture_detection"
        private const val NOTIFICATION_ID = 1
    }
}
