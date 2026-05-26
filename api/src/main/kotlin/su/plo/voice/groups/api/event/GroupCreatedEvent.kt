package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired when a group is created. */
object GroupCreatedEvent : GlobalEvent<GroupCreatedEvent.Callback>(
    { callbacks ->
        Callback { group ->
            callbacks.forEach { it.onGroupCreated(group) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the newly created group.
         */
        fun onGroupCreated(group: Group)
    }
}
