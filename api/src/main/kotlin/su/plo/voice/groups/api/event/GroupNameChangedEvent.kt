package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired when a group is renamed. */
object GroupNameChangedEvent : GlobalEvent<GroupNameChangedEvent.Callback>(
    { callbacks ->
        Callback { group, oldName, newName ->
            callbacks.forEach { it.onGroupNameChanged(group, oldName, newName) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the renamed group.
         * @param oldName the previous group name.
         * @param newName the new group name.
         */
        fun onGroupNameChanged(
            group: Group,
            oldName: String,
            newName: String,
        )
    }
}
