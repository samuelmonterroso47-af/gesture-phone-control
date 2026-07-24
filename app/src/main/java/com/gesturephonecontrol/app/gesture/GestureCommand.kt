package com.gesturephonecontrol.app.gesture

/**
 * What a recognized pose should make the phone do. Kept as data rather than as code inside the
 * accessibility service so the mapping is one readable table, and so the tutorial can describe the
 * exact same set of commands without duplicating it.
 */
enum class GestureCommand(val label: String, val repeatable: Boolean) {
    SWIPE_UP("Deslizar arriba", repeatable = true),
    SWIPE_DOWN("Deslizar abajo", repeatable = true),
    SWIPE_LEFT("Deslizar a la izquierda", repeatable = true),
    SWIPE_RIGHT("Deslizar a la derecha", repeatable = true),
    NOTIFICATIONS("Bajar notificaciones", repeatable = false),
    BACK("Atrás", repeatable = false),
    HOME("Ir al inicio", repeatable = false)
}

/**
 * The gesture vocabulary. Pointing performs a real directional swipe — the finger points the way
 * the content should move, same as swiping between TikTok modules or Instagram Reels — while the
 * three shape poses cover the system actions that don't have a natural direction.
 *
 * Swiping repeats while held, so a continuous scroll or a run of tabs doesn't need re-pointing for
 * every notch. The shape poses fire once per hold — sostener el puño no debe aporrear "inicio".
 */
val GESTURE_COMMANDS: Map<HandPoseState, GestureCommand> = mapOf(
    HandPoseState.Pointing(PointDirection.UP) to GestureCommand.SWIPE_UP,
    HandPoseState.Pointing(PointDirection.DOWN) to GestureCommand.SWIPE_DOWN,
    HandPoseState.Pointing(PointDirection.LEFT) to GestureCommand.SWIPE_LEFT,
    HandPoseState.Pointing(PointDirection.RIGHT) to GestureCommand.SWIPE_RIGHT,
    HandPoseState.TwoFingers to GestureCommand.NOTIFICATIONS,
    HandPoseState.OpenPalm to GestureCommand.BACK,
    HandPoseState.Fist to GestureCommand.HOME
)

fun commandFor(pose: HandPoseState): GestureCommand? = GESTURE_COMMANDS[pose]
