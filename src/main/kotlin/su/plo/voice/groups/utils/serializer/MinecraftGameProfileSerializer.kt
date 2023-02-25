package su.plo.voice.groups.utils.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.proto.data.player.MinecraftGameProfile
import java.util.*

//object MinecraftGameProfileSerializer : KSerializer<MinecraftGameProfile> {
//    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
//    override fun deserialize(decoder: Decoder): MinecraftGameProfile = UUID.fromString(decoder.decodeString())
//    override fun serialize(encoder: Encoder, value: MinecraftGameProfile) = encoder.encodeString(value.toString())
//}
