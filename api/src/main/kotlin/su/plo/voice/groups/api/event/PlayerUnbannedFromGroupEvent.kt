package su.plo.voice.groups.api.event

import su.plo.slib.api.entity.player.McGameProfile
import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired when a player is unbanned from a group. */
object PlayerUnbannedFromGroupEvent : GlobalEvent<PlayerUnbannedFromGroupEvent.Callback>(
    { callbacks ->
        Callback { group, profile ->
            callbacks.forEach { it.onPlayerUnbannedFromGroup(group, profile) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group the player was unbanned from.
         * @param profile the unbanned player's profile.
         */
        fun onPlayerUnbannedFromGroup(
            group: Group,
            profile: McGameProfile,
        )
    }
}
