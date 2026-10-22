package su.plo.voice.groups.group

import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.groups.api.GroupInvite
import java.util.UUID

data class GroupInviteImpl(
    override val id: UUID,
    override val groupId: UUID,
    override val inviter: McGameProfile,
    override val target: McGameProfile,
) : GroupInvite
