package su.plo.voice.groups.protocol

import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPackConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

val channelMsgPack = MsgPack(MsgPackConfiguration(ignoreUnknownKeys = true))

@Serializable
data class ChannelMessage(
    val type: String,
    val requestId: String? = null,
    val payload: ByteArray,
)

interface ChannelRequest<Response>

/***
 * Packets must set an explicit `@SerialName`,
 * otherwise default one (class FQCN) will be used,
 * which would make renaming or moving a packet class a protocol break.
 */
inline fun <reified T> packetType(): String = serializer<T>().descriptor.serialName
