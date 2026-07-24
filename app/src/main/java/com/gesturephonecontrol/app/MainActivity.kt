package com.gesturephonecontrol.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gesturephonecontrol.app.accessibility.GestureAccessibilityService
import com.gesturephonecontrol.app.gesture.GestureForegroundService
import com.gesturephonecontrol.app.training.GestureTrainingScreen

class MainActivity : ComponentActivity() {

    private var hasCameraPermission by mutableStateOf(false)
    private var isAccessibilityEnabled by mutableStateOf(false)
    private var canKeepScreenOn by mutableStateOf(false)
    private var showTraining by mutableStateOf(false)

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted && !hasCompletedTraining()) showTraining = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        showTraining = hasCameraPermission && !hasCompletedTraining()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showTraining) {
                        GestureTrainingScreen(
                            onFinished = {
                                markTrainingCompleted()
                                showTraining = false
                            },
                            onExit = { showTraining = false }
                        )
                    } else {
                        GestureControlScreen(
                            hasCameraPermission = hasCameraPermission,
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            canKeepScreenOn = canKeepScreenOn,
                            onRequestCameraPermission = { requestCameraPermission.launch(Manifest.permission.CAMERA) },
                            onOpenAccessibilitySettings = {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onRequestScreenOnPermission = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                )
                            },
                            onStartDetection = {
                                ContextCompat.startForegroundService(
                                    this,
                                    Intent(this, GestureForegroundService::class.java)
                                )
                            },
                            onStopDetection = {
                                stopService(Intent(this, GestureForegroundService::class.java))
                            },
                            onOpenTraining = { showTraining = true }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Both permissions can only be granted outside the app (Settings), so re-check on resume.
        hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        isAccessibilityEnabled = isAccessibilityServiceEnabled()
        canKeepScreenOn = Settings.canDrawOverlays(this)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, GestureAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    private fun prefs() = getSharedPreferences("gesture_prefs", Context.MODE_PRIVATE)

    private fun hasCompletedTraining() = prefs().getBoolean(KEY_TRAINING_DONE, false)

    private fun markTrainingCompleted() {
        prefs().edit().putBoolean(KEY_TRAINING_DONE, true).apply()
    }

    private companion object {
        const val KEY_TRAINING_DONE = "training_completed"
    }
}

@Composable
private fun GestureControlScreen(
    hasCameraPermission: Boolean,
    isAccessibilityEnabled: Boolean,
    canKeepScreenOn: Boolean,
    onRequestCameraPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestScreenOnPermission: () -> Unit,
    onStartDetection: () -> Unit,
    onStopDetection: () -> Unit,
    onOpenTraining: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Gesture Phone Control", style = MaterialTheme.typography.headlineSmall)

        Text(
            "1. Permiso de cámara: " + if (hasCameraPermission) "concedido" else "pendiente"
        )
        if (!hasCameraPermission) {
            Button(onClick = onRequestCameraPermission) { Text("Conceder permiso de cámara") }
        }

        Text(
            "2. Servicio de accesibilidad: " + if (isAccessibilityEnabled) "activado" else "desactivado"
        )
        if (!isAccessibilityEnabled) {
            Button(onClick = onOpenAccessibilitySettings) { Text("Activar en Ajustes de Accesibilidad") }
        }

        Text(
            "3. Mantener pantalla encendida: " +
                if (canKeepScreenOn) "permitido" else "pendiente"
        )
        if (!canKeepScreenOn) {
            Button(onClick = onRequestScreenOnPermission) {
                Text("Permitir (evita que se apague la pantalla)")
            }
        }

        Text("4. Detección de gestos con la cámara frontal")
        Button(onClick = onStartDetection, enabled = hasCameraPermission && isAccessibilityEnabled) {
            Text("Iniciar detección")
        }
        Button(onClick = onStopDetection) { Text("Detener detección") }

        Button(onClick = onOpenTraining, enabled = hasCameraPermission) {
            Text("Practicar gestos (tutorial)")
        }

        Text(
            "Gestos: índice + medio juntos hacia arriba = scroll · mano abierta abajo = " +
                "notificaciones · a un lado = atrás / cambiar app."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GestureControlScreenPreview() {
    MaterialTheme {
        GestureControlScreen(
            hasCameraPermission = false,
            isAccessibilityEnabled = false,
            canKeepScreenOn = false,
            onRequestCameraPermission = {},
            onOpenAccessibilitySettings = {},
            onRequestScreenOnPermission = {},
            onStartDetection = {},
            onStopDetection = {},
            onOpenTraining = {}
        )
    }
}
