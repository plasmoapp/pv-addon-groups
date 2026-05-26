package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired when a group's permission filter changes. */
object GroupPermissionsChangedEvent : GlobalEvent<GroupPermissionsChangedEvent.Callback>(
    { callbacks ->
        Callback { group, oldPermissions, newPermissions ->
            callbacks.forEach { it.onGroupPermissionsChanged(group, oldPermissions, newPermissions) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group whose permission filter changed.
         * @param oldPermissions the previous set of required permissions.
         * @param newPermissions the new set of required permissions.
         */
        fun onGroupPermissionsChanged(
            group: Group,
            oldPermissions: Set<String>,
            newPermissions: Set<String>,
        )
    }
}
