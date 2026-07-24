package com.gesturephonecontrol.app.gesture

/**
 * What a recognized pose should make the phone do. Kept as data rather than as code inside the
 * accessibility service so the mapping is one readable table, and so the tutorial can describe the
 * exact same set of commands without duplicating it.
 */
enum class GestureCommand(val label: String, val repeatable: Boolean) {
    SCROLL_UP("Scroll hacia arriba", repeatable = true),
    SCROLL_DOWN("Scroll hacia abajo", repeatable = true),
    BACK("Atrás", repeatable = false),
    RECENTS("Apps recientes", repeatable = false),
    NOTIFICATIONS("Bajar notificaciones", repeatable = false),
    HOME("Ir al inicio", repeatable = false)
}

/**
 * The gesture vocabulary. Pointing with the index finger covers the four directional commands —
 * the finger points the way you want to go — while the two shape poses cover the remaining two.
 *
 * Only scrolling repeats while held; the rest fire once per pose so that holding a fist doesn't
 * slam the home button over and over.
 */
val GESTURE_COMMANDS: Map<HandPoseState, GestureCommand> = mapOf(
    HandPoseState.Pointing(PointDirection.UP) to GestureCommand.SCROLL_UP,
    HandPoseState.Pointing(PointDirection.DOWN) to GestureCommand.SCROLL_DOWN,
    HandPoseState.Pointing(PointDirection.LEFT) to GestureCommand.BACK,
    HandPoseState.Pointing(PointDirection.RIGHT) to GestureCommand.RECENTS,
    HandPoseState.TwoFingers to GestureCommand.NOTIFICATIONS,
    HandPoseState.Fist to GestureCommand.HOME
)

fun commandFor(pose: HandPoseState): GestureCommand? = GESTURE_COMMANDS[pose]
