package su.plo.voice.groups.api.event

import su.plo.slib.api.entity.player.McGameProfile
import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group

/** Fired when a player is banned from a group. */
object PlayerBannedFromGroupEvent : GlobalEvent<PlayerBannedFromGroupEvent.Callback>(
    { callbacks ->
        Callback { group, profile, wasMember ->
            callbacks.forEach { it.onPlayerBannedFromGroup(group, profile, wasMember) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group the player was banned from.
         * @param profile the banned player's profile.
         * @param wasMember whether the player was a member at the time of the ban.
         */
        fun onPlayerBannedFromGroup(
            group: Group,
            profile: McGameProfile,
            wasMember: Boolean,
        )
    }
}
