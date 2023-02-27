package su.plo.voice.groups

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.line.ServerPlayersSourceLine
import su.plo.voice.api.server.audio.source.ServerDirectSource
import su.plo.voice.api.server.event.VoiceServerShutdownEvent
import su.plo.voice.api.server.event.connection.UdpClientConnectedEvent
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.group.Group
import su.plo.voice.groups.group.GroupOfflineData
import su.plo.voice.groups.utils.serializer.UUIDSerializer
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GroupsManager(
    val config: Config,
    val server: PlasmoVoiceServer,
    private val addon: GroupsAddon,
    val activation: ServerActivation,
    val sourceLine: ServerPlayersSourceLine,
) {
    val groupByPlayer: MutableMap<UUID, Group> = ConcurrentHashMap()
    val groups: MutableMap<UUID, Group> = ConcurrentHashMap()
    val sourceByPlayer: MutableMap<UUID, ServerDirectSource> = ConcurrentHashMap()

//    val groupByPlayerCache: MutableMap<@Serializable(with = UUIDSerializer::class) UUID, UUID> = ConcurrentHashMap()

    fun join(player: VoicePlayer, group: Group) {
        leave(player)
        initSource(player, group)
        groupByPlayer[player.instance.uuid] = group
        group.playersSet.addPlayer(player)
        sourceLine.setPlayersSet(player, group.playersSet) // todo: DRY
    }

    private fun initSource(player: VoicePlayer, group: Group) {
        val source = server.sourceManager.createDirectSource(addon, sourceLine, "opus", false)
        source.addFilter { it.instance != player.instance }
        source.setSender(player)
        source.setPlayers(group::players)
        sourceByPlayer[player.instance.uuid] = source
    }

    fun leave(player: VoicePlayer): Boolean {

        sourceByPlayer.remove(player.instance.uuid)
            ?.let { server.sourceManager.remove(it) }

        val group = groupByPlayer.remove(player.instance.uuid)
        val didLeft = group?.playersSet?.let {
            it.removePlayer(player.instance.uuid).also {
                sourceLine.setPlayersSet(player, null)
            }
        }

        if (didLeft == false) return false

        if (group?.persistent == false) {
            if (group.owner?.id == player.instance.uuid)
                group.owner = group.onlinePlayers.randomOrNull()?.instance?.gameProfile
                group.owner?.let { group.notifyPlayersTranslatable("pv.addon.groups.notifications.new_owner", it.name) }
            if (group.onlinePlayers.isEmpty()) deleteGroup(group)
        }

        return true
    }

    fun deleteGroup(group: Group) = groups.remove(group.id)?.let { group ->
        group.players.forEach { leave(it) }
    }

    @EventSubscribe
    fun onPlayerJoin(event: UdpClientConnectedEvent) {
        val player = event.connection.player
        val playerId = player.instance.uuid

        groupByPlayer[playerId]?.let { group ->
            group.playersSet.removePlayer(playerId)
            group.playersSet.addPlayer(player)
            sourceLine.setPlayersSet(player, group.playersSet)
            initSource(player, group)
        }
    }

    @EventSubscribe
    fun onPlayerLeave(event: UdpClientDisconnectedEvent) {
        val playerId = event.connection.player.instance.uuid
        sourceByPlayer.remove(playerId)?.let { server.sourceManager.remove(it) }
        groupByPlayer[playerId]?.let {
            if (it.onlinePlayers.isEmpty()) deleteGroup(it)
        }
    }

    @EventSubscribe
    fun onVoiceServerShutdown(event: VoiceServerShutdownEvent) {
        val groups = groups.values
            .filter { it.persistent }
            .map { it.asOfflineData() }

        File(addon.getAddonFolder(server), "groups.json")
            .writeText(Json.encodeToString(Data(
                groups,
                groupByPlayer.map { it.key to it.value.id }.toMap(),
            )))
    }

    @Serializable
    data class Data(
        val groups: List<GroupOfflineData>,
        val groupByPlayer: Map<
                @Serializable(with = UUIDSerializer::class) UUID,
                @Serializable(with = UUIDSerializer::class) UUID
            >
    )
}
