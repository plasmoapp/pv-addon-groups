package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group

/** Fired when a player is invited to a group. */
object GroupInviteEvent : GlobalEvent<GroupInviteEvent.Callback>(
    { callbacks ->
        Callback { group, inviter, target ->
            callbacks.forEach { it.onGroupInvite(group, inviter, target) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group the player is invited to.
         * @param inviter the player who sent the invite.
         * @param target the invited player.
         */
        fun onGroupInvite(
            group: Group,
            inviter: VoicePlayer,
            target: VoicePlayer,
        )
    }
}
