package com.gesturephonecontrol.app.training

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gesturephonecontrol.app.gesture.GESTURE_COMMANDS
import com.gesturephonecontrol.app.gesture.GestureCommand
import com.gesturephonecontrol.app.gesture.HandGesturePipeline
import com.gesturephonecontrol.app.gesture.HandPoseState
import com.gesturephonecontrol.app.gesture.PointDirection

/**
 * Guided tutorial: walks through each pose one at a time, showing the exact hand shape to hold,
 * with the live camera and a rep counter. Nothing here touches
 * [com.gesturephonecontrol.app.gesture.GestureEventBus], so practising never fires real system
 * actions — the user can drill safely before turning detection on.
 */
@Composable
fun GestureTrainingScreen(onFinished: () -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var lessonIndex by remember { mutableIntStateOf(0) }
    var reps by remember { mutableIntStateOf(0) }
    var handPose by remember { mutableStateOf<HandPoseState?>(null) }
    var lastWrongCommand by remember { mutableStateOf<GestureCommand?>(null) }

    val lesson = GESTURE_LESSONS[lessonIndex]

    DisposableEffect(Unit) {
        val pipeline = HandGesturePipeline(
            context = context.applicationContext,
            onHandState = { pose -> handPose = pose },
            onCommand = { command ->
                val current = GESTURE_LESSONS[lessonIndex]
                if (command == GESTURE_COMMANDS[current.pose]) {
                    lastWrongCommand = null
                    reps += 1
                    vibrate(context, 40)
                    if (reps >= current.repsToPass) {
                        if (lessonIndex < GESTURE_LESSONS.lastIndex) {
                            lessonIndex += 1
                            reps = 0
                        } else {
                            onFinished()
                        }
                    }
                } else {
                    lastWrongCommand = command
                }
            }
        )
        pipeline.start(lifecycleOwner, previewView.surfaceProvider)
        onDispose { pipeline.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Paso ${lessonIndex + 1} de ${GESTURE_LESSONS.size}",
            style = MaterialTheme.typography.labelLarge
        )
        Text(text = lesson.title, style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HandPoseDiagram(
                pose = lesson.pose,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            }
        }

        Text(text = lesson.instruction)
        Text(
            text = "Acción real: " + lesson.actionLabel,
            style = MaterialTheme.typography.bodySmall
        )

        HandStatusRow(handPose = handPose, requiredPose = lesson.pose)

        lastWrongCommand?.let {
            Text(
                text = "Detecté \"${it.label}\". Ajusta la pose e inténtalo de nuevo.",
                color = Color(0xFFD08A00),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Repeticiones: $reps de ${lesson.repsToPass}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LinearProgressIndicator(
            progress = { reps.toFloat() / lesson.repsToPass },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onExit) { Text("Salir") }
            if (lessonIndex < GESTURE_LESSONS.lastIndex) {
                Button(
                    onClick = {
                        lessonIndex += 1
                        reps = 0
                        lastWrongGesture = null
                    }
                ) { Text("Saltar este gesto") }
            } else {
                Button(onClick = onFinished) { Text("Terminar") }
            }
        }
    }
}

@Composable
private fun HandStatusRow(handPose: HandPoseState?, requiredPose: HandPoseState) {
    val (label, color) = when {
        handPose == null -> "No veo tu mano — acércala a la cámara frontal" to Color(0xFFB00020)
        handPose != requiredPose ->
            "Veo tu mano, pero aún no la pose: ${poseLabel(requiredPose)}" to Color(0xFFD08A00)
        else -> "¡Pose correcta! Sostenla" to Color(0xFF1B873F)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = label, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun poseLabel(pose: HandPoseState) = when (pose) {
    is HandPoseState.Pointing -> "índice apuntando " + when (pose.direction) {
        PointDirection.UP -> "arriba"
        PointDirection.DOWN -> "abajo"
        PointDirection.LEFT -> "a la izquierda"
        PointDirection.RIGHT -> "a la derecha"
    }
    HandPoseState.TwoFingers -> "índice + medio en \"V\""
    HandPoseState.Fist -> "puño cerrado"
    HandPoseState.None -> "ninguna reconocida"
}

private fun vibrate(context: android.content.Context, durationMs: Long) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(VibratorManager::class.java)).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
}
