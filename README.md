# Gesture Phone Control

App de Android que controla el teléfono con gestos de mano detectados por la
cámara frontal — pensado para cuando tienes las manos ocupadas (ej. comiendo)
y quieres bajar notificaciones, hacer scroll, ir atrás o cambiar de app sin
tocar la pantalla.

## Cómo funciona

Los gestos deben funcionar **dentro de cualquier app**, no solo dentro de
esta. Eso solo es posible en Android usando un
[`AccessibilityService`](https://developer.android.com/guide/topics/ui/accessibility/service),
que es el único tipo de componente con permiso para inyectar acciones/gestos
sobre el sistema completo. Por eso la arquitectura tiene dos servicios
separados que se comunican en el mismo proceso:

```
Cámara frontal
     │
     ▼
HandGesturePipeline                (CameraX + MediaPipe HandLandmarker)
 - Captura frames de la cámara frontal
 - Detecta los 21 landmarks de la mano por frame
 - Calcula el centroide de la palma y su movimiento
     │  usado por
     ├── GestureForegroundService (sin preview) → GestureEventBus (in-process, SharedFlow)
     │                                                    │
     │                                                    ▼
     │                                      GestureAccessibilityService (AccessibilityService)
     │                                       - performGlobalAction(...) → notificaciones, atrás, recientes
     │                                       - dispatchGesture(...)     → swipe sintético real (scroll)
     │                                       - vibra al ejecutar cada acción
     │
     └── GestureTestScreen (con preview en vivo) → solo muestra estado, no dispara acciones
```

- **`GestureClassifier`** (`gesture/GestureClassifier.kt`) es lógica pura sin
  dependencias de Android: recibe la posición del centroide de la palma por
  frame y decide si hubo un swipe rápido (arriba/abajo/izquierda/derecha),
  con un umbral de velocidad/desplazamiento y un cooldown para no disparar
  dos veces el mismo gesto. Tiene tests unitarios en
  `app/src/test/.../GestureClassifierTest.kt`.
- **`HandGesturePipeline`** (`gesture/HandGesturePipeline.kt`) encapsula la
  cámara + MediaPipe; la reutilizan tanto el servicio en segundo plano como
  la pantalla de calibración en pantalla (`GestureTestScreen`).

### Mapeo de gestos por defecto

| Gesto (mano) | Acción                                   |
|---|---|
| Deslizar **abajo** | Baja el panel de notificaciones |
| Deslizar **arriba** | Swipe sintético hacia arriba (scroll/deslizar dentro de la app activa) |
| Deslizar a un **lado (izquierda)** | Atrás |
| Deslizar a un **lado (derecha)** | Apps recientes / cambiar de vista |

Este mapeo es una decisión de diseño, no algo fijo — se cambia en
`GestureAccessibilityService.handle()` (un `when` sobre `GestureDirection`).
La sensibilidad (distancia mínima, velocidad mínima, cooldown) se ajusta en
el constructor de `GestureClassifier`.

## Limitaciones importantes (léelo antes de instalar)

- **No se puede probar ni compilar en este entorno remoto.** Este proyecto se
  construyó 100% en línea (código + modelo de MediaPipe ya incluido en
  `app/src/main/assets/hand_landmarker.task`), pero no hay un teléfono ni
  emulador con cámara aquí para verificar que el reconocimiento de gestos
  funcione bien en la práctica. Eso solo lo puedes probar tú, en tu equipo.
- **El servicio de Accesibilidad se activa manualmente.** Por seguridad,
  Android exige que el usuario active el servicio a mano desde
  Ajustes > Accesibilidad — ninguna app (ni yo) puede activarlo de forma
  remota o automática. La app tiene un botón que te lleva directo a esa
  pantalla.
- **La cámara corre en segundo plano mientras el servicio esté activo**,
  pero nunca se sube ni se guarda video: los frames se procesan localmente
  en el teléfono para calcular la posición de la mano y se descartan.
- El mapeo de gestos es un punto de partida razonable, no el único posible;
  espera afinarlo (falsos positivos, sensibilidad) probando en tu teléfono.

## Construir y probar en tu equipo

1. Clona este repo y ábrelo en **Android Studio (Koala o más reciente)**.
   Android Studio detectará el proyecto Gradle y generará el wrapper
   automáticamente en el primer sync (este repo no incluye el binario
   `gradle-wrapper.jar`).
2. Conecta un teléfono Android físico (con cámara frontal) por USB con
   depuración habilitada, o usa un emulador con soporte de cámara virtual.
3. Ejecuta la app (▶). Se instalará y abrirá la pantalla principal.
4. Antes de activar nada del sistema, prueba el botón **"Modo de prueba (ver
   cámara en vivo)"** — solo necesita el permiso de cámara. Muestra la
   cámara frontal en vivo con "Mano detectada: sí/no" y el último gesto
   reconocido, para calibrar sin riesgo de disparar acciones reales.
5. Cuando estés conforme, sigue los otros pasos: activar el servicio de
   accesibilidad en Ajustes, y presionar "Iniciar detección".
6. Prueba los gestos frente a la cámara frontal, en cualquier app abierta.
   Cada gesto reconocido vibra brevemente como confirmación.

Para correr los tests unitarios de la lógica de clasificación de gestos:

```
./gradlew testDebugUnitTest
```

## Estructura del proyecto

```
app/src/main/java/com/gesturephonecontrol/app/
├── MainActivity.kt                       # UI (Compose): permisos, estado, iniciar/detener
├── GestureTestScreen.kt                  # pantalla de calibración (cámara en vivo)
├── gesture/
│   ├── GestureClassifier.kt              # lógica pura: landmarks -> dirección de swipe
│   ├── GestureClassifierTest.kt          # (en app/src/test/...)
│   ├── GestureEventBus.kt                # bus in-process entre los dos servicios
│   ├── HandGesturePipeline.kt            # CameraX + MediaPipe HandLandmarker (compartido)
│   └── GestureForegroundService.kt       # foreground service sin preview
└── accessibility/
    └── GestureAccessibilityService.kt    # ejecuta las acciones del sistema + vibración
```

## Próximos pasos sugeridos

- Afinar sensibilidad y mapeo de gestos probando en tu teléfono real (usa el
  "Modo de prueba" para ver en vivo qué está detectando la cámara).
- Agregar más gestos (ej. puño cerrado, pellizco) usando los landmarks que
  ya provee MediaPipe.
- Pantalla de configuración para remapear gestos sin tocar código.
