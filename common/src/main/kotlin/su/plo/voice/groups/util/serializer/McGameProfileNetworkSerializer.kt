package su.plo.voice.groups.util.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import su.plo.slib.api.entity.player.McGameProfile
import java.util.UUID

object McGameProfileNetworkSerializer : KSerializer<McGameProfile> {
    override val descriptor = SerialDescriptor("McGameProfileNetwork", Surrogate.serializer().descriptor)

    override fun deserialize(decoder: Decoder): McGameProfile {
        val surrogate = decoder.decodeSerializableValue(Surrogate.serializer())

        return McGameProfile(
            surrogate.id,
            surrogate.name,
            surrogate.properties.map { McGameProfile.Property(it.name, it.value, it.signature) },
        )
    }

    override fun serialize(
        encoder: Encoder,
        value: McGameProfile,
    ) = encoder.encodeSerializableValue(
        Surrogate.serializer(),
        Surrogate(
            value.id,
            value.name,
            value.properties.map { Property(it.name, it.value, it.signature) },
        ),
    )

    @Serializable
    private class Surrogate(
        @Serializable(with = UUIDByteSerializer::class)
        val id: UUID,
        val name: String,
        val properties: List<Property> = emptyList(),
    )

    @Serializable
    private class Property(
        val name: String,
        val value: String,
        val signature: String? = null,
    )
}
