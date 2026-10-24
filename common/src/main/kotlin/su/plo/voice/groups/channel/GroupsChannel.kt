package su.plo.voice.groups.channel

import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.group.GroupImpl
import su.plo.voice.groups.listener.GroupsStateListener
import su.plo.voice.groups.protocol.AlreadyInGroup
import su.plo.voice.groups.protocol.BanRequest
import su.plo.voice.groups.protocol.BanResult
import su.plo.voice.groups.protocol.Banned
import su.plo.voice.groups.protocol.BrowseRequest
import su.plo.voice.groups.protocol.BrowseResponse
import su.plo.voice.groups.protocol.CHANNEL_KEY
import su.plo.voice.groups.protocol.CancelInviteRequest
import su.plo.voice.groups.protocol.CancelInviteResult
import su.plo.voice.groups.protocol.ChannelRequest
import su.plo.voice.groups.protocol.CreateRequest
import su.plo.voice.groups.protocol.CreateResult
import su.plo.voice.groups.protocol.DeclineInviteRequest
import su.plo.voice.groups.protocol.DeclineInviteResult
import su.plo.voice.groups.protocol.DeleteRequest
import su.plo.voice.groups.protocol.DeleteResult
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupNotFound
import su.plo.voice.groups.protocol.GroupsState
import su.plo.voice.groups.protocol.HandshakeRequest
import su.plo.voice.groups.protocol.HandshakeResponse
import su.plo.voice.groups.protocol.InviteRequest
import su.plo.voice.groups.protocol.InviteResult
import su.plo.voice.groups.protocol.JoinRequest
import su.plo.voice.groups.protocol.JoinResult
import su.plo.voice.groups.protocol.KickRequest
import su.plo.voice.groups.protocol.KickResult
import su.plo.voice.groups.protocol.LeaveRequest
import su.plo.voice.groups.protocol.LeaveResult
import su.plo.voice.groups.protocol.NameLengthInvalid
import su.plo.voice.groups.protocol.NoPermission
import su.plo.voice.groups.protocol.NotBanned
import su.plo.voice.groups.protocol.NotInGroup
import su.plo.voice.groups.protocol.Owner
import su.plo.voice.groups.protocol.PasswordRequired
import su.plo.voice.groups.protocol.PlayerNotFound
import su.plo.voice.groups.protocol.Self
import su.plo.voice.groups.protocol.StateRequest
import su.plo.voice.groups.protocol.Success
import su.plo.voice.groups.protocol.TransferRequest
import su.plo.voice.groups.protocol.TransferResult
import su.plo.voice.groups.protocol.UnbanRequest
import su.plo.voice.groups.protocol.UnbanResult
import su.plo.voice.groups.protocol.UpdateSettingsRequest
import su.plo.voice.groups.protocol.UpdateSettingsResult
import su.plo.voice.groups.protocol.WrongPassword
import su.plo.voice.groups.util.PlayerCollector
import su.plo.voice.groups.util.extend.createGroup
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.hasFlagPermission
import su.plo.voice.groups.util.extend.hasTierPermission
import java.util.UUID
import su.plo.voice.groups.api.result.BanResult as ApiBanResult
import su.plo.voice.groups.api.result.CancelInviteResult as ApiCancelInviteResult
import su.plo.voice.groups.api.result.CreateResult as ApiCreateResult
import su.plo.voice.groups.api.result.DeclineInviteResult as ApiDeclineInviteResult
import su.plo.voice.groups.api.result.InviteResult as ApiInviteResult
import su.plo.voice.groups.api.result.JoinResult as ApiJoinResult
import su.plo.voice.groups.api.result.KickResult as ApiKickResult
import su.plo.voice.groups.api.result.LeaveResult as ApiLeaveResult
import su.plo.voice.groups.api.result.SetNameResult as ApiSetNameResult
import su.plo.voice.groups.api.result.TransferResult as ApiTransferResult
import su.plo.voice.groups.api.result.UnbanResult as ApiUnbanResult

