package com.gesturephonecontrol.app.gesture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.gesturephonecontrol.app.R
import java.io.ByteArrayOutputStream

/**
 * Foreground service that owns the camera + hand-tracking pipeline. It never shows a preview —
 * it only extracts the palm centroid per frame and forwards swipe events to [GestureEventBus].
 *
 * Runs as a `camera`-typed foreground service because Android forbids camera access from a
 * background process on API 28+ otherwise (see README for why this can't be a plain Service).
 */
class GestureForegroundService : LifecycleService() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var handLandmarker: HandLandmarker? = null
    private val classifier = GestureClassifier()

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        handLandmarker = createHandLandmarker()
        startCamera()
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        handLandmarker?.close()
        super.onDestroy()
    }

    private fun createHandLandmarker(): HandLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET_PATH)
            .build()
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener(::onHandResult)
            .setErrorListener { /* transient MediaPipe errors are safe to drop a frame over */ }
            .build()
        return HandLandmarker.createFromOptions(applicationContext, options)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                analyzeFrame(imageProxy)
            }

            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toUprightBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()
        val timestampMs = imageProxy.imageInfo.timestamp / 1_000_000
        handLandmarker?.detectAsync(mpImage, timestampMs)
        imageProxy.close()
    }

    private fun onHandResult(result: HandLandmarkerResult, input: MPImage) {
        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) {
            classifier.onHandLost()
            return
        }
        val hand = landmarks[0]
        val palmIndices = intArrayOf(0, 5, 9, 13, 17) // wrist + the four MCP knuckles
        var sumX = 0f
        var sumY = 0f
        for (i in palmIndices) {
            sumX += hand[i].x()
            sumY += hand[i].y()
        }
        val centroidX = sumX / palmIndices.size
        val centroidY = sumY / palmIndices.size
        // Raw front-camera frames aren't mirrored, so flip X: a hand moving to the user's
        // physical right should read as "right", matching how a selfie view looks.
        val mirroredX = 1f - centroidX
        val direction = classifier.onPalmCentroid(HandPoint(mirroredX, centroidY, System.currentTimeMillis()))
        if (direction != null) {
            GestureEventBus.emit(direction)
        }
    }

    /** Converts a YUV_420_888 [ImageProxy] to an upright RGB [Bitmap], correcting sensor rotation. */
    private fun ImageProxy.toUprightBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImageCompat(nv21, width, height)
        val jpegBytes = yuvImage.toJpegBytes()
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        val rotationDegrees = imageInfo.rotationDegrees
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private class YuvImageCompat(nv21: ByteArray, width: Int, height: Int) {
        private val yuvImage = android.graphics.YuvImage(nv21, ImageFormat.NV21, width, height, null)
        private val rect = Rect(0, 0, width, height)

        fun toJpegBytes(): ByteArray {
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(rect, 90, out)
            return out.toByteArray()
        }
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
        private const val MODEL_ASSET_PATH = "hand_landmarker.task"
    }
}
