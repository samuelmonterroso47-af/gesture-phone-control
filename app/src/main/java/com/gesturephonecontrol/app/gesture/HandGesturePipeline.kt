package com.gesturephonecontrol.app.gesture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.io.ByteArrayOutputStream

/**
 * Owns the CameraX + MediaPipe HandLandmarker pipeline: front camera in, palm-centroid swipe
 * detection out. Shared by [GestureForegroundService] (no preview, runs in the background) and
 * the in-app calibration screen (with a live preview), so the camera/model setup lives in one
 * place instead of being duplicated.
 *
 * [onHandLandmarks] fires on every frame with the detected hand (or null if none), useful for a
 * "is it seeing my hand" indicator. [onGesture] fires only when a swipe is actually recognized.
 */
class HandGesturePipeline(
    private val context: Context,
    private val onHandLandmarks: (List<NormalizedLandmark>?) -> Unit = {},
    private val onGesture: (GestureDirection) -> Unit
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var handLandmarker: HandLandmarker? = null
    private val classifier = GestureClassifier()

    /** Starts the camera bound to [lifecycleOwner]. Pass a [previewSurfaceProvider] to also show a live preview. */
    fun start(lifecycleOwner: LifecycleOwner, previewSurfaceProvider: Preview.SurfaceProvider? = null) {
        handLandmarker = createHandLandmarker()

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider

            val useCases = mutableListOf<UseCase>()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                analyzeFrame(imageProxy)
            }
            useCases += analysis

            if (previewSurfaceProvider != null) {
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewSurfaceProvider)
                useCases += preview
            }

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, *useCases.toTypedArray())
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        handLandmarker?.close()
        handLandmarker = null
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
        return HandLandmarker.createFromOptions(context.applicationContext, options)
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
            onHandLandmarks(null)
            return
        }
        val hand = landmarks[0]
        onHandLandmarks(hand)

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
            onGesture(direction)
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

    private companion object {
        const val MODEL_ASSET_PATH = "hand_landmarker.task"
    }
}