class GroupsChannel(
    private val addon: GroupsAddon,
    private val playerCollector: PlayerCollector,
) {
    val server = ChannelServer(CHANNEL_KEY)

    private val voiceServer = addon.voiceServer
    private val groupManager = addon.groupManager
    private val projection = GroupsProjection(groupManager, playerCollector)
    private val sessions = GroupsSessionManager()
    private val stateListener = GroupsStateListener(server, sessions, voiceServer, groupManager, projection)

    init {
        voiceServer.eventBus.register(addon, sessions)
        stateListener.register()
        voiceServer.eventBus.register(addon, stateListener)

        server.registerRequestHandler<HandshakeRequest, HandshakeResponse> { player, request ->
            sessions.handshake(player, request)
        }

        handle<StateRequest, GroupsState> { player, _ ->
            synchronized(groupManager) {
                projection.state(player)
            }
        }
        handle<BrowseRequest, BrowseResponse> { player, _ ->
            synchronized(groupManager) {
                BrowseResponse(projection.browse(player))
            }
        }
        handle<CreateRequest, CreateResult> { player, _ -> create(player) }
        handle<JoinRequest, JoinResult>(::join)
        handle<LeaveRequest, LeaveResult> { player, _ -> leave(player) }
        handle<KickRequest, KickResult>(::kick)
        handle<BanRequest, BanResult>(::ban)
        handle<UnbanRequest, UnbanResult>(::unban)
        handle<TransferRequest, TransferResult>(::transfer)
        handle<InviteRequest, InviteResult>(::invite)
        handle<CancelInviteRequest, CancelInviteResult>(::cancelInvite)
        handle<DeclineInviteRequest, DeclineInviteResult>(::declineInvite)
        handle<DeleteRequest, DeleteResult>(::delete)
        handle<UpdateSettingsRequest, UpdateSettingsResult>(::updateSettings)
    }

    fun shutdown() {
        stateListener.unregister()
        voiceServer.eventBus.unregister(addon, stateListener)
        voiceServer.eventBus.unregister(addon, sessions)
        server.shutdown()
    }

    private fun create(player: VoicePlayer): CreateResult =
        mutate {
            if (!player.instance.hasAddonPermission("create")) return NoPermission

            when (val result = groupManager.createGroup { owner(player) }) {
                is ApiCreateResult.Success -> Success
                is ApiCreateResult.NameLengthInvalid -> NameLengthInvalid(result.min, result.max)
            }
        }

    private fun join(
        player: VoicePlayer,
        request: JoinRequest,
    ): JoinResult =
        mutate {
            val source = player.instance
            val group = groupManager.groupsById[request.groupId] ?: return GroupNotFound

            val result =
                when {
                    source.hasAddonPermission("join.*") -> {
                        groupManager.joinUnchecked(player, group)
                    }

                    !source.hasAddonPermission("join") -> {
                        return NoPermission
                    }

                    else -> {
                        groupManager.join(
                            player,
                            group,
                            request.password,
                            ignorePermissionsFilter = source.hasAddonPermission("join.all"),
                        )
                    }
                }

            when (result) {
                is ApiJoinResult.Success -> Success
                ApiJoinResult.AlreadyInGroup -> AlreadyInGroup
                ApiJoinResult.Banned -> Banned
                ApiJoinResult.PasswordRequired -> PasswordRequired
                ApiJoinResult.WrongPassword -> WrongPassword
                ApiJoinResult.NoPermission -> NoPermission
            }
        }

    private fun leave(player: VoicePlayer): LeaveResult =
        mutate {
            if (!player.instance.hasAddonPermission("leave")) return NoPermission

            when (groupManager.leave(player.instance.uuid)) {
                is ApiLeaveResult.Success -> Success
                ApiLeaveResult.NotInGroup -> NotInGroup
            }
        }

    private fun kick(
        player: VoicePlayer,
        request: KickRequest,
    ): KickResult =
        mutate {
            val group = currentGroup(player, request.groupId) ?: return GroupNotFound
            if (GroupAction.KICK !in projection.actions(group, player)) return NoPermission
            if (request.playerId == player.instance.uuid) return Self

            when (group.kick(request.playerId)) {
                ApiKickResult.Success -> Success
                ApiKickResult.NotInGroup -> NotInGroup
            }
        }

    private fun ban(
        player: VoicePlayer,
        request: BanRequest,
    ): BanResult =
        mutate {
            val group = currentGroup(player, request.groupId) ?: return GroupNotFound
            if (GroupAction.BAN !in projection.actions(group, player)) return NoPermission
            if (request.playerId == player.instance.uuid) return Self
            if (group.isBanned(request.playerId)) return Success

            val target = banTarget(group, player, request.playerId) ?: return PlayerNotFound

            when (group.ban(target)) {
                ApiBanResult.Success -> Success
                ApiBanResult.Owner -> Owner
            }
        }

    private fun unban(
        player: VoicePlayer,
        request: UnbanRequest,
    ): UnbanResult =
        mutate {
            val group = currentGroup(player, request.groupId) ?: return GroupNotFound
            if (GroupAction.UNBAN !in projection.actions(group, player)) return NoPermission

            val target = group.bannedPlayers.firstOrNull { it.id == request.playerId } ?: return NotBanned

            when (group.unban(target)) {
                ApiUnbanResult.Success -> Success
                ApiUnbanResult.NotBanned -> NotBanned
            }
        }

    private fun transfer(
        player: VoicePlayer,
        request: TransferRequest,
    ): TransferResult =
        mutate {
            val group = currentGroup(player, request.groupId) ?: return GroupNotFound
            if (GroupAction.TRANSFER !in projection.actions(group, player)) return NoPermission
            if (request.playerId == player.instance.uuid) return Self

            when (group.transferOwnership(request.playerId)) {
                ApiTransferResult.Success -> Success
                ApiTransferResult.NotInGroup -> NotInGroup
            }
        }

    private fun invite(
        player: VoicePlayer,
        request: InviteRequest,
    ): InviteResult =
        mutate {
            val group = currentGroup(player, request.groupId) ?: return GroupNotFound
            if (GroupAction.INVITE !in projection.actions(group, player)) return NoPermission

            val target =
                playerCollector
                    .getVisibleOnlinePlayers(player)
                    .firstOrNull { it.instance.uuid == request.playerId }
                    ?: return PlayerNotFound

            when (group.invite(player, target)) {
                ApiInviteResult.Success -> Success
                ApiInviteResult.AlreadyInGroup -> AlreadyInGroup
                ApiInviteResult.Banned -> Banned
                ApiInviteResult.Self -> Self
            }
        }

    private fun cancelInvite(
        player: VoicePlayer,
        request: CancelInviteRequest,
    ): CancelInviteResult =
        mutate {
            val group = currentGroup(player, request.groupId) ?: return GroupNotFound
            if (GroupAction.INVITE !in projection.actions(group, player)) return NoPermission

            when (group.cancelInvite(request.playerId)) {
                ApiCancelInviteResult.Success -> Success
                ApiCancelInviteResult.NotInvited -> PlayerNotFound
            }
        }

    private fun declineInvite(
        player: VoicePlayer,
        request: DeclineInviteRequest,
    ): DeclineInviteResult =
        mutate {
            val invite =
                groupManager
                    .getPlayerInvites(player.instance.uuid)
                    .firstOrNull { it.id == request.inviteId }
                    ?: return GroupNotFound

            when (groupManager.declineInvite(invite)) {
                ApiDeclineInviteResult.Success -> Success
                ApiDeclineInviteResult.NotFound -> GroupNotFound
            }
        }

    private fun delete(
        player: VoicePlayer,
        request: DeleteRequest,
    ): DeleteResult =
        mutate {
            val group = currentGroup(player, request.groupId) ?: return GroupNotFound
            if (GroupAction.DELETE !in projection.actions(group, player)) return NoPermission

            groupManager.deleteGroup(group)

            Success
        }

    private fun updateSettings(
        player: VoicePlayer,
        request: UpdateSettingsRequest,
    ): UpdateSettingsResult =
        mutate {
            val group = currentGroup(player, request.groupId) ?: return GroupNotFound
            val source = player.instance
            val actions = projection.actions(group, player)
            val unsetTier = source.hasTierPermission("unset", group.isOwner(player))

            fun canUnset(flag: String) = unsetTier && source.hasFlagPermission(flag)

            val password = request.password
            val permissions =
                request.permissions
                    ?.map { it.trim() }
                    ?.filterTo(HashSet()) { it.isNotEmpty() }

            val canChangePassword =
                when {
                    password == null -> true
                    password.isEmpty() -> canUnset("password")
                    else -> GroupAction.SET_PASSWORD in actions
                }
            val canChangePermissions =
                when {
                    permissions == null -> true
                    permissions.isEmpty() -> canUnset("permissions")
                    else -> GroupAction.SET_PERMISSIONS in actions
                }

            val allowed =
                (request.name == null || GroupAction.SET_NAME in actions) &&
                    canChangePassword &&
                    canChangePermissions &&
                    (request.persistent == null || GroupAction.SET_PERSISTENT in actions)
            if (!allowed) return NoPermission

            request.name?.let { name ->
                val result = group.setName(name)
                if (result is ApiSetNameResult.NameLengthInvalid) return NameLengthInvalid(result.min, result.max)
            }
            password?.let { group.setPassword(it.ifEmpty { null }) }
            permissions?.let { group.setPermissions(it) }
            request.persistent?.let { group.setPersistent(it) }

            Success
        }

    private fun currentGroup(
        player: VoicePlayer,
        groupId: UUID,
    ): GroupImpl? = groupManager.groupByPlayer[player.instance.uuid]?.takeIf { it.id == groupId }

    private fun banTarget(
        group: GroupImpl,
        player: VoicePlayer,
        playerId: UUID,
    ): McGameProfile? =
        group.playersById[playerId]
            ?: group.getInvite(playerId)?.target
            ?: playerCollector
                .getVisibleOnlinePlayers(player)
                .firstOrNull { it.instance.uuid == playerId }
                ?.instance
                ?.gameProfile

    private inline fun <R> mutate(block: () -> R): R =
        synchronized(groupManager) {
            try {
                block()
            } finally {
                stateListener.flush()
            }
        }

    private inline fun <reified Request : ChannelRequest<Response>, reified Response> handle(
        crossinline handler: (VoicePlayer, Request) -> Response,
    ) = server.registerRequestHandler<Request, Response> { player, request ->
        voiceServer.playerManager
            .getPlayerById(player.uuid)
            .orElse(null)
            ?.let { handler(it, request) }
    }
}
