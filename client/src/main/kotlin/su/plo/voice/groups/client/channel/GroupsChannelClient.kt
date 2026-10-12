package su.plo.voice.groups.client.channel

import su.plo.slib.api.logging.McLoggerFactory
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.HandshakeRequest
import su.plo.voice.groups.protocol.HandshakeResponse
import su.plo.voice.groups.protocol.MIN_PROTOCOL_VERSION
import su.plo.voice.groups.protocol.PROTOCOL_VERSION
import su.plo.voice.groups.protocol.isProtocolCompatible

private val logger = McLoggerFactory.createLogger(ADDON_LOGGER, "ChannelClient")

class GroupsChannelClient(
    private val channel: ChannelClient,
) {
    @Volatile
    var session: HandshakeResponse? = null
        private set

    suspend fun handshake(): HandshakeResponse? {
        session = null

        val response = channel.request(HandshakeRequest(PROTOCOL_VERSION, MIN_PROTOCOL_VERSION))

        session =
            response.takeIf { isProtocolCompatible(it.protocolVersion, it.minProtocolVersion) }
                ?: run {
                    logger.warn(
                        "Groups protocol mismatch: server supports {}..{}, this mod supports {}..{}",
                        response.minProtocolVersion,
                        response.protocolVersion,
                        MIN_PROTOCOL_VERSION,
                        PROTOCOL_VERSION,
                    )
                    null
                }

        return session
    }

    fun reset() {
        session = null
        channel.reset()
    }
}
