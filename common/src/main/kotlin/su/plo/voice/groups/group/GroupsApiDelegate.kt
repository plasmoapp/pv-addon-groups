package su.plo.voice.groups.group

import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group
import su.plo.voice.groups.api.GroupCreateBuilder
import su.plo.voice.groups.api.GroupInvite
import su.plo.voice.groups.api.GroupsApi
import su.plo.voice.groups.api.result.DeclineInviteResult
import su.plo.voice.groups.api.result.DeleteResult
import su.plo.voice.groups.api.result.JoinResult
import su.plo.voice.groups.api.result.LeaveResult
import java.util.UUID

class GroupsApiDelegate(
    private val groupManager: () -> GroupsManager,
) : GroupsApi {
    override val groups: Collection<Group>
        get() = groupManager().groups

    override fun getGroup(id: UUID): Group? = groupManager().getGroup(id)

    override fun getPlayerGroup(playerId: UUID): Group? = groupManager().getPlayerGroup(playerId)

    override fun getPlayerInvites(playerId: UUID): Collection<GroupInvite> = groupManager().getPlayerInvites(playerId)

    override fun declineInvite(invite: GroupInvite): DeclineInviteResult = groupManager().declineInvite(invite)

    override fun createGroup(): GroupCreateBuilder = groupManager().createGroup()

    override fun deleteGroup(group: Group): DeleteResult = groupManager().deleteGroup(group)

    override fun join(
        player: VoicePlayer,
        group: Group,
        password: String?,
    ): JoinResult = groupManager().join(player, group, password)

    override fun leave(playerId: UUID): LeaveResult = groupManager().leave(playerId)
}
