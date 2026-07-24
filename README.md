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
 - Detecta los 21 landmarks de la mano por frame
 - HandPose      -> qué pose sostiene la mano (a dónde apunta el índice, "V", puño)
 - PoseHoldDetector -> ¿la sostuvo el tiempo suficiente para contar?
     │  usado por
     ├── GestureForegroundService (sin preview) → GestureEventBus (in-process, SharedFlow)
     │                                                    │
     │                                                    ▼
     │                                      GestureAccessibilityService (AccessibilityService)
     │                                       - performGlobalAction(...) → notificaciones, atrás, recientes, inicio
     │                                       - dispatchGesture(...)     → scroll sintético real
     │                                       - vibra al ejecutar cada acción
     │
     └── GestureTrainingScreen (tutorial, con preview en vivo) → solo muestra estado, no dispara acciones
```

- **`HandPose`** (`gesture/HandPose.kt`) es lógica pura sin dependencias de
  Android: convierte los 21 landmarks en una pose. Para apuntar, lo que mide
  es la **orientación del dedo** (el vector del nudillo a la punta), no dónde
  está la mano en el encuadre: al rotar el índice de apuntar arriba a apuntar
  al lado, la mano casi no se desplaza, así que rastrear su posición no
  detectaría nada.
- **`PoseHoldDetector`** (`gesture/PoseHoldDetector.kt`), también lógica pura,
  exige sostener la pose ~450 ms antes de contarla, para que una pose por la
  que solo estás pasando no dispare. Mientras la sostienes, el scroll se
  repite cada 700 ms; los demás comandos disparan una sola vez por pose.

### Vocabulario de gestos

**Las poses se sostienen, no se agitan.** No hay manotazos: pones la mano en
la pose y la mantienes quieta frente a la cámara. Eso cansa mucho menos y
funciona con la mano apoyada, que es el caso real (estás comiendo y quieres
cambiar de video).

| Pose | Acción | ¿Se repite al sostener? |
|---|---|---|
| **Índice** apuntando **arriba** | Deslizar arriba (scroll) | Sí |
| **Índice** apuntando **abajo** | Deslizar abajo (scroll) | Sí |
| **Índice** apuntando a la **izquierda** | Deslizar a la izquierda | Sí |
| **Índice** apuntando a la **derecha** | Deslizar a la derecha | Sí |
| **Índice + medio** en "V" | Bajar notificaciones | No |
| **Mano abierta** (4 dedos) | Atrás | No |
| **Puño** cerrado | Ir al inicio | No |

Apuntar dispara un swipe sintético real en esa dirección —igual que un dedo
real— así que sirve tanto para hacer scroll vertical como para moverte entre
pestañas/módulos horizontales (como los tabs de TikTok o deslizar entre
Reels de Instagram). Las tres poses de forma cubren las acciones de sistema
que no tienen una dirección natural. Solo el deslizamiento se repite
mientras sostienes: así sostener el puño no aporrea el botón de inicio una y
otra vez.

### Tutorial de entrenamiento

La primera vez que abres la app (con permiso de cámara concedido) entra
automáticamente al tutorial guiado: enseña una pose a la vez, con un diagrama
de la pose exacta, la cámara en vivo al lado, y un semáforo de estado ("no veo
tu mano" / "veo tu mano pero no la pose" / "¡pose correcta, sostenla!"). Son 6
pasos en total. Durante el tutorial **no se ejecuta ninguna acción real** del
sistema, así que puedes practicar sin miedo. Se puede repetir cuando quieras
con el botón "Practicar gestos (tutorial)" en la pantalla principal.

### Cómo cambiar el mapeo

Todo el vocabulario vive en una sola tabla, `GESTURE_COMMANDS` en
`gesture/GestureCommand.kt`: agregar o remapear un comando es editar esa
tabla (y agregar su lección en `training/GestureLesson.kt`, que lee de la
misma tabla para no desincronizarse). `GestureAccessibilityService` solo sabe
*ejecutar* cada comando, no cuál pose lo produce. Qué tan rápido responde
—cuánto hay que sostener y cada cuánto se repite— se ajusta en el constructor
de `PoseHoldDetector`.

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
- **Android 13+ bloquea permisos sensibles en APKs instalados fuera de Play
  Store ("restricted settings").** Como este APK lo instalas manualmente
  (no viene de Play Store), es probable que el interruptor de Accesibilidad
  aparezca gris/deshabilitado la primera vez. Para habilitarlo: Ajustes >
  Apps > Gesture Phone Control > menú de 3 puntos (arriba a la derecha) >
  **"Permitir configuración restringida"**, y luego vuelve a Accesibilidad
  a activarlo. Es un paso único por instalación.
- **La pantalla se mantiene encendida mientras la detección esté activa**,
  porque un teléfono que se apaga a media sesión hace inútil el control sin
  manos. Usa un wake lock de pantalla clásico (`PowerManager.SCREEN_BRIGHT_WAKE_LOCK`)
  con el permiso `WAKE_LOCK`, que es normal y se concede solo al instalar — no
  hay ningún paso manual que puedas olvidar. Recuerda **detener la detección**
  cuando no la uses, o la pantalla encendida te consumirá batería.
- **La cámara corre en segundo plano mientras el servicio esté activo**,
  pero nunca se sube ni se guarda video: los frames se procesan localmente
  en el teléfono para calcular la posición de la mano y se descartan.
- El mapeo de gestos es un punto de partida razonable, no el único posible;
  espera afinarlo (falsos positivos, sensibilidad) probando en tu teléfono.

## Compilación automática (CI) y flujo de iteración

Cada push a `main` dispara `.github/workflows/build-debug-apk.yml`, que
compila `app-debug.apk` en un runner de GitHub (con internet completo, sin
las restricciones del entorno donde se escribió este código) y lo sube como
artifact. Para bajarlo: pestaña **Actions** del repo → la corrida más
reciente → sección **Artifacts** al final → descargar y descomprimir.

El debug keystore (`app/debug.keystore`) está fijo y committeado a propósito
(solo para builds de debug, nunca hagas esto con una llave de release): así
todas las compilaciones —la tuya en Android Studio y las de CI— comparten la
misma firma, y puedes instalar una versión nueva **encima** de la anterior
sin desinstalar primero.

## Construir y probar en tu equipo

1. Clona este repo y ábrelo en **Android Studio (Koala o más reciente)**.
   Android Studio detectará el proyecto Gradle y generará el wrapper
   automáticamente en el primer sync (este repo no incluye el binario
   `gradle-wrapper.jar`).
2. Conecta un teléfono Android físico (con cámara frontal) por USB con
   depuración habilitada, o usa un emulador con soporte de cámara virtual.
3. Ejecuta la app (▶). Se instalará y abrirá la pantalla principal.
4. La primera vez entra solo al **tutorial guiado** (solo necesita permiso de
   cámara). Practica ahí las 6 poses hasta que te salgan naturales; no
   dispara ninguna acción real del sistema.
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
├── gesture/
│   ├── HandPose.kt                       # lógica pura: landmarks -> pose (a dónde apunta)
│   ├── PoseHoldDetector.kt               # lógica pura: ¿sostuvo la pose lo suficiente?
│   ├── GestureCommand.kt                 # tabla única: qué pose hace qué
│   ├── GestureEventBus.kt                # bus in-process entre los dos servicios
│   ├── HandGesturePipeline.kt            # CameraX + MediaPipe HandLandmarker (compartido)
│   ├── ScreenAwakeController.kt          # evita que la pantalla se apague durante el uso
│   └── GestureForegroundService.kt       # foreground service sin preview
├── training/
│   ├── GestureLesson.kt                  # contenido del tutorial (pasos e instrucciones)
│   ├── HandPoseDiagram.kt                # diagrama animado de la seña y el movimiento
│   └── GestureTrainingScreen.kt          # pantalla de práctica guiada
└── accessibility/
    └── GestureAccessibilityService.kt    # ejecuta las acciones del sistema + vibración

app/src/test/.../gesture/                 # tests unitarios de GestureClassifier y HandPose
```

## Próximos pasos sugeridos

- Afinar sensibilidad y mapeo de gestos probando en tu teléfono real (usa el
  tutorial para ver en vivo qué está detectando la cámara).
- Agregar más gestos (ej. puño cerrado, pellizco) usando los landmarks que
  ya provee MediaPipe.
- Pantalla de configuración para remapear gestos sin tocar código.
