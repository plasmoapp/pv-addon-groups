package su.plo.voice.groups.util

import su.plo.slib.api.event.GlobalEvent
import java.util.concurrent.CopyOnWriteArraySet

class ScopedEvent<T>(
    private val event: GlobalEvent<T>,
    private val context: GlobalEventContext,
) {
    fun registerListener(listener: T) {
        context.addListener(event, listener)
        event.registerListener(listener)
    }
}

fun <T> GlobalEvent<T>.scoped(context: GlobalEventContext) = ScopedEvent(this, context)

class GlobalEventContext {
    private val listeners = CopyOnWriteArraySet<Pair<GlobalEvent<Any>, Any>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> addListener(
        event: GlobalEvent<T>,
        listener: T,
    ) = listeners.add(event as GlobalEvent<Any> to listener as Any)

    fun unregisterListeners() {
        listeners.forEach { (event, listener) -> event.unregisterListener(listener) }
        listeners.clear()
    }
}
