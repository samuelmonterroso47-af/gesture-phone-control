package com.gesturephonecontrol.app.training

import com.gesturephonecontrol.app.gesture.GestureDirection
import com.gesturephonecontrol.app.gesture.HandShape

/**
 * One step of the guided tutorial: which gesture is being taught, how to perform it, and what the
 * resulting action is. [requiredShape] is non-null when the gesture demands a deliberate hand pose,
 * which the training screen coaches the user into holding before they swipe.
 */
data class GestureLesson(
    val direction: GestureDirection,
    val title: String,
    val poseInstruction: String,
    val motionInstruction: String,
    val actionDescription: String,
    val requiredShape: HandShape?,
    val repsToPass: Int = 3
)

val GESTURE_LESSONS = listOf(
    GestureLesson(
        direction = GestureDirection.UP,
        title = "Deslizar arriba",
        poseInstruction = "Levanta solo el dedo índice y el medio, juntos (como una V cerrada). " +
            "Dobla el anular y el meñique.",
        motionInstruction = "Con esos dos dedos, sube la mano rápido, en línea recta.",
        actionDescription = "Hace scroll hacia arriba en la app que tengas abierta.",
        requiredShape = HandShape.TWO_FINGERS
    ),
    GestureLesson(
        direction = GestureDirection.DOWN,
        title = "Deslizar abajo",
        poseInstruction = "Mano abierta, con la palma hacia la cámara.",
        motionInstruction = "Baja la mano rápido, en línea recta, como si limpiaras un vidrio.",
        actionDescription = "Baja el panel de notificaciones.",
        requiredShape = null
    ),
    GestureLesson(
        direction = GestureDirection.LEFT,
        title = "Deslizar a la izquierda",
        poseInstruction = "Mano abierta, con la palma hacia la cámara.",
        motionInstruction = "Mueve la mano rápido hacia tu izquierda, en línea recta.",
        actionDescription = "Va hacia atrás (botón Atrás).",
        requiredShape = null
    ),
    GestureLesson(
        direction = GestureDirection.RIGHT,
        title = "Deslizar a la derecha",
        poseInstruction = "Mano abierta, con la palma hacia la cámara.",
        motionInstruction = "Mueve la mano rápido hacia tu derecha, en línea recta.",
        actionDescription = "Abre las apps recientes para cambiar de vista.",
        requiredShape = null
    )
)
