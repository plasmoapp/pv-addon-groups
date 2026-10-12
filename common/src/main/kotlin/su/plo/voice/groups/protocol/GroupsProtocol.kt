package su.plo.voice.groups.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val CHANNEL_KEY = "pv_addon_groups:channel"

/**
 * Bump it only for a breaking change (renaming fields, changing field types).
 *
 * [HandshakeRequest] and [HandshakeResponse] must stay backward compatible:
 * they're how both sides find out that the other one is incompatible.
 */
const val PROTOCOL_VERSION = 1

const val MIN_PROTOCOL_VERSION = 1

fun isProtocolCompatible(
    theirVersion: Int,
    theirMinVersion: Int,
): Boolean = theirVersion >= MIN_PROTOCOL_VERSION && theirMinVersion <= PROTOCOL_VERSION

@Serializable
@SerialName("handshake")
data class HandshakeRequest(
    val protocolVersion: Int,
    val minProtocolVersion: Int,
) : ChannelRequest<HandshakeResponse>

@Serializable
@SerialName("handshake_response")
data class HandshakeResponse(
    val protocolVersion: Int,
    val minProtocolVersion: Int,
)
