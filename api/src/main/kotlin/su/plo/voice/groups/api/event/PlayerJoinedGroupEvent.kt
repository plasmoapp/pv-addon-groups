package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group

/** Fired when a player joins a group. */
object PlayerJoinedGroupEvent : GlobalEvent<PlayerJoinedGroupEvent.Callback>(
    { callbacks ->
        Callback { group, player ->
            callbacks.forEach { it.onPlayerJoinedGroup(group, player) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the joined group.
         * @param player the player who joined.
         */
        fun onPlayerJoinedGroup(
            group: Group,
            player: VoicePlayer,
        )
    }
}
