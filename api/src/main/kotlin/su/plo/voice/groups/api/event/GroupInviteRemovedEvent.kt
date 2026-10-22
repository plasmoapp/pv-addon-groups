package su.plo.voice.groups.api.event

import su.plo.slib.api.event.GlobalEvent
import su.plo.voice.groups.api.Group
import su.plo.voice.groups.api.GroupInvite

/** Fired when a pending invite is removed. */
object GroupInviteRemovedEvent : GlobalEvent<GroupInviteRemovedEvent.Callback>(
    { callbacks ->
        Callback { group, invite, reason ->
            callbacks.forEach { it.onGroupInviteRemoved(group, invite, reason) }
        }
    },
) {
    fun interface Callback {
        /**
         * @param group the group the invite was for. Already deleted if [reason] is [InviteRemoveReason.GROUP_DELETED].
         * @param invite the removed invite.
         * @param reason why the invite was removed.
         */
        fun onGroupInviteRemoved(
            group: Group,
            invite: GroupInvite,
            reason: InviteRemoveReason,
        )
    }
}
