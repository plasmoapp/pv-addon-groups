package su.plo.voice.groups.channel

import su.plo.slib.api.entity.player.McPlayer
import su.plo.voice.api.event.EventPriority
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.HandshakeRequest
import su.plo.voice.groups.protocol.HandshakeResponse
import su.plo.voice.groups.protocol.MIN_PROTOCOL_VERSION
import su.plo.voice.groups.protocol.PROTOCOL_VERSION
import su.plo.voice.groups.protocol.isProtocolCompatible
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GroupsSessionManager {
    private val sessions: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    operator fun contains(playerId: UUID): Boolean = playerId in sessions

    fun handshake(
        player: McPlayer,
        request: HandshakeRequest,
    ): HandshakeResponse {
        if (isProtocolCompatible(request.protocolVersion, request.minProtocolVersion)) {
            sessions += player.uuid
        } else {
            sessions -= player.uuid
            ADDON_LOGGER.debug(
                "Incompatible client protocol from {}: client {}..{}, server {}..{}",
                player.name,
                request.minProtocolVersion,
                request.protocolVersion,
                MIN_PROTOCOL_VERSION,
                PROTOCOL_VERSION,
            )
        }

        return HandshakeResponse(PROTOCOL_VERSION, MIN_PROTOCOL_VERSION)
    }

    // runs before the other disconnect handlers, so state pushes they trigger already skip this player
    @EventSubscribe(priority = EventPriority.LOWEST)
    fun onUdpDisconnected(event: UdpClientDisconnectedEvent) {
        sessions -= event.connection.player.instance.uuid
    }
}
