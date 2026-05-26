package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired when a group's password changes. */
object GroupPasswordChangedEvent : GlobalEvent<GroupPasswordChangedEvent.Callback>(
    { callbacks ->
        Callback { group, oldPassword, newPassword ->
            callbacks.forEach { it.onGroupPasswordChanged(group, oldPassword, newPassword) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group whose password changed.
         * @param oldPassword the previous password, or `null` if none was set.
         * @param newPassword the new password, or `null` if the password was cleared.
         */
        fun onGroupPasswordChanged(
            group: Group,
            oldPassword: String?,
            newPassword: String?,
        )
    }
}
