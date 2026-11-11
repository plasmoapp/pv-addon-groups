package su.plo.voice.groups.group

import kotlinx.serialization.Serializable
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.groups.util.serializer.McGameProfileSerializer
import su.plo.voice.groups.util.serializer.UUIDSerializer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class GroupData(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val password: String? = null,
    val persistent: Boolean = false,
    val playersById: MutableMap<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        @Serializable(with = McGameProfileSerializer::class)
        McGameProfile,
    > = ConcurrentHashMap(),
    val bannedPlayers: MutableList<
        @Serializable(with = McGameProfileSerializer::class)
        McGameProfile,
    > = CopyOnWriteArrayList(),
    @Serializable(with = McGameProfileSerializer::class)
    val owner: McGameProfile? = null,
)
