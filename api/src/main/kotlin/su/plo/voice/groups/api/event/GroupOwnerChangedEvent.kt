package su.plo.voice.groups.api.event

import su.plo.slib.api.entity.player.McGameProfile
import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired when a group's owner changes. */
object GroupOwnerChangedEvent : GlobalEvent<GroupOwnerChangedEvent.Callback>(
    { callbacks ->
        Callback { group, oldOwner, newOwner ->
            callbacks.forEach { it.onGroupOwnerChanged(group, oldOwner, newOwner) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group whose owner changed.
         * @param oldOwner the previous owner's profile, or `null` if there was none.
         * @param newOwner the new owner's profile, or `null` if ownership was cleared.
         */
        fun onGroupOwnerChanged(
            group: Group,
            oldOwner: McGameProfile?,
            newOwner: McGameProfile?,
        )
    }
}
