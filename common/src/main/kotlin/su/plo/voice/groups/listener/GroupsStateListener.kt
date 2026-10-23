package su.plo.voice.groups.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.voice.api.event.EventPriority
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.api.server.event.connection.UdpClientConnectedEvent
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent
import su.plo.voice.groups.api.Group
import su.plo.voice.groups.api.event.GroupInviteEvent
import su.plo.voice.groups.api.event.GroupInviteRemovedEvent
import su.plo.voice.groups.api.event.GroupNameChangedEvent
import su.plo.voice.groups.api.event.GroupOwnerChangedEvent
import su.plo.voice.groups.api.event.GroupPasswordChangedEvent
import su.plo.voice.groups.api.event.GroupPermissionsChangedEvent
import su.plo.voice.groups.api.event.GroupPersistentChangedEvent
import su.plo.voice.groups.api.event.PlayerBannedFromGroupEvent
import su.plo.voice.groups.api.event.PlayerJoinedGroupEvent
import su.plo.voice.groups.api.event.PlayerLeftGroupEvent
import su.plo.voice.groups.api.event.PlayerUnbannedFromGroupEvent
import su.plo.voice.groups.channel.ChannelServer
import su.plo.voice.groups.channel.GroupsProjection
import su.plo.voice.groups.channel.GroupsSessionManager
import su.plo.voice.groups.group.GroupsManager
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.GroupsStateChanged
import su.plo.voice.groups.util.GlobalEventContext
import su.plo.voice.groups.util.scoped
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = McLoggerFactory.createLogger(ADDON_LOGGER, "GroupsStateListener")

class GroupsStateListener(
    private val channel: ChannelServer,
    private val sessions: GroupsSessionManager,
    private val voiceServer: PlasmoBaseVoiceServer,
    private val groupManager: GroupsManager,
    private val projection: GroupsProjection,
) {
    private val dirty: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val flushSignal = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val events = GlobalEventContext()

    fun register() {
        PlayerJoinedGroupEvent.scoped(events).registerListener { group, player ->
            markGroup(group, player.instance.uuid)
        }
        PlayerLeftGroupEvent.scoped(events).registerListener { group, profile, _ ->
            markGroup(group, profile.id)
        }
        PlayerBannedFromGroupEvent.scoped(events).registerListener { group, profile, _ ->
            markGroup(group, profile.id)
        }
        PlayerUnbannedFromGroupEvent.scoped(events).registerListener { group, profile ->
            markGroup(group, profile.id)
        }
        GroupOwnerChangedEvent.scoped(events).registerListener { group, _, _ ->
            markGroup(group)
        }
        GroupNameChangedEvent.scoped(events).registerListener { group, _, _ ->
            markGroup(group)
        }
        GroupPasswordChangedEvent.scoped(events).registerListener { group, _, _ ->
            markGroup(group)
        }
        GroupPermissionsChangedEvent.scoped(events).registerListener { group, _, _ ->
            markGroup(group)
        }
        GroupPersistentChangedEvent.scoped(events).registerListener { group, _ ->
            markGroup(group)
        }
        GroupInviteEvent.scoped(events).registerListener { group, _, target ->
            markGroup(group, target.instance.uuid)
        }
        GroupInviteRemovedEvent.scoped(events).registerListener { group, invite, _ ->
            markGroup(group, invite.target.id)
        }

        scope.launch {
            for (signal in flushSignal) {
                try {
                    flush()
                } catch (e: Exception) {
                    logger.error("Failed to push groups state", e)
                }
            }
        }
    }

    fun unregister() {
        events.unregisterListeners()
        scope.cancel()
    }

    fun flush() {
        synchronized(groupManager) {
            val iterator = dirty.iterator()

            while (iterator.hasNext()) {
                val playerId = iterator.next()
                iterator.remove()
                if (playerId !in sessions) continue

                val player =
                    voiceServer.playerManager
                        .getPlayerById(playerId)
                        .orElse(null)
                        ?: continue

                channel.sendMessage(
                    player.instance,
                    GroupsStateChanged(projection.state(player)),
                )
            }
        }
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    fun onUdpConnected(event: UdpClientConnectedEvent) {
        groupManager.getPlayerGroup(event.connection.player.instance.uuid)?.let { markGroup(it) }
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    fun onUdpDisconnected(event: UdpClientDisconnectedEvent) {
        groupManager.getPlayerGroup(event.connection.player.instance.uuid)?.let { markGroup(it) }
    }

    private fun markGroup(
        group: Group,
        vararg extra: UUID,
    ) {
        val ids =
            buildList {
                group.onlinePlayers.forEach { add(it.instance.uuid) }
                group.invites.forEach { add(it.target.id) }
                addAll(extra)
            }

        ids.filterTo(dirty) { it in sessions }
        flushSignal.trySend(Unit)
    }
}
