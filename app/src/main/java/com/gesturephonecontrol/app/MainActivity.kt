package com.gesturephonecontrol.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gesturephonecontrol.app.accessibility.GestureAccessibilityService
import com.gesturephonecontrol.app.gesture.GestureForegroundService

class MainActivity : ComponentActivity() {

    private var hasCameraPermission by mutableStateOf(false)
    private var isAccessibilityEnabled by mutableStateOf(false)

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GestureControlScreen(
                        hasCameraPermission = hasCameraPermission,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onRequestCameraPermission = { requestCameraPermission.launch(Manifest.permission.CAMERA) },
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onStartDetection = {
                            ContextCompat.startForegroundService(
                                this,
                                Intent(this, GestureForegroundService::class.java)
                            )
                        },
                        onStopDetection = {
                            stopService(Intent(this, GestureForegroundService::class.java))
                        }
                    )
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
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, GestureAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}

@Composable
private fun GestureControlScreen(
    hasCameraPermission: Boolean,
    isAccessibilityEnabled: Boolean,
    onRequestCameraPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onStartDetection: () -> Unit,
    onStopDetection: () -> Unit
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

        Text("3. Detección de gestos con la cámara frontal")
        Button(onClick = onStartDetection, enabled = hasCameraPermission && isAccessibilityEnabled) {
            Text("Iniciar detección")
        }
        Button(onClick = onStopDetection) { Text("Detener detección") }

        Text(
            "Gestos: deslizar mano abajo = bajar notificaciones · " +
                "arriba = deslizar/scroll · a un lado = atrás/cambiar app."
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
            onRequestCameraPermission = {},
            onOpenAccessibilitySettings = {},
            onStartDetection = {},
            onStopDetection = {}
        )
    }
}
