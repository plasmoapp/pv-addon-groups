package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired after a group is deleted and its members are removed. */
object GroupDeletedEvent : GlobalEvent<GroupDeletedEvent.Callback>(
    { callbacks ->
        Callback { group ->
            callbacks.forEach { it.onGroupDeleted(group) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the deleted group.
         */
        fun onGroupDeleted(group: Group)
    }
}
