package su.plo.voice.groups.utils.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import su.plo.slib.api.entity.player.McGameProfile
import java.util.*

object McGameProfileSerializer : KSerializer<McGameProfile> {

    override val descriptor = buildClassSerialDescriptor("MinecraftGameProfileSerializer") {
        element<String>("uuid")
        element<String>("name")
    }

    override fun deserialize(decoder: Decoder): McGameProfile =
        decoder.decodeStructure(descriptor) {
            var uuid: UUID? = null
            var name: String? = null

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    DECODE_DONE -> break@loop

                    0 -> uuid = decodeSerializableElement(descriptor, 0, UUIDSerializer)
                    1 -> name = decodeStringElement(descriptor, 1)

                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            McGameProfile(requireNotNull(uuid), requireNotNull(name), Collections.emptyList())
        }

    override fun serialize(encoder: Encoder, value: McGameProfile) =
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, UUIDSerializer, value.id)
            encodeStringElement(descriptor, 1, value.name)
        }
}
