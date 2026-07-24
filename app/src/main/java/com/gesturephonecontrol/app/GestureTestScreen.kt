package com.gesturephonecontrol.app

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gesturephonecontrol.app.gesture.HandGesturePipeline

/**
 * Live calibration screen: shows the raw front-camera feed with a status overlay (hand detected?
 * last gesture recognized?), so the gesture mapping/sensitivity can be checked before turning on
 * the accessibility service. It runs [HandGesturePipeline] directly, tied to this screen's
 * lifecycle — it does NOT touch [com.gesturephonecontrol.app.gesture.GestureEventBus], so it
 * never triggers real system actions.
 */
@Composable
fun GestureTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var handDetected by remember { mutableStateOf(false) }
    var lastGesture by remember { mutableStateOf("-") }

    DisposableEffect(Unit) {
        val pipeline = HandGesturePipeline(
            context = context.applicationContext,
            onHandLandmarks = { landmarks -> handDetected = landmarks != null },
            onGesture = { direction -> lastGesture = direction.name }
        )
        pipeline.start(lifecycleOwner, previewView.surfaceProvider)
        onDispose { pipeline.stop() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp)
        ) {
            Text(
                text = if (handDetected) "Mano detectada" else "Sin mano detectada",
                color = if (handDetected) Color.Green else Color.White
            )
            Text(text = "Último gesto: $lastGesture", color = Color.White)
            Button(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                Text("Volver")
            }
        }
    }
}
