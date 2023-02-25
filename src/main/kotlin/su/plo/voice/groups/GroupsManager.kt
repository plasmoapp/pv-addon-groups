package su.plo.voice.groups

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import su.plo.voice.api.event.EventPriority
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.line.ServerSourceLine
import su.plo.voice.api.server.audio.source.ServerDirectSource
import su.plo.voice.api.server.event.VoiceServerShutdownEvent
import su.plo.voice.api.server.event.player.PlayerJoinEvent
import su.plo.voice.api.server.event.player.PlayerQuitEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.group.Group
import su.plo.voice.groups.group.GroupData
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class GroupsManager(
    val config: Config,
    val server: PlasmoVoiceServer,
    val addon: GroupsAddon,
    val activation: ServerActivation,
    val sourceLine: ServerSourceLine,
) {
    val groupByPlayer: MutableMap<UUID, Group> = ConcurrentHashMap()
    val groups: MutableMap<UUID, Group> = ConcurrentHashMap()
    val sourceByPlayer: MutableMap<UUID, ServerDirectSource> = ConcurrentHashMap()

    fun join(player: VoicePlayer, group: Group) {
        leave(player)
        initSource(player, group)
        groupByPlayer[player.instance.uuid] = group
        group.players.add(player)
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
        val didLeft = group?.players?.remove(player)

        if (didLeft == false) return false

        if (group?.persistent == false) {
            if (group.owner?.id == player.instance.uuid)
                group.owner = group.players.randomOrNull()?.instance?.gameProfile
            if (group.onlinePlayers.isEmpty()) deleteGroup(group)
        }

        return true
    }

    fun deleteGroup(group: Group) = groups.remove(group.id)?.let { group ->
        group.players.forEach { leave(it) }
    }

    @EventSubscribe
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = server.playerManager.getPlayerById(event.playerId).orElse( null ) ?: return
        groupByPlayer[event.playerId]?.let { initSource(player, it) }
    }

    @EventSubscribe
    fun onPlayerLeave(event: PlayerQuitEvent) {
        sourceByPlayer.remove(event.playerId)?.let { server.sourceManager.remove(it) }
        groupByPlayer[event.playerId]?.let {
            if (it.onlinePlayers.isEmpty()) deleteGroup(it)
        }
    }

    @EventSubscribe
    fun onVoiceServerShutdown(event: VoiceServerShutdownEvent) {
        groups.values.map { group -> GroupData(
            group.owner?.id,
            group.players.map { it.instance.uuid }.toSet(),
            group
        )}.let {
            File(addon.getAddonFolder(server), "groups.json")
                .writeText(Json.encodeToString(it))
        }
    }
}