package su.plo.voice.groups.group

import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.api.server.audio.line.BaseServerSourceLine
import su.plo.voice.api.server.audio.source.ServerBroadcastSource
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.GroupsConfig
import su.plo.voice.groups.api.Group
import su.plo.voice.groups.api.GroupCreateBuilder
import su.plo.voice.groups.api.GroupInvite
import su.plo.voice.groups.api.GroupsApi
import su.plo.voice.groups.api.event.GroupCreatedEvent
import su.plo.voice.groups.api.event.GroupDeletedEvent
import su.plo.voice.groups.api.event.GroupInviteEvent
import su.plo.voice.groups.api.event.GroupInviteRemovedEvent
import su.plo.voice.groups.api.event.GroupNameChangedEvent
import su.plo.voice.groups.api.event.GroupOwnerChangedEvent
import su.plo.voice.groups.api.event.GroupPasswordChangedEvent
import su.plo.voice.groups.api.event.GroupPermissionsChangedEvent
import su.plo.voice.groups.api.event.GroupPersistentChangedEvent
import su.plo.voice.groups.api.event.InviteRemoveReason
import su.plo.voice.groups.api.event.LeaveReason
import su.plo.voice.groups.api.event.PlayerBannedFromGroupEvent
import su.plo.voice.groups.api.event.PlayerJoinedGroupEvent
import su.plo.voice.groups.api.event.PlayerLeftGroupEvent
import su.plo.voice.groups.api.event.PlayerUnbannedFromGroupEvent
import su.plo.voice.groups.api.result.BanResult
import su.plo.voice.groups.api.result.CancelInviteResult
import su.plo.voice.groups.api.result.CreateResult
import su.plo.voice.groups.api.result.DeclineInviteResult
import su.plo.voice.groups.api.result.DeleteResult
import su.plo.voice.groups.api.result.InviteResult
import su.plo.voice.groups.api.result.JoinResult
import su.plo.voice.groups.api.result.KickResult
import su.plo.voice.groups.api.result.LeaveResult
import su.plo.voice.groups.api.result.SetNameResult
import su.plo.voice.groups.api.result.SetPasswordResult
import su.plo.voice.groups.api.result.SetPermissionsResult
import su.plo.voice.groups.api.result.SetPersistentResult
import su.plo.voice.groups.api.result.TransferResult
import su.plo.voice.groups.api.result.UnbanResult
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GroupsManager(
    val config: GroupsConfig,
    private val voiceServer: PlasmoBaseVoiceServer,
    val sourceLine: BaseServerSourceLine,
) : GroupsApi {
    val groupByPlayer: MutableMap<UUID, GroupImpl> = ConcurrentHashMap()
    val groupsById: MutableMap<UUID, GroupImpl> = ConcurrentHashMap()
    val sourceByPlayer: MutableMap<UUID, ServerBroadcastSource> = ConcurrentHashMap()

    override val groups: Collection<Group>
        get() = groupsById.values

    override fun getGroup(id: UUID): Group? = groupsById[id]

    override fun getPlayerGroup(playerId: UUID): Group? = groupByPlayer[playerId]

    override fun getPlayerInvites(playerId: UUID): Collection<GroupInvite> = groupsById.values.mapNotNull { it.getInvite(playerId) }

    @Synchronized
    override fun declineInvite(invite: GroupInvite): DeclineInviteResult {
        val group = groupsById[invite.groupId] ?: return DeclineInviteResult.NotFound
        if (!group.removeInvite(invite)) return DeclineInviteResult.NotFound

        GroupInviteRemovedEvent.invoker.onGroupInviteRemoved(group, invite, InviteRemoveReason.DECLINED)
        return DeclineInviteResult.Success
    }

    override fun createGroup(): GroupCreateBuilder = GroupCreateBuilder(::createGroupInternal)

    @Synchronized
    override fun deleteGroup(group: Group): DeleteResult {
        checkNotDeleted(group)
        deleteGroupInternal(group as GroupImpl)
        return DeleteResult.Success
    }

    override fun join(
        player: VoicePlayer,
        group: Group,
        password: String?,
    ): JoinResult = join(player, group, password, ignorePermissionsFilter = false)

    @Synchronized
    override fun leave(playerId: UUID): LeaveResult {
        val group = groupByPlayer[playerId] ?: return LeaveResult.NotInGroup
        leaveInternal(playerId, LeaveReason.LEFT)
        return LeaveResult.Success(group)
    }

    @Synchronized
    fun join(
        player: VoicePlayer,
        group: Group,
        password: String?,
        ignorePermissionsFilter: Boolean,
    ): JoinResult {
        group as GroupImpl
        checkNotDeleted(group)

        val currentGroup = groupByPlayer[player.instance.uuid]
        if (currentGroup?.id == group.id) return JoinResult.AlreadyInGroup

        if (group.isBanned(player.instance.uuid)) return JoinResult.Banned
        if (!ignorePermissionsFilter && !group.hasPermission(player.instance)) return JoinResult.NoPermission

        if (group.password != null && group.getInvite(player.instance.uuid) == null) {
            if (password == null) return JoinResult.PasswordRequired
            if (group.password != password) return JoinResult.WrongPassword
        }

        return joinUnchecked(player, group)
    }

    @Synchronized
    fun joinUnchecked(
        player: VoicePlayer,
        group: GroupImpl,
    ): JoinResult {
        checkNotDeleted(group)

        val currentGroup = groupByPlayer[player.instance.uuid]
        if (currentGroup?.id == group.id) return JoinResult.AlreadyInGroup

        if (currentGroup != null) {
            leaveInternal(player.instance.uuid, LeaveReason.LEFT)
        }

        initSource(player, group)
        groupByPlayer[player.instance.uuid] = group
        group.addPlayer(player)
        sourceLine.playerSetManager?.setPlayerSet(player, group.playerSet)
        removeInvite(group, player.instance.uuid, InviteRemoveReason.ACCEPTED)

        PlayerJoinedGroupEvent.invoker.onPlayerJoinedGroup(group, player)

        return JoinResult.Success(group)
    }

    fun leave(player: VoicePlayer): LeaveResult = leave(player.instance.uuid)

    @Synchronized
    fun kick(
        group: GroupImpl,
        playerId: UUID,
    ): KickResult {
        checkNotDeleted(group)
        if (playerId !in group.playersById) return KickResult.NotInGroup

        leaveInternal(playerId, LeaveReason.KICKED)
        return KickResult.Success
    }

    @Synchronized
    fun ban(
        group: GroupImpl,
        target: McGameProfile,
    ): BanResult {
        checkNotDeleted(group)
        if (group.owner?.id == target.id) return BanResult.Owner

        val wasMember = target.id in group.playersById
        if (wasMember) leaveInternal(target.id, LeaveReason.BANNED)
        group.banPlayer(target)
        removeInvite(group, target.id, InviteRemoveReason.BANNED)

        PlayerBannedFromGroupEvent.invoker.onPlayerBannedFromGroup(group, target, wasMember)
        return BanResult.Success
    }

    @Synchronized
    fun unban(
        group: GroupImpl,
        target: McGameProfile,
    ): UnbanResult {
        checkNotDeleted(group)
        val removed = group.unbanPlayer(target)
        if (!removed) return UnbanResult.NotBanned

        PlayerUnbannedFromGroupEvent.invoker.onPlayerUnbannedFromGroup(group, target)
        return UnbanResult.Success
    }

    @Synchronized
    fun invite(
        group: GroupImpl,
        inviter: VoicePlayer,
        target: VoicePlayer,
    ): InviteResult {
        checkNotDeleted(group)
        if (inviter.instance.uuid == target.instance.uuid) return InviteResult.Self
        if (group.isBanned(target.instance.uuid)) return InviteResult.Banned
        if (groupByPlayer[target.instance.uuid]?.id == group.id) return InviteResult.AlreadyInGroup

        group.addInvite(inviter.instance.gameProfile, target.instance.gameProfile)
        GroupInviteEvent.invoker.onGroupInvite(group, inviter, target)
        return InviteResult.Success
    }

    @Synchronized
    fun cancelInvite(
        group: GroupImpl,
        playerId: UUID,
    ): CancelInviteResult {
        checkNotDeleted(group)
        if (!removeInvite(group, playerId, InviteRemoveReason.CANCELLED)) return CancelInviteResult.NotInvited
        return CancelInviteResult.Success
    }

    @Synchronized
    fun removePlayerInvites(
        playerId: UUID,
        reason: InviteRemoveReason,
    ) {
        groupsById.values.forEach { removeInvite(it, playerId, reason) }
    }

    @Synchronized
    fun transferOwnership(
        group: GroupImpl,
        playerId: UUID,
    ): TransferResult {
        checkNotDeleted(group)
        val newOwner = group.playersById[playerId] ?: return TransferResult.NotInGroup
        val oldOwner = group.owner
        group.owner = newOwner
        GroupOwnerChangedEvent.invoker.onGroupOwnerChanged(group, oldOwner, group.owner)
        return TransferResult.Success
    }

    @Synchronized
    fun setName(
        group: GroupImpl,
        name: String,
    ): SetNameResult {
        checkNotDeleted(group)
        if (group.name == name) return SetNameResult.IdenticalValue
        if (name.length !in config.minimumNameLength..config.maximumNameLength) {
            return SetNameResult.NameLengthInvalid(config.minimumNameLength, config.maximumNameLength)
        }
        val oldName = group.name
        group.name = name
        GroupNameChangedEvent.invoker.onGroupNameChanged(group, oldName, name)
        return SetNameResult.Success(oldName)
    }

    @Synchronized
    fun setPassword(
        group: GroupImpl,
        password: String?,
    ): SetPasswordResult {
        checkNotDeleted(group)
        if (group.password == password) return SetPasswordResult.IdenticalValue
        val oldPassword = group.password
        group.password = password
        GroupPasswordChangedEvent.invoker.onGroupPasswordChanged(group, oldPassword, password)
        return SetPasswordResult.Success
    }

    @Synchronized
    fun setPermissions(
        group: GroupImpl,
        permissions: Set<String>?,
    ): SetPermissionsResult {
        checkNotDeleted(group)
        val newFilter = permissions?.toHashSet() ?: hashSetOf()
        if (group.permissionsFilterInternal == newFilter) return SetPermissionsResult.IdenticalValue
        val oldFilter = group.permissionsFilterInternal
        group.permissionsFilterInternal = newFilter

        val removed =
            group.onlinePlayers
                .filter { !group.hasPermission(it.instance) }
                .toList()

        GroupPermissionsChangedEvent.invoker.onGroupPermissionsChanged(group, oldFilter, newFilter)

        removed.forEach { kicked ->
            leaveInternal(kicked.instance.uuid, LeaveReason.PERMISSIONS_REVOKED)
        }

        return SetPermissionsResult.Success(removed)
    }

    @Synchronized
    fun setPersistent(
        group: GroupImpl,
        persistent: Boolean,
    ): SetPersistentResult {
        checkNotDeleted(group)
        if (group.persistent == persistent) return SetPersistentResult.IdenticalValue
        group.persistent = persistent
        GroupPersistentChangedEvent.invoker.onGroupPersistentChanged(group, persistent)
        return SetPersistentResult.Success
    }

    @Synchronized
    fun deleteGroupInternal(group: GroupImpl) {
        if (!groupsById.remove(group.id, group)) return
        group.playersById.keys
            .toList()
            .forEach { leaveInternal(it, LeaveReason.GROUP_DELETED) }
        group.invites.toList().forEach { removeInvite(group, it.target.id, InviteRemoveReason.GROUP_DELETED) }
        GroupDeletedEvent.invoker.onGroupDeleted(group)
    }

    @Synchronized
    fun initSource(
        player: VoicePlayer,
        group: GroupImpl,
    ) {
        val source = sourceLine.createBroadcastSource()
        source.players = group.onlinePlayers
        source.addFilter<VoicePlayer> { it.instance != player.instance }
        source.sender = player
        sourceByPlayer[player.instance.uuid] = source
    }

    private fun leaveInternal(
        playerId: UUID,
        reason: LeaveReason,
    ): Boolean {
        sourceByPlayer.remove(playerId)?.let { sourceLine.removeSource(it) }

        val group = groupByPlayer.remove(playerId) ?: return false
        val profile =
            group.removePlayer(playerId).also {
                voiceServer.playerManager
                    .getPlayerById(playerId)
                    .orElse(null)
                    ?.let { sourceLine.playerSetManager?.setPlayerSet(it, null) }
            } ?: return false

        PlayerLeftGroupEvent.invoker.onPlayerLeftGroup(group, profile, reason)

        if (!group.persistent && reason != LeaveReason.GROUP_DELETED) {
            if (group.owner?.id == playerId) {
                val oldOwner = group.owner
                group.owner =
                    group.onlinePlayers
                        .randomOrNull()
                        ?.instance
                        ?.gameProfile
                group.owner?.let {
                    GroupOwnerChangedEvent.invoker.onGroupOwnerChanged(group, oldOwner, it)
                }
            }

            if (group.onlinePlayers.isEmpty()) deleteGroupInternal(group)
        }

        return true
    }

    @Synchronized
    private fun createGroupInternal(builder: GroupCreateBuilder): CreateResult {
        val owner = builder.owner
        val effectiveName =
            builder.name ?: config.defaultGroupNameFormat
                .replace("%player%", owner?.instance?.name ?: "Server")

        if (effectiveName.length !in config.minimumNameLength..config.maximumNameLength) {
            return CreateResult.NameLengthInvalid(config.minimumNameLength, config.maximumNameLength)
        }

        val group =
            GroupImpl(
                this,
                sourceLine.playerSetManager!!.createBroadcastSet(),
                UUID.randomUUID(),
                effectiveName,
                builder.password,
                builder.persistent,
            )

        if (builder.permissions != null) {
            group.permissionsFilterInternal = builder.permissions!!.toHashSet()
        }

        groupsById[group.id] = group

        if (owner != null) {
            group.owner = owner.instance.gameProfile
            join(owner, group, group.password)
        }

        GroupCreatedEvent.invoker.onGroupCreated(group)

        return CreateResult.Success(group)
    }

    private fun removeInvite(
        group: GroupImpl,
        playerId: UUID,
        reason: InviteRemoveReason,
    ): Boolean {
        val invite = group.removeInvite(playerId) ?: return false
        GroupInviteRemovedEvent.invoker.onGroupInviteRemoved(group, invite, reason)
        return true
    }

    private fun checkNotDeleted(group: Group) = check(!group.isDeleted) { "Group ${group.id} is deleted" }
}
