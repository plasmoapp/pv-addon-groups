package su.plo.voice.groups.util.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.LongArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.ByteBuffer
import java.util.UUID

object UUIDByteSerializer : KSerializer<UUID> {
    private val bytesSerializer = ByteArraySerializer()

    override val descriptor = SerialDescriptor("UUIDBytes", bytesSerializer.descriptor)

    override fun deserialize(decoder: Decoder): UUID {
        val buffer = ByteBuffer.wrap(decoder.decodeSerializableValue(bytesSerializer))
        return UUID(buffer.long, buffer.long)
    }

    override fun serialize(
        encoder: Encoder,
        value: UUID,
    ) = encoder.encodeSerializableValue(
        bytesSerializer,
        ByteBuffer
            .allocate(16)
            .putLong(value.mostSignificantBits)
            .putLong(value.leastSignificantBits)
            .array(),
    )
}
