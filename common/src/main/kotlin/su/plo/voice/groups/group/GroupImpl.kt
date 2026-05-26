package su.plo.voice.groups.group

import com.google.common.collect.Sets
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.api.server.audio.line.ServerPlayerSet
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group
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
import java.util.concurrent.CopyOnWriteArrayList

class GroupImpl(
    internal val manager: GroupsManager,
    val playerSet: ServerPlayerSet,
    id: UUID,
    name: String,
    password: String? = null,
    persistent: Boolean = false,
    playersIds: MutableSet<UUID> = Sets.newConcurrentHashSet(),
    bannedPlayers: MutableList<McGameProfile> = CopyOnWriteArrayList(),
    owner: McGameProfile? = null,
) : GroupData(id, name, password, persistent, playersIds, bannedPlayers, owner),
    Group {
    internal var permissionsFilterInternal: HashSet<String> = HashSet()

    override val isDeleted: Boolean
        get() = manager.groupsById[id] !== this

    override val permissionsFilter: Set<String>
        get() = permissionsFilterInternal

    override val onlinePlayers: Collection<VoicePlayer>
        get() = playerSet.players

    override val playerCount: Int
        get() = playersIds.size

    override val onlinePlayerCount: Int
        get() = onlinePlayers.size

    fun addPlayer(player: VoicePlayer) {
        playersIds.add(player.instance.uuid)
        onPlayerJoin(player)
    }

    fun removePlayer(player: VoicePlayer): Boolean = removePlayer(player.instance.uuid)

    fun removePlayer(playerUuid: UUID): Boolean =
        playersIds.remove(playerUuid).also {
            onPlayerQuit(playerUuid)
        }

    fun onPlayerJoin(player: VoicePlayer) {
        playerSet.addPlayer(player)
    }

    fun onPlayerQuit(player: VoicePlayer): Boolean = onPlayerQuit(player.instance.uuid)

    fun onPlayerQuit(playerUuid: UUID): Boolean = playerSet.removePlayer(playerUuid)

    override fun isBanned(playerUuid: UUID): Boolean = bannedPlayers.any { it.id == playerUuid }

    fun banPlayer(player: VoicePlayer) {
        bannedPlayers.add(player.instance.gameProfile)
    }

    fun unbanPlayer(player: McGameProfile): Boolean = bannedPlayers.removeIf { it.id == player.id }

    override fun isOwner(player: VoicePlayer): Boolean = owner?.id == player.instance.uuid

    override fun hasPermission(source: McCommandSource): Boolean {
        if (permissionsFilterInternal.isEmpty()) return true
        return permissionsFilterInternal.any { source.hasPermission(it) }
    }

    override fun kick(target: VoicePlayer): KickResult = manager.kick(this, target)

    override fun ban(target: VoicePlayer): BanResult = manager.ban(this, target)

    override fun unban(target: McGameProfile): UnbanResult = manager.unban(this, target)

    override fun invite(
        inviter: VoicePlayer,
        target: VoicePlayer,
    ): InviteResult = manager.invite(this, inviter, target)

    override fun transferOwnership(target: VoicePlayer): TransferResult = manager.transferOwnership(this, target)

    override fun setName(name: String): SetNameResult = manager.setName(this, name)

    override fun setPassword(password: String?): SetPasswordResult = manager.setPassword(this, password)

    override fun setPermissions(permissions: Set<String>?): SetPermissionsResult = manager.setPermissions(this, permissions)

    override fun setPersistent(persistent: Boolean): SetPersistentResult = manager.setPersistent(this, persistent)
}
