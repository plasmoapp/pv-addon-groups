package su.plo.voice.groups

import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.line.ServerSourceLine
import su.plo.voice.api.server.audio.source.ServerDirectSource
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.group.Group
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

        val source = server.sourceManager.createDirectSource(addon, sourceLine, "opus", false)
        source.addFilter { it.info.playerId != player.info.playerId }
        source.setSender(player)
        source.setPlayers(group::players)

        groupByPlayer[player.info.playerId] = group
        sourceByPlayer[player.instance.uuid] = source
        group.players.add(player)
    }

    fun leave(player: VoicePlayer): Boolean {

        sourceByPlayer.remove(player.info.playerId)
            ?.let { server.sourceManager.remove(it) }

        val group = groupByPlayer.remove(player.info.playerId)
        val wasRemoved = group?.players?.remove(player)

        if (group?.persistent == false) {
            if (group.owner == player) group.owner = group.players.randomOrNull()
            if (group.players.isEmpty()) groups.remove(group.id)
        }

        return wasRemoved ?: false
    }

    fun deleteGroup() {

    }
}
