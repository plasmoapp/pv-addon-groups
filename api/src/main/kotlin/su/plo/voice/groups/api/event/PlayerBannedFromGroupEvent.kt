package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group

/** Fired when a player is banned from a group. */
object PlayerBannedFromGroupEvent : GlobalEvent<PlayerBannedFromGroupEvent.Callback>(
    { callbacks ->
        Callback { group, player, wasMember ->
            callbacks.forEach { it.onPlayerBannedFromGroup(group, player, wasMember) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group the player was banned from.
         * @param player the banned player.
         * @param wasMember whether the player was a member at the time of the ban.
         */
        fun onPlayerBannedFromGroup(
            group: Group,
            player: VoicePlayer,
            wasMember: Boolean,
        )
    }
}
