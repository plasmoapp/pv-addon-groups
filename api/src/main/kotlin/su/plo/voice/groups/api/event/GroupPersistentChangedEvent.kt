package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired when a group's persistence changes. */
object GroupPersistentChangedEvent : GlobalEvent<GroupPersistentChangedEvent.Callback>(
    { callbacks ->
        Callback { group, persistent ->
            callbacks.forEach { it.onGroupPersistentChanged(group, persistent) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group whose persistence flag changed.
         * @param persistent the new persistence value.
         */
        fun onGroupPersistentChanged(
            group: Group,
            persistent: Boolean,
        )
    }
}
