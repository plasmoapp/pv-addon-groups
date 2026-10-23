package su.plo.voice.groups.client.service

import su.plo.voice.groups.protocol.BanResult
import su.plo.voice.groups.protocol.CancelInviteResult
import su.plo.voice.groups.protocol.CreateResult
import su.plo.voice.groups.protocol.DeclineInviteResult
import su.plo.voice.groups.protocol.DeleteResult
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupSummary
import su.plo.voice.groups.protocol.GroupsState
import su.plo.voice.groups.protocol.InviteResult
import su.plo.voice.groups.protocol.JoinResult
import su.plo.voice.groups.protocol.KickResult
import su.plo.voice.groups.protocol.LeaveResult
import su.plo.voice.groups.protocol.TransferResult
import su.plo.voice.groups.protocol.UnbanResult
import su.plo.voice.groups.protocol.UpdateSettingsRequest
import su.plo.voice.groups.protocol.UpdateSettingsResult
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

    suspend fun kick(
        groupId: UUID,
        playerId: UUID,
    ): KickResult

    suspend fun ban(
        groupId: UUID,
        playerId: UUID,
    ): BanResult

    suspend fun unban(
        groupId: UUID,
        playerId: UUID,
    ): UnbanResult

    suspend fun transfer(
        groupId: UUID,
        playerId: UUID,
    ): TransferResult

    suspend fun invite(
        groupId: UUID,
        playerId: UUID,
    ): InviteResult

    suspend fun cancelInvite(
        groupId: UUID,
        playerId: UUID,
    ): CancelInviteResult

    suspend fun declineInvite(inviteId: UUID): DeclineInviteResult

    suspend fun delete(groupId: UUID): DeleteResult

    suspend fun updateSettings(request: UpdateSettingsRequest): UpdateSettingsResult
}
