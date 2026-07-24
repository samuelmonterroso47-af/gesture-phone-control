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
     └── GestureTrainingScreen (tutorial, con preview en vivo) → solo muestra estado, no dispara acciones
```

- **`GestureClassifier`** (`gesture/GestureClassifier.kt`) es lógica pura sin
  dependencias de Android: recibe la posición del punto rastreado por frame y
  decide si hubo un swipe rápido (arriba/abajo/izquierda/derecha), con un
  umbral de velocidad/desplazamiento y un cooldown para no disparar dos veces
  el mismo gesto.
- **`HandPose`** (`gesture/HandPose.kt`) también es lógica pura: clasifica la
  forma de la mano a partir de los 21 landmarks (¿índice y medio extendidos
  con anular y meñique doblados?) y calcula el punto a rastrear.
- **`HandGesturePipeline`** (`gesture/HandGesturePipeline.kt`) encapsula la
  cámara + MediaPipe; la reutilizan tanto el servicio en segundo plano como
  la pantalla de entrenamiento.

### Mapeo de gestos por defecto

| Gesto (mano) | Acción                                   |
|---|---|
| **Índice + medio juntos**, deslizar **arriba** | Swipe sintético hacia arriba (scroll dentro de la app activa) |
| Mano abierta, deslizar **abajo** | Baja el panel de notificaciones |
| Mano abierta, deslizar a la **izquierda** | Atrás |
| Mano abierta, deslizar a la **derecha** | Apps recientes / cambiar de vista |

El gesto de "arriba" exige deliberadamente la seña de dos dedos (índice +
medio extendidos, anular y meñique doblados): levantar la mano frente a la
cámara es algo que uno hace todo el tiempo sin querer, así que sin una seña
explícita ese gesto se dispararía solo. Los otros tres, al ser movimientos
menos frecuentes de forma accidental, funcionan con la mano abierta.

### Tutorial de entrenamiento

La primera vez que abres la app (con permiso de cámara concedido) entra
automáticamente al tutorial guiado: enseña un gesto a la vez, con un diagrama
animado de la seña y el movimiento correcto, la cámara en vivo al lado, y un
semáforo de estado ("no veo tu mano" / "veo tu mano pero no la seña" /
"¡listo, haz el movimiento!"). Cada gesto pide 3 repeticiones correctas antes
de avanzar. Durante el tutorial **no se ejecuta ninguna acción real** del
sistema, así que puedes practicar sin miedo. Se puede repetir cuando quieras
con el botón "Practicar gestos (tutorial)" en la pantalla principal.

### Cómo cambiar el mapeo

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
- **Android 13+ bloquea permisos sensibles en APKs instalados fuera de Play
  Store ("restricted settings").** Como este APK lo instalas manualmente
  (no viene de Play Store), es probable que el interruptor de Accesibilidad
  aparezca gris/deshabilitado la primera vez. Para habilitarlo: Ajustes >
  Apps > Gesture Phone Control > menú de 3 puntos (arriba a la derecha) >
  **"Permitir configuración restringida"**, y luego vuelve a Accesibilidad
  a activarlo. Es un paso único por instalación.
- **La pantalla se mantiene encendida mientras la detección esté activa**,
  porque un teléfono que se apaga a media sesión hace inútil el control sin
  manos. Eso requiere el permiso "Mostrar sobre otras apps": la app coloca
  una ventana invisible de 1x1 px cuyo único fin es cargar la bandera
  `FLAG_KEEP_SCREEN_ON` (un servicio no tiene ventana propia donde ponerla).
  Si no concedes ese permiso todo lo demás sigue funcionando; la pantalla
  simplemente se apaga con su tiempo normal. Recuerda **detener la detección**
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
   cámara). Practica ahí los 4 gestos hasta que te salgan naturales; no
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
│   ├── GestureClassifier.kt              # lógica pura: movimiento -> dirección de swipe
│   ├── HandPose.kt                       # lógica pura: landmarks -> forma de la mano
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
