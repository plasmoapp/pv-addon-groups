package su.plo.voice.groups

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.line.BaseServerSourceLine
import su.plo.voice.api.server.audio.source.ServerBroadcastSource
import su.plo.voice.api.server.event.connection.UdpClientConnectedEvent
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.group.Group
import su.plo.voice.groups.group.GroupData
import su.plo.voice.groups.utils.extend.sendTranslatable
import su.plo.voice.groups.utils.serializer.UUIDSerializer
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GroupsManager(
    val config: Config,
    private val voiceServer: PlasmoBaseVoiceServer,
    private val addon: GroupsAddon,
    val activation: ServerActivation,
    val sourceLine: BaseServerSourceLine,
) {
    val groupByPlayer: MutableMap<UUID, Group> = ConcurrentHashMap()
    val groups: MutableMap<UUID, Group> = ConcurrentHashMap()
    val sourceByPlayer: MutableMap<UUID, ServerBroadcastSource> = ConcurrentHashMap()

//    val groupByPlayerCache: MutableMap<@Serializable(with = UUIDSerializer::class) UUID, UUID> = ConcurrentHashMap()

    fun join(player: VoicePlayer, group: Group) {
        leave(player.instance.uuid)
        initSource(player, group)
        groupByPlayer[player.instance.uuid] = group

        group.addPlayer(player)
        sourceLine.playerSetManager?.setPlayerSet(player, group.playerSet) // todo: DRY
    }

    private fun initSource(player: VoicePlayer, group: Group) {
        val source = sourceLine.createBroadcastSource()
        source.players = group.onlinePlayers
        source.addFilter<VoicePlayer> { it.instance != player.instance }
        source.sender = player
        sourceByPlayer[player.instance.uuid] = source
    }

    fun kick(group: Group, player: VoicePlayer) {
        leave(player)
        player.instance.sendTranslatable("pv.addon.groups.notifications.kicked")
        group.notifyPlayersTranslatable("pv.addon.groups.notifications.player_kicked", player.instance.name)
    }

    fun ban(group: Group, player: VoicePlayer) {
        val didLeft = leave(player)
        group.banPlayer(player)

        group.notifyPlayersTranslatable("pv.addon.groups.notifications.player_banned", player.instance.name)

        if (didLeft) {
            player.instance.sendTranslatable("pv.addon.groups.notifications.banned", group.inlineChatComponent())
        }
    }

    fun unban(group: Group, player: McGameProfile) {
        group.unbanPlayer(player)
        group.notifyPlayersTranslatable("pv.addon.groups.notifications.player_unbanned", player.name)
    }

    fun leave(player: VoicePlayer): Boolean =
        leave(player.instance.uuid)

    fun leave(playerUuid: UUID): Boolean {
        sourceByPlayer.remove(playerUuid)
            ?.let { sourceLine.removeSource(it) }

        val group = groupByPlayer.remove(playerUuid)
        val didLeft = group?.removePlayer(playerUuid).also {
            voiceServer.playerManager
                .getPlayerById(playerUuid)
                .orElse(null)
                ?.let {
                    sourceLine.playerSetManager?.setPlayerSet(it, null)
                }
        }

        if (didLeft != true) return false

        if (group?.persistent == false) {
            if (group.owner?.id == playerUuid) {
                group.owner = group.onlinePlayers.randomOrNull()?.instance?.gameProfile
                group.owner?.let { group.notifyPlayersTranslatable("pv.addon.groups.notifications.new_owner", it.name) }
            }

            if (group.onlinePlayers.isEmpty()) deleteGroup(group)
        }

        return true
    }

    fun deleteGroup(group: Group) = groups.remove(group.id)?.let { group ->
        group.playersIds.forEach { leave(it) }
    }

    @EventSubscribe
    fun onPlayerJoin(event: UdpClientConnectedEvent) {
        val player = event.connection.player
        val playerId = player.instance.uuid

        groupByPlayer[playerId]?.let { group ->
            // todo: broken in persistent groups if owner is not in the group
            if (group.owner?.id == playerId) {
                group.owner = player.instance.gameProfile
            }

            group.onPlayerJoin(player)
            sourceLine.playerSetManager?.setPlayerSet(player, group.playerSet)
            initSource(player, group)
        }
    }

    @EventSubscribe
    fun onPlayerLeave(event: UdpClientDisconnectedEvent) {
        val playerId = event.connection.player.instance.uuid

        sourceByPlayer.remove(playerId)?.let { sourceLine.removeSource(it) }
        groupByPlayer[playerId]?.let {
            it.onPlayerQuit(playerId)
            if (it.onlinePlayers.isEmpty() && !it.persistent) deleteGroup(it)
        }
    }

    fun onVoiceServerShutdown(server: PlasmoBaseVoiceServer) {
        val groups = groups.values
            .filter { it.persistent }

        File(addon.getAddonFolder(server.minecraftServer), "groups.json")
            .writeText(Json.encodeToString(Data(
                groups,
                groupByPlayer.map { it.key to it.value.id }.toMap(),
            )))
    }

    @Serializable
    data class Data(
        val groups: List<GroupData>,
        val groupByPlayer: Map<
                @Serializable(with = UUIDSerializer::class) UUID,
                @Serializable(with = UUIDSerializer::class) UUID
            >
    )
}
