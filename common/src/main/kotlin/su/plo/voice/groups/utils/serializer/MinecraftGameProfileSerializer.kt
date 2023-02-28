package su.plo.voice.groups.utils.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import su.plo.voice.proto.data.player.MinecraftGameProfile
import java.util.*

object MinecraftGameProfileSerializer : KSerializer<MinecraftGameProfile> {
    override val descriptor = buildClassSerialDescriptor("MinecraftGameProfileSerializer") {
        element<UUID>("uuid")
        element<String>("name")
    }

    override fun deserialize(decoder: Decoder): MinecraftGameProfile =
        decoder.decodeStructure(descriptor) {
            val uuid = decodeSerializableElement(descriptor, 0, UUIDSerializer)
            val name = decodeStringElement(descriptor, 1)

            MinecraftGameProfile(uuid, name, Collections.emptyList())
        }

    override fun serialize(encoder: Encoder, value: MinecraftGameProfile) =
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, UUIDSerializer, value.id)
            encodeStringElement(descriptor, 1, value.name)
        }
}
