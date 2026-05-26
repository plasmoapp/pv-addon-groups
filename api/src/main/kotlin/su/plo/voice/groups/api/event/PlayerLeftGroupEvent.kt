package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group

/** Fired when a player leaves a group. */
object PlayerLeftGroupEvent : GlobalEvent<PlayerLeftGroupEvent.Callback>(
    { callbacks ->
        Callback { group, player, reason ->
            callbacks.forEach { it.onPlayerLeftGroup(group, player, reason) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group the player left.
         * @param player the player who left.
         * @param reason why the player left.
         */
        fun onPlayerLeftGroup(
            group: Group,
            player: VoicePlayer,
            reason: LeaveReason,
        )
    }
}
