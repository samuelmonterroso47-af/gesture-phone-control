package com.gesturephonecontrol.app.training

import com.gesturephonecontrol.app.gesture.GESTURE_COMMANDS
import com.gesturephonecontrol.app.gesture.HandPoseState
import com.gesturephonecontrol.app.gesture.PointDirection

/**
 * One step of the guided tutorial: the pose being taught and how to hold it. The pose and its
 * resulting action come from [GESTURE_COMMANDS], so the tutorial can never drift out of sync with
 * what the app actually does.
 */
data class GestureLesson(
    val pose: HandPoseState,
    val title: String,
    val instruction: String,
    val repsToPass: Int = 2
) {
    val actionLabel: String
        get() = GESTURE_COMMANDS.getValue(pose).label
}

private const val INDEX_ONLY =
    "Extiende solo el dedo índice (los demás doblados) y apunta"

val GESTURE_LESSONS = listOf(
    GestureLesson(
        pose = HandPoseState.Pointing(PointDirection.UP),
        title = "Scroll hacia arriba",
        instruction = "$INDEX_ONLY hacia arriba. Sostén la pose — mientras la mantengas, sigue haciendo scroll."
    ),
    GestureLesson(
        pose = HandPoseState.Pointing(PointDirection.DOWN),
        title = "Scroll hacia abajo",
        instruction = "$INDEX_ONLY hacia abajo. Sostén la pose para seguir bajando."
    ),
    GestureLesson(
        pose = HandPoseState.Pointing(PointDirection.LEFT),
        title = "Atrás",
        instruction = "$INDEX_ONLY hacia tu izquierda, con el dedo en horizontal."
    ),
    GestureLesson(
        pose = HandPoseState.Pointing(PointDirection.RIGHT),
        title = "Apps recientes",
        instruction = "$INDEX_ONLY hacia tu derecha, con el dedo en horizontal."
    ),
    GestureLesson(
        pose = HandPoseState.TwoFingers,
        title = "Bajar notificaciones",
        instruction = "Levanta el índice y el medio en \"V\". Sostén la pose."
    ),
    GestureLesson(
        pose = HandPoseState.Fist,
        title = "Ir al inicio",
        instruction = "Cierra la mano en un puño y sostenlo frente a la cámara."
    )
)
