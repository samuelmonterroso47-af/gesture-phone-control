package com.gesturephonecontrol.app.training

import com.gesturephonecontrol.app.gesture.GESTURE_COMMANDS
import com.gesturephonecontrol.app.gesture.GestureDirection
import com.gesturephonecontrol.app.gesture.GestureEvent
import com.gesturephonecontrol.app.gesture.HandShape

/**
 * One step of the guided tutorial: the gesture being taught and how to perform it. The gesture and
 * its resulting action come from [GESTURE_COMMANDS], so the tutorial can never drift out of sync
 * with what the app actually does.
 */
data class GestureLesson(
    val event: GestureEvent,
    val title: String,
    val poseInstruction: String,
    val motionInstruction: String,
    val repsToPass: Int = 3
) {
    val actionLabel: String
        get() = GESTURE_COMMANDS.getValue(event).label
}

private const val TWO_FINGERS_POSE =
    "Levanta solo el índice y el medio, juntos. Dobla el anular y el meñique."
private const val OPEN_PALM_POSE = "Mano completamente abierta, con la palma hacia la cámara."
private const val FIST_POSE = "Cierra la mano en un puño."

val GESTURE_LESSONS = listOf(
    GestureLesson(
        event = GestureEvent(HandShape.TWO_FINGERS, GestureDirection.UP),
        title = "Scroll hacia arriba",
        poseInstruction = TWO_FINGERS_POSE,
        motionInstruction = "Con esos dos dedos, sube la mano rápido, en línea recta."
    ),
    GestureLesson(
        event = GestureEvent(HandShape.TWO_FINGERS, GestureDirection.DOWN),
        title = "Scroll hacia abajo",
        poseInstruction = TWO_FINGERS_POSE,
        motionInstruction = "Con los mismos dos dedos, baja la mano rápido, en línea recta."
    ),
    GestureLesson(
        event = GestureEvent(HandShape.OPEN_PALM, GestureDirection.DOWN),
        title = "Bajar notificaciones",
        poseInstruction = OPEN_PALM_POSE,
        motionInstruction = "Baja la mano rápido, como si limpiaras un vidrio."
    ),
    GestureLesson(
        event = GestureEvent(HandShape.OPEN_PALM, GestureDirection.LEFT),
        title = "Atrás",
        poseInstruction = OPEN_PALM_POSE,
        motionInstruction = "Mueve la mano rápido hacia tu izquierda, en línea recta."
    ),
    GestureLesson(
        event = GestureEvent(HandShape.OPEN_PALM, GestureDirection.RIGHT),
        title = "Apps recientes",
        poseInstruction = OPEN_PALM_POSE,
        motionInstruction = "Mueve la mano rápido hacia tu derecha, en línea recta."
    ),
    GestureLesson(
        event = GestureEvent(HandShape.FIST, GestureDirection.UP),
        title = "Ir al inicio",
        poseInstruction = FIST_POSE,
        motionInstruction = "Con el puño cerrado, sube la mano rápido, en línea recta."
    )
)
