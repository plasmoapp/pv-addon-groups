package su.plo.voice.groups.api

import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.event.GroupInviteRemovedEvent
import su.plo.voice.groups.api.event.InviteRemoveReason
import su.plo.voice.groups.api.result.DeclineInviteResult
import su.plo.voice.groups.api.result.DeleteResult
import su.plo.voice.groups.api.result.JoinResult
import su.plo.voice.groups.api.result.LeaveResult
import java.util.UUID

/**
 * Entry point of the groups addon API.
 */
interface GroupsApi {
    /** Unmodifiable live view of all groups. */
    val groups: Collection<Group>

    /**
     * Returns the group with [id], or `null` if it doesn't exist.
     *
     * @param id the group ID.
     */
    fun getGroup(id: UUID): Group?

    /**
     * Returns the group the player is in, or `null` if they aren't in one.
     *
     * @param playerId the player UUID.
     */
    fun getPlayerGroup(playerId: UUID): Group?

    /**
     * Returns the pending invites of the player.
     *
     * @param playerId the player UUID.
     */
    fun getPlayerInvites(playerId: UUID): Collection<GroupInvite>

    /**
     * Declines [invite]. Fires [GroupInviteRemovedEvent] with [InviteRemoveReason.DECLINED].
     *
     * @param invite the invite to decline.
     */
    fun declineInvite(invite: GroupInvite): DeclineInviteResult

    /** Returns a builder for a new group. */
    fun createGroup(): GroupCreateBuilder

    /**
     * Deletes [group], removing all members.
     *
     * @param group the group to delete.
     * @throws IllegalStateException if [group] is already deleted.
     */
    fun deleteGroup(group: Group): DeleteResult

    /**
     * Adds [player] to [group], removing them from their current group first.
     *
     * @param player the player to add.
     * @param group the group to join.
     * @param password the password, or `null` if not provided. Not checked if the player has a pending invite to [group].
     * @throws IllegalStateException if [group] is deleted.
     */
    fun join(
        player: VoicePlayer,
        group: Group,
        password: String?,
    ): JoinResult

    /**
     * Removes the player from their current group.
     *
     * @param playerId the player UUID.
     */
    fun leave(playerId: UUID): LeaveResult
}
