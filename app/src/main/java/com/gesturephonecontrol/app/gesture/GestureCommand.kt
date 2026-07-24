package com.gesturephonecontrol.app.gesture

/**
 * What a recognized gesture should make the phone do. Kept as data rather than as code inside the
 * accessibility service so the mapping is one readable table, and so the tutorial can describe the
 * exact same set of commands without duplicating it.
 */
enum class GestureCommand(val label: String) {
    SCROLL_UP("Scroll hacia arriba"),
    SCROLL_DOWN("Scroll hacia abajo"),
    NOTIFICATIONS("Bajar notificaciones"),
    BACK("Atrás"),
    RECENTS("Apps recientes"),
    HOME("Ir al inicio")
}

/**
 * The gesture vocabulary, organised so the hand pose picks a "mode" and the swipe direction picks
 * the action within it — two fingers scroll, an open palm navigates, a fist goes home. Grouping it
 * that way keeps the set memorable as it grows, instead of four unrelated shortcuts.
 */
val GESTURE_COMMANDS: Map<GestureEvent, GestureCommand> = mapOf(
    GestureEvent(HandShape.TWO_FINGERS, GestureDirection.UP) to GestureCommand.SCROLL_UP,
    GestureEvent(HandShape.TWO_FINGERS, GestureDirection.DOWN) to GestureCommand.SCROLL_DOWN,

    GestureEvent(HandShape.OPEN_PALM, GestureDirection.DOWN) to GestureCommand.NOTIFICATIONS,
    GestureEvent(HandShape.OPEN_PALM, GestureDirection.LEFT) to GestureCommand.BACK,
    GestureEvent(HandShape.OPEN_PALM, GestureDirection.RIGHT) to GestureCommand.RECENTS,

    GestureEvent(HandShape.FIST, GestureDirection.UP) to GestureCommand.HOME
)

fun commandFor(event: GestureEvent): GestureCommand? = GESTURE_COMMANDS[event]
