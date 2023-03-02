package su.plo.voice.groups

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoCommonVoiceServer
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.line.ServerPlayersSourceLine
import su.plo.voice.api.server.audio.source.ServerDirectSource
import su.plo.voice.api.server.event.connection.UdpClientConnectedEvent
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.group.Group
import su.plo.voice.groups.group.GroupData
import su.plo.voice.groups.utils.serializer.UUIDSerializer
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GroupsManager(
    val config: Config,
    private val voiceServer: PlasmoCommonVoiceServer,
    private val addon: GroupsAddon,
    val activation: ServerActivation,
    val sourceLine: ServerPlayersSourceLine,
) {
    val groupByPlayer: MutableMap<UUID, Group> = ConcurrentHashMap()
    val groups: MutableMap<UUID, Group> = ConcurrentHashMap()
    val sourceByPlayer: MutableMap<UUID, ServerDirectSource> = ConcurrentHashMap()

//    val groupByPlayerCache: MutableMap<@Serializable(with = UUIDSerializer::class) UUID, UUID> = ConcurrentHashMap()

    fun join(player: VoicePlayer, group: Group) {
        leave(player.instance.uuid)
        initSource(player, group)
        groupByPlayer[player.instance.uuid] = group

        group.addPlayer(player)
        sourceLine.setPlayersSet(player, group.playersSet) // todo: DRY
    }

    private fun initSource(player: VoicePlayer, group: Group) {
        val source = voiceServer.sourceManager.createDirectSource(addon, sourceLine, "opus", false)
        source.setPlayers(group::onlinePlayers)
        source.addFilter { it.instance != player.instance }
        source.setSender(player)
        sourceByPlayer[player.instance.uuid] = source
    }

    fun leave(player: VoicePlayer): Boolean =
        leave(player.instance.uuid)

    fun leave(playerUuid: UUID): Boolean {
        sourceByPlayer.remove(playerUuid)
            ?.let { voiceServer.sourceManager.remove(it) }

        val group = groupByPlayer.remove(playerUuid)
        val didLeft = group?.removePlayer(playerUuid).also {
            voiceServer.playerManager
                .getPlayerById(playerUuid)
                .orElse(null)
                ?.let {
                    sourceLine.setPlayersSet(it, null)
                }
        }

        if (didLeft == false) return false

        if (group?.persistent == false) {
            if (group.owner?.id == playerUuid)
                group.owner = group.onlinePlayers.randomOrNull()?.instance?.gameProfile
            group.owner?.let { group.notifyPlayersTranslatable("pv.addon.groups.notifications.new_owner", it.name) }
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
            sourceLine.setPlayersSet(player, group.playersSet)
            initSource(player, group)
        }
    }

    @EventSubscribe
    fun onPlayerLeave(event: UdpClientDisconnectedEvent) {
        val playerId = event.connection.player.instance.uuid

        sourceByPlayer.remove(playerId)?.let { voiceServer.sourceManager.remove(it) }
        groupByPlayer[playerId]?.let {
            it.onPlayerQuit(playerId)
            if (it.onlinePlayers.isEmpty() && !it.persistent) deleteGroup(it)
        }
    }

    fun onVoiceServerShutdown(server: PlasmoCommonVoiceServer) {
        val groups = groups.values
            .filter { it.persistent }

        File(addon.getAddonFolder(server), "groups.json")
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
