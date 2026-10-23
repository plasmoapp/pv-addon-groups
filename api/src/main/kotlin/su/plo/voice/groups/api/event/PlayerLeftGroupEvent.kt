package su.plo.voice.groups.api.event

import su.plo.slib.api.entity.player.McGameProfile
import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired when a player leaves a group, including offline members removed from it. */
object PlayerLeftGroupEvent : GlobalEvent<PlayerLeftGroupEvent.Callback>(
    { callbacks ->
        Callback { group, profile, reason ->
            callbacks.forEach { it.onPlayerLeftGroup(group, profile, reason) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group the player left.
         * @param profile the profile of the player who left.
         * @param reason why the player left.
         */
        fun onPlayerLeftGroup(
            group: Group,
            profile: McGameProfile,
            reason: LeaveReason,
        )
    }
}
