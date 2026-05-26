package su.plo.voice.groups.api

import org.jetbrains.annotations.ApiStatus
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.event.GroupInviteEvent
import su.plo.voice.groups.api.result.BanResult
import su.plo.voice.groups.api.result.InviteResult
import su.plo.voice.groups.api.result.KickResult
import su.plo.voice.groups.api.result.SetNameResult
import su.plo.voice.groups.api.result.SetPasswordResult
import su.plo.voice.groups.api.result.SetPermissionsResult
import su.plo.voice.groups.api.result.SetPersistentResult
import su.plo.voice.groups.api.result.TransferResult
import su.plo.voice.groups.api.result.UnbanResult
import java.util.UUID

/**
 * A group of players sharing a voice channel.
 *
 * Mutating methods throw [IllegalStateException] once the group [isDeleted].
 */
@ApiStatus.NonExtendable
interface Group {
    /** The group ID. */
    val id: UUID

    /**
     * Whether the group was deleted, either explicitly or automatically after its last member left.
     */
    val isDeleted: Boolean

    /** The group name. */
    val name: String

    /** The group password, or `null` if there is none. */
    val password: String?

    /** Whether the group persists across restarts. */
    val persistent: Boolean

    /** The group owner, or `null` if there is none. */
    val owner: McGameProfile?

    /** Permissions required to join, any one of which is enough. Empty means no restriction. */
    val permissionsFilter: Set<String>

    /** Banned players. */
    val bannedPlayers: List<McGameProfile>

    /** Number of members, including offline ones. */
    val playerCount: Int

    /** Number of online members. */
    val onlinePlayerCount: Int

    /** Online members. */
    val onlinePlayers: Collection<VoicePlayer>

    /**
     * Returns `true` if the player is banned.
     *
     * @param playerUuid the player UUID.
     */
    fun isBanned(playerUuid: UUID): Boolean

    /**
     * Returns `true` if [player] is the owner.
     *
     * @param player the player to check.
     */
    fun isOwner(player: VoicePlayer): Boolean

    /**
     * Returns `true` if [source] passes [permissionsFilter].
     *
     * @param source the source to check.
     */
    fun hasPermission(source: McCommandSource): Boolean

    /**
     * Kicks [target] from the group.
     *
     * @param target the player to kick.
     */
    fun kick(target: VoicePlayer): KickResult

    /**
     * Bans [target], kicking them if they are a member.
     *
     * @param target the player to ban.
     */
    fun ban(target: VoicePlayer): BanResult

    /**
     * Unbans [target].
     *
     * @param target the player to unban.
     */
    fun unban(target: McGameProfile): UnbanResult

    /**
     * Fires [GroupInviteEvent] if [target] can be invited. Doesn't notify [target].
     *
     * @param inviter the inviting player.
     * @param target the invited player.
     */
    fun invite(
        inviter: VoicePlayer,
        target: VoicePlayer,
    ): InviteResult

    /**
     * Transfers ownership to [target], who must be a member.
     *
     * @param target the new owner.
     */
    fun transferOwnership(target: VoicePlayer): TransferResult

    /**
     * Renames the group.
     *
     * @param name the new name.
     */
    fun setName(name: String): SetNameResult

    /**
     * Sets the group password.
     *
     * @param password the new password, or `null` to remove it.
     */
    fun setPassword(password: String?): SetPasswordResult

    /**
     * Sets the permission filter, kicking members who no longer pass it.
     *
     * @param permissions the new filter, or `null` to clear it.
     */
    fun setPermissions(permissions: Set<String>?): SetPermissionsResult

    /**
     * Sets whether the group persists across restarts.
     *
     * @param persistent the new value.
     */
    fun setPersistent(persistent: Boolean): SetPersistentResult
}
