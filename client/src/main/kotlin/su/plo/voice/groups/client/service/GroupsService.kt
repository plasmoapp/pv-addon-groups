package su.plo.voice.groups.client.service

import su.plo.voice.groups.protocol.CreateResult
import su.plo.voice.groups.protocol.DeclineInviteResult
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupSummary
import su.plo.voice.groups.protocol.GroupsState
import su.plo.voice.groups.protocol.InviteResult
import su.plo.voice.groups.protocol.JoinResult
import su.plo.voice.groups.protocol.LeaveResult
import su.plo.voice.groups.protocol.MemberAction
import su.plo.voice.groups.protocol.MemberActionResult
import java.util.UUID

typealias GroupsStateListener = (GroupsState) -> Unit

interface GroupsService {
    val available: Boolean

    val self: GroupPlayer

    val state: GroupsState?

    val voicePlayers: List<GroupPlayer>

    fun addListener(listener: GroupsStateListener)

    fun removeListener(listener: GroupsStateListener)

    suspend fun fetchState(): GroupsState

    suspend fun browse(): List<GroupSummary>

    suspend fun create(): CreateResult

    suspend fun join(
        groupId: UUID,
        password: String? = null,
    ): JoinResult

    suspend fun leave(): LeaveResult

    suspend fun memberAction(
        groupId: UUID,
        playerId: UUID,
        action: MemberAction,
    ): MemberActionResult

    suspend fun invite(
        groupId: UUID,
        playerId: UUID,
    ): InviteResult

    suspend fun declineInvite(inviteId: UUID): DeclineInviteResult
}
