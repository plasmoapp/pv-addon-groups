package su.plo.voice.groups.channel

import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.HandshakeRequest
import su.plo.voice.groups.protocol.HandshakeResponse
import su.plo.voice.groups.protocol.MIN_PROTOCOL_VERSION
import su.plo.voice.groups.protocol.PROTOCOL_VERSION
import su.plo.voice.groups.protocol.isProtocolCompatible

fun ChannelServer.registerGroupsHandlers() {
    registerRequestHandler<HandshakeRequest, HandshakeResponse> { player, request ->
        if (!isProtocolCompatible(request.protocolVersion, request.minProtocolVersion)) {
            ADDON_LOGGER.debug(
                "Incompatible client protocol from {}: client {}..{}, server {}..{}",
                player.name,
                request.minProtocolVersion,
                request.protocolVersion,
                MIN_PROTOCOL_VERSION,
                PROTOCOL_VERSION,
            )
        }

        HandshakeResponse(PROTOCOL_VERSION, MIN_PROTOCOL_VERSION)
    }
}
