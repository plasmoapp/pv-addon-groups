package su.plo.voice.groups.listener

import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.event.connection.UdpClientConnectedEvent
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent
import su.plo.voice.groups.group.GroupsManager

class GroupsConnectionListener(
    private val groupManager: GroupsManager,
) {
    @EventSubscribe
    fun onPlayerJoin(event: UdpClientConnectedEvent) {
        val player = event.connection.player
        val playerId = player.instance.uuid

        groupManager.groupByPlayer[playerId]?.let { group ->
            if (group.owner?.id == playerId) {
                group.owner = player.instance.gameProfile
            }

            group.onPlayerJoin(player)
            groupManager.sourceLine.playerSetManager?.setPlayerSet(player, group.playerSet)
            groupManager.initSource(player, group)
        }
    }

    @EventSubscribe
    fun onPlayerLeave(event: UdpClientDisconnectedEvent) {
        val playerId = event.connection.player.instance.uuid

        groupManager.sourceByPlayer.remove(playerId)?.let { groupManager.sourceLine.removeSource(it) }
        groupManager.groupByPlayer[playerId]?.let {
            it.onPlayerQuit(playerId)
            if (it.onlinePlayers.isEmpty() && !it.persistent) groupManager.deleteGroupInternal(it)
        }
    }
}
