package su.plo.voice.groups.api

import org.jetbrains.annotations.ApiStatus
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.groups.api.event.InviteRemoveReason
import java.util.UUID

/**
 * A pending invite to a group.
 *
 * Lets [target] join the group without the password until it's removed.
 */
@ApiStatus.NonExtendable
interface GroupInvite {
    /** The invite ID. */
    val id: UUID

    /** The ID of the group the player is invited to. */
    val groupId: UUID

    /** The player who sent the invite. */
    val inviter: McGameProfile

    /** The invited player. */
    val target: McGameProfile
}
