package com.gesturephonecontrol.app.gesture

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process bridge between [GestureForegroundService] (which detects gestures from the camera)
 * and [com.gesturephonecontrol.app.accessibility.GestureAccessibilityService] (which acts on
 * them). Both run in the same app process, so a shared singleton is enough — no IPC needed.
 */
object GestureEventBus {
    private val _events = MutableSharedFlow<GestureCommand>(extraBufferCapacity = 4)
    val events: SharedFlow<GestureCommand> = _events.asSharedFlow()

    fun emit(command: GestureCommand) {
        _events.tryEmit(command)
    }
}
